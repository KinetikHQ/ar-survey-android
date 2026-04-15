package com.kinetik.arsurvey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetik.arsurvey.api.ApiClient
import com.kinetik.arsurvey.api.JobDetailResponse
import com.kinetik.arsurvey.data.AppDatabase
import com.kinetik.arsurvey.data.UploadJob
import com.kinetik.arsurvey.queue.UploadQueueManager
import com.kinetik.arsurvey.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(jobId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobDao = remember { AppDatabase.getInstance(context).jobDao() }
    val queueManager = remember { UploadQueueManager(context) }
    val prefsManager = remember { PreferencesManager(context) }

    var localJob by remember { mutableStateOf<UploadJob?>(null) }
    var remoteJob by remember { mutableStateOf<JobDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load local job
    LaunchedEffect(jobId) {
        jobDao.getByIdFlow(jobId).collect { job ->
            localJob = job
        }
    }

    // Fetch remote status
    fun fetchRemoteStatus() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val token = "Bearer ${prefsManager.getApiKey()}"
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getJob(token, jobId)
                }
                remoteJob = response
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    // Auto-fetch if processing
    LaunchedEffect(localJob?.status) {
        if (localJob?.status in listOf("processing", "uploaded")) {
            fetchRemoteStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchRemoteStatus() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status banner
            item {
                localJob?.let { job ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (job.status) {
                                "completed" -> MaterialTheme.colorScheme.primaryContainer
                                "failed" -> MaterialTheme.colorScheme.errorContainer
                                "processing", "uploaded", "uploading" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status: ${job.status.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(status = job.status)
                        }
                    }
                }
            }

            // Error from local job
            item {
                localJob?.errorMessage?.let { error ->
                    ErrorMessage(message = error) {
                        queueManager.retryJob(jobId)
                    }
                }
            }

            // Remote fetch error
            item {
                errorMessage?.let { error ->
                    ErrorMessage(message = "Network error: $error") {
                        fetchRemoteStatus()
                    }
                }
            }

            // Summary section
            remoteJob?.summary?.let { summary ->
                item {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SummaryRow("Total Detections", summary.totalDetections.toString())
                            SummaryRow("PPE Violations", summary.ppeViolations.toString())
                            SummaryRow("PPE Compliant", summary.ppeCompliant.toString())
                            SummaryRow("Frames Analyzed", summary.framesAnalyzed.toString())
                        }
                    }
                }
            }

            // Remote error
            remoteJob?.errorMessage?.let { error ->
                item {
                    ErrorMessage(message = error) {
                        fetchRemoteStatus()
                    }
                }
            }

            // Results list
            remoteJob?.results?.let { results ->
                if (results.isNotEmpty()) {
                    item {
                        Text(
                            text = "Detections (${results.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(results) { item ->
                        DetectionCard(item = item)
                    }
                }
            }

            // Job ID info
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Job ID",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = jobId,
                            style = MaterialTheme.typography.bodySmall
                        )
                        localJob?.let { job ->
                            Text(
                                text = "Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(job.createdAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading
            if (isLoading) {
                item {
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
