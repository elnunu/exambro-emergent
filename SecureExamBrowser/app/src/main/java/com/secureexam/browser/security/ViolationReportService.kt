package com.secureexam.browser.security

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.domain.repository.ViolationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background service for syncing violations to Moodle.
 */
@AndroidEntryPoint
class ViolationReportService : Service() {
    
    @Inject
    lateinit var violationRepository: ViolationRepository
    
    @Inject
    lateinit var securePreferences: SecurePreferences
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        const val ACTION_SYNC = "com.secureexam.browser.SYNC_VIOLATIONS"
        const val EXTRA_MOODLE_URL = "moodle_url"
        const val EXTRA_TOKEN = "token"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SYNC -> {
                val moodleUrl = intent.getStringExtra(EXTRA_MOODLE_URL) ?: return START_NOT_STICKY
                val token = intent.getStringExtra(EXTRA_TOKEN) ?: return START_NOT_STICKY
                syncViolations(moodleUrl, token)
            }
        }
        return START_NOT_STICKY
    }
    
    private fun syncViolations(moodleUrl: String, token: String) {
        scope.launch {
            try {
                violationRepository.syncToMoodle(moodleUrl, token)
            } catch (e: Exception) {
                // Log error, retry later
            } finally {
                stopSelf()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
