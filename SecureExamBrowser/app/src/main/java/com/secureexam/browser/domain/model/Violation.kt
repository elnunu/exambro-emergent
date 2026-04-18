package com.secureexam.browser.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Types of security violations that can be detected.
 */
enum class ViolationType {
    SCREENSHOT_ATTEMPT,
    SCREEN_RECORDING_ATTEMPT,
    APP_SWITCH_ATTEMPT,
    NAVIGATION_BLOCKED,
    EXTERNAL_URL_BLOCKED,
    CLIPBOARD_ACCESS,
    SPLIT_SCREEN_ATTEMPT,
    PIP_ATTEMPT,
    SCREEN_MIRROR_DETECTED,
    EXTERNAL_DISPLAY_DETECTED,
    USB_DEBUG_DETECTED,
    VPN_DETECTED,
    DEVELOPER_OPTIONS_DETECTED,
    KIOSK_EXIT_ATTEMPT,
    NOTIFICATION_ATTEMPT,
    VOLUME_BUTTON_PRESS,
    POWER_BUTTON_PRESS,
    UNKNOWN
}

/**
 * Represents a security violation during an exam session.
 * Stored in Room database and synced to Moodle.
 */
@Entity(tableName = "violations")
data class Violation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val type: ViolationType,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val details: String? = null,
    val syncedToMoodle: Boolean = false,
    val severity: Int = getSeverityForType(type)
) {
    companion object {
        /**
         * Returns severity level (1-5) for a violation type.
         * 5 = Critical, 1 = Minor
         */
        private fun getSeverityForType(type: ViolationType): Int {
            return when (type) {
                ViolationType.SCREENSHOT_ATTEMPT -> 4
                ViolationType.SCREEN_RECORDING_ATTEMPT -> 5
                ViolationType.APP_SWITCH_ATTEMPT -> 4
                ViolationType.NAVIGATION_BLOCKED -> 2
                ViolationType.EXTERNAL_URL_BLOCKED -> 3
                ViolationType.CLIPBOARD_ACCESS -> 3
                ViolationType.SPLIT_SCREEN_ATTEMPT -> 4
                ViolationType.PIP_ATTEMPT -> 4
                ViolationType.SCREEN_MIRROR_DETECTED -> 5
                ViolationType.EXTERNAL_DISPLAY_DETECTED -> 5
                ViolationType.USB_DEBUG_DETECTED -> 4
                ViolationType.VPN_DETECTED -> 3
                ViolationType.DEVELOPER_OPTIONS_DETECTED -> 3
                ViolationType.KIOSK_EXIT_ATTEMPT -> 4
                ViolationType.NOTIFICATION_ATTEMPT -> 2
                ViolationType.VOLUME_BUTTON_PRESS -> 1
                ViolationType.POWER_BUTTON_PRESS -> 2
                ViolationType.UNKNOWN -> 2
            }
        }
    }
}
