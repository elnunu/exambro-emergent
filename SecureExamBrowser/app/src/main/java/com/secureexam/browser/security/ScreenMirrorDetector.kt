package com.secureexam.browser.security

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaRouter
import android.os.Build
import android.view.Display
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects screen mirroring and casting activities.
 */
@Singleton
class ScreenMirrorDetector @Inject constructor(
    private val context: Context
) {
    
    /**
     * Check if any screen mirroring is active.
     */
    fun isScreenMirroringActive(): Boolean {
        return isMediaRouterActive() || 
               isDisplayMirroringActive() ||
               isChromecastActive()
    }
    
    /**
     * Check media router for active routes (Miracast, etc.).
     */
    private fun isMediaRouterActive(): Boolean {
        return try {
            val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
            mediaRouter?.let { router ->
                val route = router.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
                // If route is not the default device, something is casting
                route != null && route.playbackType != MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check for mirrored displays using DisplayManager.
     */
    private fun isDisplayMirroringActive(): Boolean {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays
            
            // Look for presentation displays (external screens, wireless displays)
            displays.any { display ->
                when {
                    display.displayId == Display.DEFAULT_DISPLAY -> false
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        display.flags and Display.FLAG_PRESENTATION != 0
                    }
                    else -> {
                        // Check for common wireless display names
                        val displayName = display.name?.lowercase() ?: ""
                        displayName.contains("cast") ||
                        displayName.contains("miracast") ||
                        displayName.contains("wireless") ||
                        displayName.contains("chromecast") ||
                        displayName.contains("airplay") ||
                        displayName.contains("screen mirror")
                    }
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if Chromecast is actively casting.
     */
    private fun isChromecastActive(): Boolean {
        return try {
            val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
            mediaRouter?.let { router ->
                val route = router.getSelectedRoute(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY)
                route != null && route.playbackType == MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get list of connected displays for logging.
     */
    fun getConnectedDisplays(): List<DisplayInfo> {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.displays.map { display ->
                DisplayInfo(
                    id = display.displayId,
                    name = display.name ?: "Unknown",
                    isDefault = display.displayId == Display.DEFAULT_DISPLAY,
                    isSecure = display.flags and Display.FLAG_SECURE != 0,
                    isPresentation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        display.flags and Display.FLAG_PRESENTATION != 0
                    } else false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Information about a connected display.
 */
data class DisplayInfo(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
    val isSecure: Boolean,
    val isPresentation: Boolean
)
