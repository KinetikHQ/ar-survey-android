package com.kinetik.arsurvey.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kinetik.arsurvey.R

/**
 * Sends notifications to Meta Ray-Ban Display glasses.
 *
 * Two integration paths:
 * 1. Android NotificationChannel — mirrors to glasses display automatically
 *    (Meta glasses show notifications from the companion phone)
 * 2. Meta SDK — direct glasses API (placeholder for when SDK is integrated)
 *
 * For MVP: uses Android notifications which appear on both phone and glasses.
 */
class GlassesNotifier(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "ar_survey_glasses"
        private const val CHANNEL_NAME = "AR Survey — Glasses Alerts"
        private const val NOTIFICATION_ID_LOW_LIGHT = 1001
        private const val NOTIFICATION_ID_RECORDING = 1002
        private const val NOTIFICATION_ID_RESULT = 1003
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts shown on Meta Ray-Ban Display glasses"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Alert user on glasses that the room is poorly lit.
     * Shows: "🔦 Low light — Enable torch for better detection"
     */
    fun notifyLowLight() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Low Light Detected")
            .setContentText("Room is too dark. Enable torch for accurate PPE detection.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(10_000)  // Auto-dismiss after 10s
            .build()

        notificationManager.notify(NOTIFICATION_ID_LOW_LIGHT, notification)
    }

    /**
     * Clear the low-light notification (e.g., when torch is turned on).
     */
    fun clearLowLight() {
        notificationManager.cancel(NOTIFICATION_ID_LOW_LIGHT)
    }

    /**
     * Notify glasses when recording starts.
     */
    fun notifyRecordingStarted() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("🔴 Recording")
            .setContentText("Inspection clip recording in progress")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)  // Persistent during recording
            .build()

        notificationManager.notify(NOTIFICATION_ID_RECORDING, notification)
    }

    /**
     * Clear recording notification.
     */
    fun clearRecording() {
        notificationManager.cancel(NOTIFICATION_ID_RECORDING)
    }

    /**
     * Notify glasses when processing is complete with results.
     */
    fun notifyResultsReady(violations: Int, compliant: Int) {
        val title = if (violations > 0) "⚠️ $violations PPE Violations Found" else "✅ All PPE Compliant"
        val text = "$violations violations, $compliant compliant"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .build()

        notificationManager.notify(NOTIFICATION_ID_RESULT, notification)
    }

    // -----------------------------------------------------------------------
    // Meta SDK integration (placeholder)
    // -----------------------------------------------------------------------

    /**
     * When Meta DAT SDK is integrated, this method will send directly to glasses.
     *
     * Expected flow:
     * 1. Meta SDK provides a WearableNotificationManager or similar
     * 2. We push text/card directly to glasses display
     * 3. User sees it in their field of view without checking phone
     *
     * For now, falls back to Android notification.
     */
    fun sendToGlasses(title: String, body: String) {
        // TODO: Replace with Meta SDK call when available
        // metaWearableManager?.sendNotification(title, body)

        // Fallback: Android notification (mirrors to glasses)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
