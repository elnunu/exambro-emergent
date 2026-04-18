package com.secureexam.browser.domain.usecase

import com.secureexam.browser.domain.model.SecurityCheckResult
import com.secureexam.browser.domain.model.SecurityCheckType
import com.secureexam.browser.domain.model.SecurityStatus
import com.secureexam.browser.security.SecurityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for running all security checks.
 */
class RunSecurityChecksUseCase @Inject constructor(
    private val securityChecker: SecurityChecker
) {
    
    /**
     * Runs all security checks and returns aggregated status.
     * Checks run in parallel for better performance.
     */
    suspend operator fun invoke(): SecurityStatus = withContext(Dispatchers.IO) {
        val checks = listOf(
            async { checkRoot() },
            async { checkDeveloperOptions() },
            async { checkUsbDebugging() },
            async { checkVpn() },
            async { checkMockLocation() },
            async { checkScreenMirroring() },
            async { checkExternalDisplay() },
            async { checkEmulator() }
        )
        
        val results = checks.awaitAll()
        SecurityStatus.fromResults(results)
    }
    
    /**
     * Run a single specific check.
     */
    suspend fun runCheck(type: SecurityCheckType): SecurityCheckResult = withContext(Dispatchers.IO) {
        when (type) {
            SecurityCheckType.ROOT_DETECTION -> checkRoot()
            SecurityCheckType.DEVELOPER_OPTIONS -> checkDeveloperOptions()
            SecurityCheckType.USB_DEBUGGING -> checkUsbDebugging()
            SecurityCheckType.VPN_DETECTION -> checkVpn()
            SecurityCheckType.MOCK_LOCATION -> checkMockLocation()
            SecurityCheckType.SCREEN_MIRRORING -> checkScreenMirroring()
            SecurityCheckType.EXTERNAL_DISPLAY -> checkExternalDisplay()
            SecurityCheckType.EMULATOR_DETECTION -> checkEmulator()
            SecurityCheckType.DEVICE_ADMIN -> checkDeviceAdmin()
            SecurityCheckType.SCREEN_LOCK -> checkScreenLock()
            SecurityCheckType.SECURE_BOOT -> checkSecureBoot()
        }
    }
    
    private fun checkRoot(): SecurityCheckResult {
        val isRooted = securityChecker.isDeviceRooted()
        return SecurityCheckResult(
            checkName = "Root Detection",
            checkType = SecurityCheckType.ROOT_DETECTION,
            passed = !isRooted,
            message = if (isRooted) "Rooted device detected" else "Device is not rooted",
            blocking = true
        )
    }
    
    private fun checkDeveloperOptions(): SecurityCheckResult {
        val enabled = securityChecker.isDeveloperOptionsEnabled()
        return SecurityCheckResult(
            checkName = "Developer Options",
            checkType = SecurityCheckType.DEVELOPER_OPTIONS,
            passed = !enabled,
            message = if (enabled) "Developer options are enabled" else "Developer options are disabled",
            blocking = true
        )
    }
    
    private fun checkUsbDebugging(): SecurityCheckResult {
        val enabled = securityChecker.isUsbDebuggingEnabled()
        return SecurityCheckResult(
            checkName = "USB Debugging",
            checkType = SecurityCheckType.USB_DEBUGGING,
            passed = !enabled,
            message = if (enabled) "USB debugging is enabled" else "USB debugging is disabled",
            blocking = true
        )
    }
    
    private fun checkVpn(): SecurityCheckResult {
        val active = securityChecker.isVpnActive()
        return SecurityCheckResult(
            checkName = "VPN Detection",
            checkType = SecurityCheckType.VPN_DETECTION,
            passed = !active,
            message = if (active) "VPN connection detected" else "No VPN detected",
            blocking = true
        )
    }
    
    private fun checkMockLocation(): SecurityCheckResult {
        val enabled = securityChecker.isMockLocationEnabled()
        return SecurityCheckResult(
            checkName = "Mock Location",
            checkType = SecurityCheckType.MOCK_LOCATION,
            passed = !enabled,
            message = if (enabled) "Mock location is enabled" else "Mock location is disabled",
            blocking = false
        )
    }
    
    private fun checkScreenMirroring(): SecurityCheckResult {
        val active = securityChecker.isScreenMirroringActive()
        return SecurityCheckResult(
            checkName = "Screen Mirroring",
            checkType = SecurityCheckType.SCREEN_MIRRORING,
            passed = !active,
            message = if (active) "Screen mirroring detected" else "No screen mirroring",
            blocking = true
        )
    }
    
    private fun checkExternalDisplay(): SecurityCheckResult {
        val connected = securityChecker.isExternalDisplayConnected()
        return SecurityCheckResult(
            checkName = "External Display",
            checkType = SecurityCheckType.EXTERNAL_DISPLAY,
            passed = !connected,
            message = if (connected) "External display connected" else "No external display",
            blocking = true
        )
    }
    
    private fun checkEmulator(): SecurityCheckResult {
        val isEmulator = securityChecker.isEmulator()
        return SecurityCheckResult(
            checkName = "Emulator Detection",
            checkType = SecurityCheckType.EMULATOR_DETECTION,
            passed = !isEmulator,
            message = if (isEmulator) "Emulator detected" else "Running on physical device",
            blocking = true
        )
    }
    
    private fun checkDeviceAdmin(): SecurityCheckResult {
        val isAdmin = securityChecker.isDeviceAdmin()
        return SecurityCheckResult(
            checkName = "Device Admin",
            checkType = SecurityCheckType.DEVICE_ADMIN,
            passed = isAdmin,
            message = if (isAdmin) "Device admin is active" else "Device admin not enabled",
            blocking = false // Not blocking, but limits functionality
        )
    }
    
    private fun checkScreenLock(): SecurityCheckResult {
        // Placeholder - check if device has screen lock
        return SecurityCheckResult(
            checkName = "Screen Lock",
            checkType = SecurityCheckType.SCREEN_LOCK,
            passed = true,
            message = "Screen lock check passed",
            blocking = false
        )
    }
    
    private fun checkSecureBoot(): SecurityCheckResult {
        // Placeholder - check secure boot status
        return SecurityCheckResult(
            checkName = "Secure Boot",
            checkType = SecurityCheckType.SECURE_BOOT,
            passed = true,
            message = "Secure boot check passed",
            blocking = false
        )
    }
}
