package com.secureexam.browser.security

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.view.Display
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive security checker for validating device state before exam.
 */
@Singleton
class SecurityChecker @Inject constructor(
    private val context: Context,
    private val rootDetector: RootDetector,
    private val screenMirrorDetector: ScreenMirrorDetector
) {
    
    /**
     * Check if device is rooted.
     */
    fun isDeviceRooted(): Boolean {
        return rootDetector.isRooted()
    }
    
    /**
     * Check if developer options are enabled.
     */
    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if USB debugging is enabled.
     */
    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if VPN is active.
     */
    fun isVpnActive(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if mock location is enabled.
     */
    fun isMockLocationEnabled(): Boolean {
        return try {
            // Check if any app is set as mock location provider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION
                )?.let { it != "0" } ?: false
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION,
                    0
                ) != 0
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if screen mirroring is active.
     */
    fun isScreenMirroringActive(): Boolean {
        return screenMirrorDetector.isScreenMirroringActive()
    }
    
    /**
     * Check if external display is connected.
     */
    fun isExternalDisplayConnected(): Boolean {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays
            
            // Check for non-default displays
            displays.any { display ->
                display.displayId != Display.DEFAULT_DISPLAY &&
                // Filter out virtual displays used internally
                !display.name.contains("Virtual", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if running on an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.BOARD.lowercase().contains("nox")
                || Build.BOOTLOADER.lowercase().contains("nox")
                || Build.HARDWARE.lowercase().contains("nox")
                || Build.PRODUCT.lowercase().contains("nox")
                || Build.SERIAL.lowercase().contains("nox")
    }
    
    /**
     * Check if app has device admin privileges.
     */
    fun isDeviceAdmin(): Boolean {
        return try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                    as android.app.admin.DevicePolicyManager
            val adminComponent = android.content.ComponentName(
                context,
                com.secureexam.browser.receiver.DeviceAdminReceiver::class.java
            )
            devicePolicyManager.isAdminActive(adminComponent)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if app is in lock task mode (kiosk mode).
     */
    fun isInLockTaskMode(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
            } else {
                @Suppress("DEPRECATION")
                activityManager.isInLockTaskMode
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check accessibility services that could interfere.
     */
    fun hasProblematicAccessibilityServices(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            // List of known problematic accessibility service patterns
            val problematicPatterns = listOf(
                "autoclick",
                "autotouch",
                "macro",
                "bot",
                "auto.tap",
                "screen.reader" // May need to allow for accessibility needs
            )
            
            enabledServices?.lowercase()?.let { services ->
                problematicPatterns.any { pattern -> services.contains(pattern) }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Run all security checks and return a summary.
     */
    fun runAllChecks(): SecurityCheckSummary {
        return SecurityCheckSummary(
            isRooted = isDeviceRooted(),
            developerOptionsEnabled = isDeveloperOptionsEnabled(),
            usbDebuggingEnabled = isUsbDebuggingEnabled(),
            vpnActive = isVpnActive(),
            mockLocationEnabled = isMockLocationEnabled(),
            screenMirroringActive = isScreenMirroringActive(),
            externalDisplayConnected = isExternalDisplayConnected(),
            isEmulator = isEmulator(),
            isDeviceAdmin = isDeviceAdmin(),
            isInLockTaskMode = isInLockTaskMode()
        )
    }
}

/**
 * Summary of all security check results.
 */
data class SecurityCheckSummary(
    val isRooted: Boolean,
    val developerOptionsEnabled: Boolean,
    val usbDebuggingEnabled: Boolean,
    val vpnActive: Boolean,
    val mockLocationEnabled: Boolean,
    val screenMirroringActive: Boolean,
    val externalDisplayConnected: Boolean,
    val isEmulator: Boolean,
    val isDeviceAdmin: Boolean,
    val isInLockTaskMode: Boolean
) {
    /**
     * Returns true if all critical checks pass.
     */
    val allCriticalChecksPassed: Boolean
        get() = !isRooted &&
                !developerOptionsEnabled &&
                !usbDebuggingEnabled &&
                !vpnActive &&
                !screenMirroringActive &&
                !externalDisplayConnected &&
                !isEmulator
    
    /**
     * Returns list of failed check names.
     */
    val failedChecks: List<String>
        get() = buildList {
            if (isRooted) add("Root Detection")
            if (developerOptionsEnabled) add("Developer Options")
            if (usbDebuggingEnabled) add("USB Debugging")
            if (vpnActive) add("VPN Detection")
            if (mockLocationEnabled) add("Mock Location")
            if (screenMirroringActive) add("Screen Mirroring")
            if (externalDisplayConnected) add("External Display")
            if (isEmulator) add("Emulator Detection")
        }
}
