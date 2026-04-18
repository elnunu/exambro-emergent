package com.secureexam.browser.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a Safe Exam Browser configuration file structure.
 * Used for parsing .seb configuration files.
 */
data class SebConfig(
    @SerializedName("startURL")
    val startUrl: String? = null,
    
    @SerializedName("startResource")
    val startResource: String? = null,
    
    @SerializedName("sebServerURL")
    val sebServerUrl: String? = null,
    
    @SerializedName("hashedQuitPassword")
    val hashedQuitPassword: String? = null,
    
    @SerializedName("hashedAdminPassword")
    val hashedAdminPassword: String? = null,
    
    @SerializedName("allowQuit")
    val allowQuit: Boolean = false,
    
    @SerializedName("ignoreExitKeys")
    val ignoreExitKeys: Boolean = true,
    
    @SerializedName("enableSebBrowser")
    val enableSebBrowser: Boolean = true,
    
    @SerializedName("browserViewMode")
    val browserViewMode: Int = 0,
    
    @SerializedName("mainBrowserWindowWidth")
    val mainBrowserWindowWidth: Int = 100,
    
    @SerializedName("mainBrowserWindowHeight")
    val mainBrowserWindowHeight: Int = 100,
    
    @SerializedName("enableJavaScript")
    val enableJavaScript: Boolean = true,
    
    @SerializedName("blockPopUpWindows")
    val blockPopUpWindows: Boolean = true,
    
    @SerializedName("allowBrowsingBackForward")
    val allowBrowsingBackForward: Boolean = false,
    
    @SerializedName("enablePlugIns")
    val enablePlugins: Boolean = false,
    
    @SerializedName("allowDownUploads")
    val allowDownUploads: Boolean = true,
    
    @SerializedName("chooseFileToUploadPolicy")
    val chooseFileToUploadPolicy: Int = 0,
    
    @SerializedName("downloadPDFFiles")
    val downloadPdfFiles: Boolean = false,
    
    @SerializedName("allowPDFPlugIn")
    val allowPdfPlugin: Boolean = false,
    
    @SerializedName("sendBrowserExamKey")
    val sendBrowserExamKey: Boolean = true,
    
    @SerializedName("browserExamKey")
    val browserExamKey: String? = null,
    
    @SerializedName("examSessionClearCookiesOnEnd")
    val examSessionClearCookiesOnEnd: Boolean = true,
    
    @SerializedName("URLFilterEnable")
    val urlFilterEnable: Boolean = true,
    
    @SerializedName("URLFilterEnableContentFilter")
    val urlFilterEnableContentFilter: Boolean = false,
    
    @SerializedName("URLFilterRules")
    val urlFilterRules: List<UrlFilterRule>? = null,
    
    @SerializedName("blacklistURLFilter")
    val blacklistUrlFilter: String? = null,
    
    @SerializedName("whitelistURLFilter")
    val whitelistUrlFilter: String? = null,
    
    @SerializedName("showTaskBar")
    val showTaskBar: Boolean = false,
    
    @SerializedName("showReloadButton")
    val showReloadButton: Boolean = true,
    
    @SerializedName("showTime")
    val showTime: Boolean = true,
    
    @SerializedName("showInputLanguage")
    val showInputLanguage: Boolean = false,
    
    @SerializedName("enableAudioControl")
    val enableAudioControl: Boolean = false,
    
    @SerializedName("audioMute")
    val audioMute: Boolean = false,
    
    @SerializedName("audioVolumeLevel")
    val audioVolumeLevel: Int = 25,
    
    @SerializedName("createNewDesktop")
    val createNewDesktop: Boolean = true,
    
    @SerializedName("killExplorerShell")
    val killExplorerShell: Boolean = false
)

/**
 * URL filter rule for SEB configuration.
 */
data class UrlFilterRule(
    @SerializedName("active")
    val active: Boolean = true,
    
    @SerializedName("regex")
    val regex: Boolean = false,
    
    @SerializedName("expression")
    val expression: String? = null,
    
    @SerializedName("action")
    val action: Int = 0 // 0 = block, 1 = allow
)

/**
 * Extension function to convert SEB config to our ExamConfig.
 */
fun SebConfig.toExamConfig(exitPasswordHash: String): ExamConfig {
    val moodleUrl = startUrl ?: sebServerUrl ?: ""
    val allowedUrls = mutableListOf<String>()
    
    // Add whitelist URLs
    whitelistUrlFilter?.let {
        allowedUrls.addAll(it.split(";").filter { url -> url.isNotBlank() })
    }
    
    // Add URLs from filter rules
    urlFilterRules?.filter { it.active && it.action == 1 }?.forEach { rule ->
        rule.expression?.let { allowedUrls.add(it) }
    }
    
    return ExamConfig(
        moodleUrl = moodleUrl,
        exitPasswordHash = exitPasswordHash,
        startUrl = startUrl,
        allowedUrls = allowedUrls,
        blockPopups = blockPopUpWindows,
        enableJavaScript = enableJavaScript,
        allowDownloads = !downloadPdfFiles,
        allowUploads = allowDownUploads,
        sendBrowserExamKey = sendBrowserExamKey
    )
}
