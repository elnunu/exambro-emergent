package com.secureexam.browser.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver for screen state changes.
 * Used to detect when screen is turned off during exam.
 */
class ScreenStateReceiver : BroadcastReceiver() {
    
    var listener: ScreenStateListener? = null
    
    companion object {
        private const val TAG = "ScreenStateReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned on")
                listener?.onScreenOn()
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen turned off")
                listener?.onScreenOff()
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "User present (unlocked)")
                listener?.onUserPresent()
            }
        }
    }
    
    interface ScreenStateListener {
        fun onScreenOn()
        fun onScreenOff()
        fun onUserPresent()
    }
}
