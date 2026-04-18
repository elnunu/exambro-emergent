package com.secureexam.browser.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.secureexam.browser.receiver.DeviceAdminReceiver
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages kiosk mode (lock task mode) for secure exam sessions.
 * Handles enabling/disabling device restrictions during exams.
 */
@Singleton
class KioskManager @Inject constructor(
    private val context: Context
) {
    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    
    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    
    private val adminComponent: ComponentName by lazy {
        ComponentName(context, DeviceAdminReceiver::class.java)
    }
    
    companion object {
        private const val TAG = "KioskManager"
    }
    
    /**
     * Check if device admin is active.
     */
    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }
    
    /**
     * Check if device owner mode is enabled (for managed devices).
     */
    fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }
    
    /**
     * Check if lock task mode is permitted for this app.
     */
    fun isLockTaskPermitted(): Boolean {
        return if (isDeviceOwner()) {
            devicePolicyManager.isLockTaskPermitted(context.packageName)
        } else {
            // Without device owner, we use screen pinning
            true
        }
    }
    
    /**
     * Check if currently in lock task (kiosk) mode.
     */
    fun isInLockTaskMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            @Suppress("DEPRECATION")
            activityManager.isInLockTaskMode
        }
    }
    
    /**
     * Start kiosk mode for the given activity.
     * Uses different strategies based on device admin status.
     */
    fun startKioskMode(activity: Activity) {
        Log.d(TAG, "Starting kiosk mode")
        
        // Apply window security flags
        applySecurityFlags(activity)
        
        // Hide system UI
        hideSystemUI(activity)
        
        // Start lock task mode
        if (isDeviceOwner()) {
            startDeviceOwnerLockTask(activity)
        } else if (isDeviceAdminActive()) {
            startAdminLockTask(activity)
        } else {
            // Fallback to screen pinning
            startScreenPinning(activity)
        }
        
        // Apply additional restrictions
        applyExamRestrictions()
    }
    
    /**
     * Stop kiosk mode.
     */
    fun stopKioskMode(activity: Activity) {
        Log.d(TAG, "Stopping kiosk mode")
        
        try {
            // Stop lock task mode
            if (isInLockTaskMode()) {
                activity.stopLockTask()
            }
            
            // Remove restrictions
            removeExamRestrictions()
            
            // Show system UI
            showSystemUI(activity)
            
            // Clear security flags
            clearSecurityFlags(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping kiosk mode", e)
        }
    }
    
    /**
     * Apply security window flags.
     */
    private fun applySecurityFlags(activity: Activity) {
        activity.window.apply {
            // Prevent screenshots and screen recording
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            
            // Keep screen on during exam
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Fullscreen
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            
            // Dismiss keyguard when app starts
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
            }
        }
    }
    
    /**
     * Clear security window flags.
     */
    private fun clearSecurityFlags(activity: Activity) {
        activity.window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }
    
    /**
     * Hide system UI (navigation bar, status bar).
     */
    @Suppress("DEPRECATION")
    private fun hideSystemUI(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = 
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
    
    /**
     * Show system UI.
     */
    @Suppress("DEPRECATION")
    private fun showSystemUI(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
    
    /**
     * Start lock task mode as device owner (fullest control).
     */
    private fun startDeviceOwnerLockTask(activity: Activity) {
        try {
            // Set allowed packages in lock task mode
            devicePolicyManager.setLockTaskPackages(
                adminComponent,
                arrayOf(context.packageName)
            )
            
            // Configure lock task features
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                devicePolicyManager.setLockTaskFeatures(
                    adminComponent,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                )
            }
            
            // Start lock task
            activity.startLockTask()
            
            Log.d(TAG, "Device owner lock task started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start device owner lock task", e)
            startScreenPinning(activity)
        }
    }
    
    /**
     * Start lock task mode with device admin (limited control).
     */
    private fun startAdminLockTask(activity: Activity) {
        try {
            activity.startLockTask()
            Log.d(TAG, "Admin lock task started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start admin lock task", e)
            startScreenPinning(activity)
        }
    }
    
    /**
     * Start screen pinning (works without device admin, but user can exit).
     */
    private fun startScreenPinning(activity: Activity) {
        try {
            activity.startLockTask()
            Log.d(TAG, "Screen pinning started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen pinning", e)
        }
    }
    
    /**
     * Apply exam-time restrictions using device policy.
     */
    private fun applyExamRestrictions() {
        if (!isDeviceOwner()) return
        
        try {
            // Disable camera
            devicePolicyManager.setCameraDisabled(adminComponent, true)
            
            // Add user restrictions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CREATE_WINDOWS)
            }
            
            Log.d(TAG, "Exam restrictions applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply exam restrictions", e)
        }
    }
    
    /**
     * Remove exam-time restrictions.
     */
    private fun removeExamRestrictions() {
        if (!isDeviceOwner()) return
        
        try {
            // Re-enable camera
            devicePolicyManager.setCameraDisabled(adminComponent, false)
            
            // Remove user restrictions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CREATE_WINDOWS)
            }
            
            Log.d(TAG, "Exam restrictions removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove exam restrictions", e)
        }
    }
    
    /**
     * Request device admin activation.
     */
    fun requestDeviceAdmin(activity: Activity, requestCode: Int) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Secure Exam Browser requires device administrator privileges to enable kiosk mode during exams."
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }
    
    /**
     * Lock the device immediately.
     */
    fun lockDevice() {
        if (isDeviceAdminActive()) {
            devicePolicyManager.lockNow()
        }
    }
}
