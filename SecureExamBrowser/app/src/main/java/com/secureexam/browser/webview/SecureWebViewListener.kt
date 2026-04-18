package com.secureexam.browser.webview

/**
 * Listener interface for SecureWebView events.
 */
interface SecureWebViewListener {
    
    /**
     * Called when page starts loading.
     */
    fun onPageStarted(url: String)
    
    /**
     * Called when page finishes loading.
     */
    fun onPageFinished(url: String)
    
    /**
     * Called when loading progress changes.
     */
    fun onProgressChanged(progress: Int)
    
    /**
     * Called when a URL is blocked due to whitelist.
     */
    fun onUrlBlocked(url: String)
    
    /**
     * Called when a page load error occurs.
     */
    fun onError(message: String)
    
    /**
     * Called when exam is detected as submitted.
     */
    fun onExamSubmitted()
    
    /**
     * Called when exam is detected as started.
     */
    fun onExamStarted()
}
