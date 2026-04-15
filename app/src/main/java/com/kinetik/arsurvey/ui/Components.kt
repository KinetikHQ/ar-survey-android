package com.kinetik.arsurvey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetik.arsurvey.api.ResultItem

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status.lowercase()) {
        "completed" -> MaterialTheme.colorScheme.primary to "Completed"
        "processing" -> MaterialTheme.colorScheme.secondary to "Processing"
        "uploaded" -> MaterialTheme.colorScheme.secondary to "Uploaded"
        "uploading" -> MaterialTheme.colorScheme.secondary to "Uploading"
        "queued" -> MaterialTheme.colorScheme.surfaceVariant to "Queued"
        "failed" -> MaterialTheme.colorScheme.error to "Failed"
        else -> MaterialTheme.colorScheme.surfaceVariant to status.replaceFirstChar { it.uppercase() }
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = text,
            color = if (status.lowercase() in listOf("queued")) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DetectionCard(item: ResultItem) {
    val isViolation = item.category.equals("ppe_violation", ignoreCase = true) ||
            item.metadata?.get("compliant") == false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isViolation)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${(item.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.frameTimestamp?.let { timestamp ->
                    Text(
                        text = "t=${String.format("%.1fs", timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isViolation) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠ PPE Violation detected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
