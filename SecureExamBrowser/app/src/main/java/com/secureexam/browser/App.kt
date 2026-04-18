package com.secureexam.browser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Secure Exam Browser.
 * Initializes Hilt dependency injection and notification channels.
 */
@HiltAndroidApp
class App : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_EXAM = "exam_session_channel"
        const val NOTIFICATION_ID_EXAM = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val examChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_EXAM,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(examChannel)
        }
    }
}
