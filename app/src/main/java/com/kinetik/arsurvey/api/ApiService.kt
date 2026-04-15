package com.kinetik.arsurvey.api

import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST("/api/v1/upload/init")
    suspend fun initUpload(
        @Header("Authorization") token: String,
        @Body request: UploadInitRequest
    ): UploadInitResponse

    @PUT
    suspend fun uploadVideo(
        @Url uploadUrl: String,
        @Body video: RequestBody
    ): retrofit2.Response<Unit>

    @POST("/api/v1/upload/complete")
    suspend fun completeUpload(
        @Header("Authorization") token: String,
        @Body request: UploadCompleteRequest
    ): UploadCompleteResponse

    @GET("/api/v1/jobs/{jobId}")
    suspend fun getJob(
        @Header("Authorization") token: String,
        @Path("jobId") jobId: String
    ): JobDetailResponse

    @POST("/api/v1/jobs/{jobId}/retry")
    suspend fun retryJob(
        @Header("Authorization") token: String,
        @Path("jobId") jobId: String
    ): RetryResponse

    @GET("/health")
    suspend fun healthCheck(): HealthResponse
}
