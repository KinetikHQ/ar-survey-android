package com.kinetik.arsurvey.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ar_survey_prefs", Context.MODE_PRIVATE)

    fun getApiUrl(): String {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }

    fun setApiUrl(url: String) {
        prefs.edit().putString(KEY_API_URL, url).apply()
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    companion object {
        private const val KEY_API_URL = "api_url"
        private const val KEY_API_KEY = "api_key"
        private const val DEFAULT_API_URL = "http://10.0.2.2:8000"
        private const val DEFAULT_API_KEY = "dev-api-key-change-in-prod"
    }
}
