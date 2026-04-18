package com.secureexam.browser.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.presentation.exam.ExamActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receiver for device boot events.
 * Restores kiosk mode if an exam session was active.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var securePreferences: SecurePreferences
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Device boot completed")
            
            // Check if there was an active exam session
            val session = securePreferences.loadExamSession()
            if (session != null && session.state == com.secureexam.browser.domain.model.ExamSessionState.ACTIVE) {
                Log.d(TAG, "Restoring active exam session")
                
                // Launch exam activity to restore kiosk mode
                val examIntent = Intent(context, ExamActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(examIntent)
            }
        }
    }
}
