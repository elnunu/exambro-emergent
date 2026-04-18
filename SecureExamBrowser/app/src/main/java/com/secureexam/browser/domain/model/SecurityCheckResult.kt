package com.secureexam.browser.domain.model

/**
 * Represents the result of a security check.
 */
data class SecurityCheckResult(
    val checkName: String,
    val checkType: SecurityCheckType,
    val passed: Boolean,
    val message: String,
    val blocking: Boolean = true, // If true, exam cannot proceed if this check fails
    val details: String? = null
)

/**
 * Types of security checks performed.
 */
enum class SecurityCheckType {
    ROOT_DETECTION,
    DEVELOPER_OPTIONS,
    USB_DEBUGGING,
    VPN_DETECTION,
    MOCK_LOCATION,
    SCREEN_MIRRORING,
    EXTERNAL_DISPLAY,
    EMULATOR_DETECTION,
    DEVICE_ADMIN,
    SCREEN_LOCK,
    SECURE_BOOT
}

/**
 * Aggregated result of all security checks.
 */
data class SecurityStatus(
    val allChecksPassed: Boolean,
    val results: List<SecurityCheckResult>,
    val canProceed: Boolean,
    val failedBlockingChecks: List<SecurityCheckResult>
) {
    companion object {
        fun fromResults(results: List<SecurityCheckResult>): SecurityStatus {
            val failedBlocking = results.filter { !it.passed && it.blocking }
            return SecurityStatus(
                allChecksPassed = results.all { it.passed },
                results = results,
                canProceed = failedBlocking.isEmpty(),
                failedBlockingChecks = failedBlocking
            )
        }
    }
}
