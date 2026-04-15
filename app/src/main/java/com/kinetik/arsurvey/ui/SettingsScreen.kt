package com.kinetik.arsurvey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kinetik.arsurvey.api.ApiClient
import com.kinetik.arsurvey.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }

    var apiUrl by remember { mutableStateOf(prefsManager.getApiUrl()) }
    var apiKey by remember { mutableStateOf(prefsManager.getApiKey()) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(title = { Text("Settings") })

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text("API Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("http://10.0.2.2:8000") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            placeholder = { Text("dev-api-key-change-in-prod") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    prefsManager.setApiUrl(apiUrl)
                    prefsManager.setApiKey(apiKey)
                    ApiClient.setBaseUrl(apiUrl)
                    savedMessage = "Settings saved"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = {
                    isTesting = true
                    testResult = null
                    savedMessage = null

                    scope.launch {
                        try {
                            // Temporarily set URL for test
                            val previousUrl = ApiClient.getBaseUrl()
                            ApiClient.setBaseUrl(apiUrl)

                            val response = withContext(Dispatchers.IO) {
                                ApiClient.apiService.healthCheck()
                            }

                            testResult = if (response.status == "ok" || response.status == "healthy") {
                                "✓ Connection successful${response.version?.let { " (v$it)" } ?: ""}"
                            } else {
                                "⚠ Server responded: ${response.status}"
                            }

                            // Restore previous URL if not saved
                            ApiClient.setBaseUrl(previousUrl)
                        } catch (e: Exception) {
                            testResult = "✗ Failed: ${e.message}"
                        } finally {
                            isTesting = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Test Connection")
                }
            }
        }

        savedMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        testResult?.let { result ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.startsWith("✓"))
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AR Survey & Inspection",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 1.0.0-mvp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Internal use only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
