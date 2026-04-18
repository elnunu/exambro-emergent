package com.secureexam.browser.security

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive root detection using multiple methods.
 */
@Singleton
class RootDetector @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // Common paths where su binary is found on rooted devices
        private val SU_PATHS = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/su/bin",
            "/magisk/.core/bin/su",
            "/system/xbin/daemonsu"
        )
        
        // Known root management apps
        private val ROOT_PACKAGES = arrayOf(
            "com.topjohnwu.magisk",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hiderootPremium",
            "com.formyhm.hideroot"
        )
        
        // Dangerous props that indicate root
        private val DANGEROUS_PROPS = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0",
            "ro.build.selinux" to "0",
            "ro.build.tags" to "test-keys"
        )
    }
    
    /**
     * Main method to check if device is rooted.
     * Uses multiple detection methods.
     */
    fun isRooted(): Boolean {
        return checkForSuBinary() ||
                checkForRootManagementApps() ||
                checkForDangerousProps() ||
                checkForRWPaths() ||
                checkSuExists() ||
                checkForBusybox() ||
                detectTestKeys()
    }
    
    /**
     * Check if su binary exists in common paths.
     */
    private fun checkForSuBinary(): Boolean {
        return SU_PATHS.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Check if known root management apps are installed.
     */
    private fun checkForRootManagementApps(): Boolean {
        val pm = context.packageManager
        return ROOT_PACKAGES.any { packageName ->
            try {
                pm.getPackageInfo(packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Check for dangerous system properties.
     */
    private fun checkForDangerousProps(): Boolean {
        return DANGEROUS_PROPS.any { (prop, dangerousValue) ->
            try {
                val value = getSystemProperty(prop)
                value == dangerousValue
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Check if system paths are writable (they shouldn't be on non-rooted devices).
     */
    private fun checkForRWPaths(): Boolean {
        val lines = readMountFile()
        val pathsThatShouldNotBeWritable = arrayOf(
            "/system",
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin",
            "/etc"
        )
        
        return lines.any { line ->
            pathsThatShouldNotBeWritable.any { path ->
                line.contains(path) && line.contains("rw")
            }
        }
    }
    
    /**
     * Try to execute su command.
     */
    private fun checkSuExists(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            process.destroy()
            line != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if busybox is installed (commonly used with root).
     */
    private fun checkForBusybox(): Boolean {
        val busyboxPaths = arrayOf(
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/sbin/busybox"
        )
        return busyboxPaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Check if build is signed with test keys.
     */
    private fun detectTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    /**
     * Read system property using reflection.
     */
    private fun getSystemProperty(propName: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $propName")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            process.destroy()
            result
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Read /proc/mounts file.
     */
    private fun readMountFile(): List<String> {
        return try {
            File("/proc/mounts").readLines()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
