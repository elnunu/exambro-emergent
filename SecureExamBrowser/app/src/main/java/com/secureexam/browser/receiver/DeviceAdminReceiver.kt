package com.secureexam.browser.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.secureexam.browser.R

/**
 * Device Admin Receiver for managing device policies.
 * Required for kiosk mode and device restrictions.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device admin enabled")
        showToast(context, "Secure Exam Browser: Device admin enabled")
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device admin disabled")
        showToast(context, "Secure Exam Browser: Device admin disabled")
    }
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device admin disable requested")
        return "Disabling device admin will prevent kiosk mode during exams. Are you sure?"
    }
    
    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, userHandle)
        Log.d(TAG, "Password changed")
    }
    
    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
        Log.d(TAG, "Password failed")
    }
    
    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        Log.d(TAG, "Password succeeded")
    }
    
    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "Entering lock task mode: $pkg")
    }
    
    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "Exiting lock task mode")
    }
    
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
