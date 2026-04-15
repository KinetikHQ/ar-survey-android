package com.kinetik.arsurvey.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upload_jobs")
data class UploadJob(
    @PrimaryKey
    val id: String,
    val videoPath: String,
    val status: String, // queued, uploading, uploaded, processing, completed, failed
    val fileSizeBytes: Long,
    val createdAt: Long,
    val errorMessage: String? = null
)
