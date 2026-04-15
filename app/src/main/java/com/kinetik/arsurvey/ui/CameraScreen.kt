package com.kinetik.arsurvey.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kinetik.arsurvey.queue.UploadQueueManager
import com.kinetik.arsurvey.util.GlassesNotifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val MAX_DURATION_SECONDS = 120
private const val HIGHLIGHT_AT_SECONDS = 60
private const val LOW_LIGHT_THRESHOLD = 60  // Average pixel brightness 0-255
private const val TAG = "CameraScreen"

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    // Torch state
    var torchEnabled by remember { mutableStateOf(false) }
    var isLowLight by remember { mutableStateOf(false) }
    var lowLightDismissed by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recordingTimer: java.util.Timer? by remember { mutableStateOf(null) }

    val queueManager = remember { UploadQueueManager(context) }
    val glassesNotifier = remember { GlassesNotifier(context) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Check permission
    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTimer?.cancel()
            recordingSeconds = 0
            val timer = java.util.Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    recordingSeconds++
                    if (recordingSeconds >= MAX_DURATION_SECONDS) {
                        // Auto-stop
                        recording?.stop()
                    }
                }
            }, 1000, 1000)
            recordingTimer = timer
        } else {
            recordingTimer?.cancel()
            recordingTimer = null
        }
    }

    // Show snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Notify glasses of low light
    LaunchedEffect(isLowLight, torchEnabled) {
        if (isLowLight && !torchEnabled && !lowLightDismissed) {
            glassesNotifier.notifyLowLight()
        } else {
            glassesNotifier.clearLowLight()
        }
    }

    // Setup camera
    DisposableEffect(lifecycleOwner) {
        if (!hasCameraPermission) return@DisposableEffect onDispose {}

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                camera = boundCamera

                // Enable torch toggle listener
                torchEnabled = false

                // Low-light detection via ImageAnalysis
                val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder()
                    .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val brightness = calculateAverageBrightness(imageProxy)
                            isLowLight = brightness < LOW_LIGHT_THRESHOLD
                            imageProxy.close()
                        }
                    }

                // Rebind with analyzer
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture,
                    imageAnalyzer
                ).also { camera = it }

            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
            recordingTimer?.cancel()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera permission required")
                }
            }

            // Low-light warning banner
            if (isLowLight && !lowLightDismissed && !torchEnabled) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (isRecording) 96.dp else 32.dp)
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.95f)  // Orange
                    ),
                    onClick = {
                        // Tap to enable torch
                        torchEnabled = true
                        camera?.cameraControl?.enableTorch(true)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔦 Low light detected",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                torchEnabled = true
                                camera?.cameraControl?.enableTorch(true)
                            }
                        ) {
                            Text("Turn on torch", color = Color.White)
                        }
                        TextButton(
                            onClick = { lowLightDismissed = true }
                        ) {
                            Text("✕", color = Color.White)
                        }
                    }
                }
            }

            // Timer overlay
            if (isRecording) {
                val timerColor = if (recordingSeconds >= HIGHLIGHT_AT_SECONDS)
                    Color.Red else Color.White

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    val minutes = recordingSeconds / 60
                    val seconds = recordingSeconds % 60
                    val maxMinutes = MAX_DURATION_SECONDS / 60
                    Text(
                        text = String.format("REC %02d:%02d / %02d:00", minutes, seconds, maxMinutes),
                        color = timerColor,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Record/Stop + Torch buttons
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            ) {
                if (!hasCameraPermission) {
                    Text("Enable camera in Settings")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spaced24.dp
                    ) {
                        // Torch toggle button
                        FloatingActionButton(
                            onClick = {
                                torchEnabled = !torchEnabled
                                camera?.cameraControl?.enableTorch(torchEnabled)
                                lowLightDismissed = false  // Reset dismiss state on toggle
                            },
                            containerColor = if (torchEnabled) Color(0xFFFFC107) else Color.DarkGray,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Text(
                                text = if (torchEnabled) "🔦" else "💡",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }

                        // Record button
                        Button(
                            onClick = {
                                if (!isRecording) {
                                    // Start recording
                                val vc = videoCapture ?: return@Button
                                val outputFile = createVideoFile(context)
                                val outputOptions = FileOutputOptions.Builder(outputFile).build()

                                val activeRecording = vc.output
                                    .prepareRecording(context, outputOptions)
                                    .apply {
                                        if (ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            withAudioEnabled()
                                        }
                                    }
                                    .start(ContextCompat.getMainExecutor(context)) { event ->
                                        when (event) {
                                            is VideoRecordEvent.Start -> {
                                                isRecording = true
                                                glassesNotifier.notifyRecordingStarted()
                                            }
                                            is VideoRecordEvent.Finalize -> {
                                                isRecording = false
                                                glassesNotifier.clearRecording()
                                                if (!event.hasError()) {
                                                    val jobId = queueManager.saveVideo(outputFile.absolutePath)
                                                    snackbarMessage = "Clip queued for upload"
                                                } else {
                                                    snackbarMessage = "Recording error: ${event.error}"
                                                }
                                            }
                                        }
                                    }

                                recording = activeRecording
                            } else {
                                // Stop recording
                                recording?.stop()
                                recording = null
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Gray else Color.Red
                        )
                    ) {
                        Text(
                            text = if (isRecording) "■" else "●",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                    } // end Row
                }
            }
        }
    }
}

private fun createVideoFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val dir = File(context.filesDir, "videos").apply { mkdirs() }
    return File(dir, "SURVEY_$timestamp.mp4")
}

/**
 * Calculate average pixel brightness from a CameraX ImageProxy.
 * Returns 0-255 where 0 is black and 255 is white.
 * Samples the Y (luminance) plane only for speed.
 */
private fun calculateAverageBrightness(imageProxy: androidx.camera.core.ImageProxy): Int {
    val yPlane = imageProxy.planes[0]
    val buffer = yPlane.buffer
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride

    // Sample every 10th pixel for speed
    var sum = 0L
    var count = 0
    val sampleStep = 10

    for (y in 0 until height step sampleStep) {
        for (x in 0 until width step sampleStep) {
            val index = y * rowStride + x * pixelStride
            if (index < buffer.capacity()) {
                sum += buffer.get(index).toInt() and 0xFF
                count++
            }
        }
    }

    return if (count > 0) (sum / count).toInt() else 128
}
