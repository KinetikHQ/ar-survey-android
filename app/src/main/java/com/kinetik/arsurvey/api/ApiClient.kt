package com.kinetik.arsurvey.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"

    @Volatile
    private var baseUrl: String = DEFAULT_BASE_URL

    @Volatile
    private var retrofit: Retrofit? = null

    fun setBaseUrl(url: String) {
        val normalized = url.trimEnd('/')
        if (normalized != baseUrl) {
            baseUrl = normalized
            retrofit = null
        }
    }

    fun getBaseUrl(): String = baseUrl

    private fun buildRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService
        get() {
            if (retrofit == null) {
                synchronized(this) {
                    if (retrofit == null) {
                        retrofit = buildRetrofit()
                    }
                }
            }
            return retrofit!!.create(ApiService::class.java)
        }
}
