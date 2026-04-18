package com.secureexam.browser.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the exam configuration.
 * Can be loaded from .seb file, JSON config, or manual entry.
 */
@Parcelize
data class ExamConfig(
    val moodleUrl: String,
    val exitPasswordHash: String,
    val examToken: String? = null,
    val startUrl: String? = null,
    val allowedUrls: List<String> = emptyList(),
    val blockPopups: Boolean = true,
    val enableJavaScript: Boolean = true,
    val allowDownloads: Boolean = false,
    val allowUploads: Boolean = true,
    val userAgent: String = "SecureExamBrowser/1.0 (Android; SEB)",
    val sendBrowserExamKey: Boolean = true,
    val examSessionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Returns the base domain from Moodle URL for whitelisting.
     */
    fun getBaseDomain(): String {
        return try {
            val url = java.net.URL(moodleUrl)
            url.host
        } catch (e: Exception) {
            moodleUrl
        }
    }

    /**
     * Checks if a URL is allowed based on whitelist.
     */
    fun isUrlAllowed(url: String): Boolean {
        val baseDomain = getBaseDomain()
        return try {
            val checkUrl = java.net.URL(url)
            checkUrl.host == baseDomain || 
                checkUrl.host.endsWith(".$baseDomain") ||
                allowedUrls.any { allowed ->
                    url.startsWith(allowed) || checkUrl.host.contains(allowed)
                }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun empty() = ExamConfig(
            moodleUrl = "",
            exitPasswordHash = ""
        )
    }
}
