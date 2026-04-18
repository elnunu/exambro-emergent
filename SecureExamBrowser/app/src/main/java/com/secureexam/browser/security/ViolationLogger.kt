package com.secureexam.browser.security

import android.content.Context
import android.util.Log
import com.secureexam.browser.data.local.ViolationDao
import com.secureexam.browser.domain.model.Violation
import com.secureexam.browser.domain.model.ViolationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logs security violations during exam sessions.
 * Provides both local logging and Moodle sync capabilities.
 */
@Singleton
class ViolationLogger @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var violationDao: ViolationDao? = null
    private var currentSessionId: String? = null
    
    companion object {
        private const val TAG = "ViolationLogger"
    }
    
    /**
     * Initialize with DAO and session ID.
     */
    fun initialize(dao: ViolationDao, sessionId: String) {
        violationDao = dao
        currentSessionId = sessionId
    }
    
    /**
     * Log a violation.
     */
    fun logViolation(
        type: ViolationType,
        description: String,
        details: String? = null
    ) {
        val sessionId = currentSessionId ?: run {
            Log.w(TAG, "No active session for violation logging")
            return
        }
        
        scope.launch {
            try {
                val violation = Violation(
                    sessionId = sessionId,
                    type = type,
                    description = description,
                    details = details,
                    timestamp = System.currentTimeMillis()
                )
                
                violationDao?.insert(violation)
                Log.d(TAG, "Logged violation: $type - $description")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log violation", e)
            }
        }
    }
    
    /**
     * Log screenshot attempt.
     */
    fun logScreenshotAttempt() {
        logViolation(
            type = ViolationType.SCREENSHOT_ATTEMPT,
            description = "Screenshot capture was attempted and blocked"
        )
    }
    
    /**
     * Log app switch attempt.
     */
    fun logAppSwitchAttempt(targetPackage: String? = null) {
        logViolation(
            type = ViolationType.APP_SWITCH_ATTEMPT,
            description = "Attempted to switch to another application",
            details = targetPackage?.let { "Target: $it" }
        )
    }
    
    /**
     * Log navigation block.
     */
    fun logNavigationBlocked(url: String) {
        logViolation(
            type = ViolationType.NAVIGATION_BLOCKED,
            description = "Navigation to unauthorized page blocked",
            details = "URL: $url"
        )
    }
    
    /**
     * Log external URL block.
     */
    fun logExternalUrlBlocked(url: String) {
        logViolation(
            type = ViolationType.EXTERNAL_URL_BLOCKED,
            description = "Access to external URL blocked",
            details = "URL: $url"
        )
    }
    
    /**
     * Log clipboard access attempt.
     */
    fun logClipboardAccess() {
        logViolation(
            type = ViolationType.CLIPBOARD_ACCESS,
            description = "Clipboard access was detected and blocked"
        )
    }
    
    /**
     * Log split screen attempt.
     */
    fun logSplitScreenAttempt() {
        logViolation(
            type = ViolationType.SPLIT_SCREEN_ATTEMPT,
            description = "Split screen mode was attempted"
        )
    }
    
    /**
     * Log picture-in-picture attempt.
     */
    fun logPipAttempt() {
        logViolation(
            type = ViolationType.PIP_ATTEMPT,
            description = "Picture-in-picture mode was attempted"
        )
    }
    
    /**
     * Log screen mirroring detection.
     */
    fun logScreenMirrorDetected() {
        logViolation(
            type = ViolationType.SCREEN_MIRROR_DETECTED,
            description = "Screen mirroring was detected during exam"
        )
    }
    
    /**
     * Log external display connection.
     */
    fun logExternalDisplayDetected(displayName: String? = null) {
        logViolation(
            type = ViolationType.EXTERNAL_DISPLAY_DETECTED,
            description = "External display connection detected",
            details = displayName?.let { "Display: $it" }
        )
    }
    
    /**
     * Log kiosk exit attempt.
     */
    fun logKioskExitAttempt() {
        logViolation(
            type = ViolationType.KIOSK_EXIT_ATTEMPT,
            description = "Attempted to exit kiosk mode without authorization"
        )
    }
    
    /**
     * Log USB debugging detection.
     */
    fun logUsbDebugDetected() {
        logViolation(
            type = ViolationType.USB_DEBUG_DETECTED,
            description = "USB debugging was enabled during exam"
        )
    }
    
    /**
     * Log VPN detection.
     */
    fun logVpnDetected() {
        logViolation(
            type = ViolationType.VPN_DETECTED,
            description = "VPN connection was detected during exam"
        )
    }
    
    /**
     * Clear session when exam ends.
     */
    fun clearSession() {
        currentSessionId = null
    }
}
