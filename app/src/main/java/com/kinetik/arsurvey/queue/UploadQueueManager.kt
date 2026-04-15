package com.kinetik.arsurvey.queue

import android.content.Context
import android.provider.Settings
import com.kinetik.arsurvey.api.ApiClient
import com.kinetik.arsurvey.api.UploadCompleteRequest
import com.kinetik.arsurvey.api.UploadInitRequest
import com.kinetik.arsurvey.data.AppDatabase
import com.kinetik.arsurvey.data.JobDao
import com.kinetik.arsurvey.data.UploadJob
import com.kinetik.arsurvey.util.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

class UploadQueueManager(context: Context) {

    private val appContext = context.applicationContext
    private val jobDao: JobDao = AppDatabase.getInstance(appContext).jobDao()
    private val prefsManager = PreferencesManager(appContext)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun saveVideo(videoPath: String): String {
        val jobId = UUID.randomUUID().toString()
        val videoFile = File(videoPath)
        val fileSize = if (videoFile.exists()) videoFile.length() else 0L

        val job = UploadJob(
            id = jobId,
            videoPath = videoPath,
            status = "queued",
            fileSizeBytes = fileSize,
            createdAt = System.currentTimeMillis()
        )

        scope.launch {
            jobDao.insert(job)
            processJob(job)
        }

        return jobId
    }

    fun processQueue() {
        scope.launch {
            val queuedJobs = jobDao.getByStatus("queued")
            val failedJobs = jobDao.getByStatus("failed")
            (queuedJobs + failedJobs).forEach { job ->
                if (isActive) processJob(job)
            }
        }
    }

    fun retryJob(jobId: String) {
        scope.launch {
            val job = jobDao.getById(jobId) ?: return@launch
            jobDao.update(job.copy(status = "queued", errorMessage = null))
            processJob(job)
        }
    }

    private suspend fun processJob(job: UploadJob) {
        try {
            // Update status to uploading
            jobDao.update(job.copy(status = "uploading"))

            val apiKey = prefsManager.getApiKey()
            val token = "Bearer $apiKey"
            val deviceId = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"

            // Step 1: Init upload
            val initResponse = ApiClient.apiService.initUpload(
                token = token,
                request = UploadInitRequest(
                    deviceId = deviceId,
                    durationSeconds = 0,
                    contentType = "video/mp4"
                )
            )

            // Step 2: Upload video to presigned URL
            val videoFile = File(job.videoPath)
            if (!videoFile.exists()) {
                throw IllegalStateException("Video file not found: ${job.videoPath}")
            }

            val requestBody = videoFile.asRequestBody("video/mp4".toMediaType())
            val uploadResponse = ApiClient.apiService.uploadVideo(
                uploadUrl = initResponse.uploadUrl,
                video = requestBody
            )

            if (!uploadResponse.isSuccessful) {
                throw RuntimeException("Upload failed with code: ${uploadResponse.code()}")
            }

            // Step 3: Complete upload
            jobDao.update(job.copy(status = "uploaded"))

            ApiClient.apiService.completeUpload(
                token = token,
                request = UploadCompleteRequest(
                    jobId = initResponse.jobId,
                    fileSizeBytes = job.fileSizeBytes
                )
            )

            // Update job status to processing
            jobDao.update(job.copy(status = "processing"))

            // Note: The local job ID may differ from the backend job ID
            // For MVP, we just update the status field

        } catch (e: Exception) {
            jobDao.update(
                job.copy(
                    status = "failed",
                    errorMessage = e.message ?: "Unknown error"
                )
            )
        }
    }

    fun cancel() {
        scope.cancel()
    }
}
