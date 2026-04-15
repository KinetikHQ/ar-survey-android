package com.kinetik.arsurvey.api

import com.google.gson.annotations.SerializedName

data class UploadInitRequest(
    val deviceId: String,
    val durationSeconds: Int,
    val contentType: String = "video/mp4"
)

data class UploadInitResponse(
    val jobId: String,
    val uploadUrl: String,
    val expiresAt: String
)

data class UploadCompleteRequest(
    val jobId: String,
    val fileSizeBytes: Long
)

data class UploadCompleteResponse(
    val jobId: String,
    val status: String
)

data class JobDetailResponse(
    val jobId: String,
    val status: String,
    val createdAt: String,
    val startedAt: String?,
    val completedAt: String?,
    val results: List<ResultItem>?,
    val summary: Summary?,
    val errorMessage: String?
)

data class ResultItem(
    val id: String,
    val category: String,
    val label: String,
    val confidence: Float,
    val bbox: List<Float>?,
    val frameTimestamp: Float?,
    val metadata: Map<String, Any>?
)

data class Summary(
    val totalDetections: Int,
    val ppeViolations: Int,
    val ppeCompliant: Int,
    val framesAnalyzed: Int
)

data class RetryResponse(
    val jobId: String,
    val status: String
)

data class HealthResponse(
    val status: String,
    val version: String?
)
