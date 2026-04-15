package com.kinetik.arsurvey.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetik.arsurvey.data.AppDatabase
import com.kinetik.arsurvey.data.UploadJob
import com.kinetik.arsurvey.util.GlassesNotifier
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(onJobClick: (String) -> Unit) {
    val context = LocalContext.current
    val jobDao = remember { AppDatabase.getInstance(context).jobDao() }
    val jobs by jobDao.getAll().collectAsState(initial = emptyList())

    // Auto-refresh for processing jobs
    val hasProcessingJobs = jobs.any { it.status in listOf("processing", "uploading", "queued") }
    var refreshTick by remember { mutableStateOf(0L) }

    // Notify glasses when jobs complete
    val glassesNotifier = remember { GlassesNotifier(context) }
    val completedCount = jobs.count { it.status == "completed" }
    val failedCount = jobs.count { it.status == "failed" }
    var lastNotifiedCount by remember { mutableStateOf(0) }

    LaunchedEffect(completedCount, failedCount) {
        if (completedCount + failedCount > lastNotifiedCount) {
            if (completedCount > 0) {
                glassesNotifier.notifyResultsReady(
                    violations = failedCount,  // Simplified: failed = had violations
                    compliant = completedCount - failedCount
                )
            }
            lastNotifiedCount = completedCount + failedCount
        }
    }

    LaunchedEffect(hasProcessingJobs) {
        if (hasProcessingJobs) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                refreshTick = System.currentTimeMillis()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Jobs") },
            actions = {
                IconButton(onClick = { refreshTick = System.currentTimeMillis() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        )

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No jobs yet. Record a video to get started.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs) { job ->
                    JobItem(job = job, onClick = { onJobClick(job.id) })
                }
            }
        }
    }
}

@Composable
private fun JobItem(job: UploadJob, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(job.createdAt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${job.id.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (job.fileSizeBytes > 0) {
                    Text(
                        text = formatFileSize(job.fileSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusBadge(status = job.status)
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
