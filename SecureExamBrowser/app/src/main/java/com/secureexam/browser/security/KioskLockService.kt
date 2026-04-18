package com.secureexam.browser.security

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.secureexam.browser.App
import com.secureexam.browser.R
import com.secureexam.browser.presentation.exam.ExamActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that maintains kiosk lock during exam.
 * Keeps the app running in foreground to prevent system from killing it.
 */
@AndroidEntryPoint
class KioskLockService : Service() {
    
    @Inject
    lateinit var securityChecker: SecurityChecker
    
    @Inject
    lateinit var violationLogger: ViolationLogger
    
    private var isMonitoring = false
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.secureexam.browser.START_KIOSK"
        const val ACTION_STOP = "com.secureexam.browser.STOP_KIOSK"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startKioskLock()
            ACTION_STOP -> stopKioskLock()
        }
        return START_STICKY
    }
    
    private fun startKioskLock() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        // Start foreground notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start monitoring for security violations
        startSecurityMonitoring()
    }
    
    private fun stopKioskLock() {
        isMonitoring = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ExamActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_EXAM)
            .setContentTitle(getString(R.string.notification_exam_active))
            .setContentText(getString(R.string.notification_tap_to_return))
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun startSecurityMonitoring() {
        // Periodic security checks would be implemented here
        // For example, checking for screen mirroring, VPN, etc.
        // Using a Handler or CoroutineScope to periodically check
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Restart the service if it gets killed
        if (isMonitoring) {
            val restartIntent = Intent(this, KioskLockService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
        }
    }
}
