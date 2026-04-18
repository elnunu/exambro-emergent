package com.secureexam.browser.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.secureexam.browser.BuildConfig
import com.secureexam.browser.domain.model.ExamConfig

/**
 * Hardened WebView for secure exam browsing.
 * Implements URL whitelisting, SSL validation, and security restrictions.
 */
@SuppressLint("SetJavaScriptEnabled")
class SecureWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    
    private var examConfig: ExamConfig? = null
    private var listener: SecureWebViewListener? = null
    
    companion object {
        private const val TAG = "SecureWebView"
        private const val JS_INTERFACE_NAME = "SecureExamBrowser"
    }
    
    init {
        configureWebView()
    }
    
    /**
     * Configure WebView settings for security.
     */
    private fun configureWebView() {
        settings.apply {
            // JavaScript (required for Moodle)
            javaScriptEnabled = true
            
            // Disable caching for exam content
            cacheMode = WebSettings.LOAD_NO_CACHE
            
            // Disable storage that could be used to store exam content
            domStorageEnabled = true // Moodle needs this
            databaseEnabled = false
            
            // Block file access
            allowFileAccess = false
            allowContentAccess = false
            
            // Disable geolocation
            setGeolocationEnabled(false)
            
            // Block mixed content
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            
            // Disable zoom (prevents screenshot workarounds)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            
            // Custom user agent for SEB identification
            userAgentString = BuildConfig.SEB_USER_AGENT
            
            // Standard settings
            loadWithOverviewMode = true
            useWideViewPort = true
            
            // Media settings
            mediaPlaybackRequiresUserGesture = true
            
            // Disable save form data
            saveFormData = false
            
            // Disable password saving
            savePassword = false
        }
        
        // Disable long click (copy/paste)
        isLongClickable = false
        setOnLongClickListener { true }
        
        // Disable haptic feedback
        isHapticFeedbackEnabled = false
        
        // Clear any existing data
        clearCache(true)
        clearHistory()
        clearFormData()
        
        // Clear cookies from previous sessions
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        
        // Add JavaScript interface for Moodle SEB communication
        addJavascriptInterface(WebAppInterface(), JS_INTERFACE_NAME)
    }
    
    /**
     * Initialize with exam configuration.
     */
    fun initialize(config: ExamConfig, webViewListener: SecureWebViewListener) {
        examConfig = config
        listener = webViewListener
        
        // Update user agent if specified in config
        settings.userAgentString = config.userAgent
        
        // Set custom WebViewClient
        webViewClient = SecureWebViewClient(config, webViewListener)
        
        // Set custom WebChromeClient
        webChromeClient = SecureWebChromeClient(webViewListener)
    }
    
    /**
     * Load the exam URL.
     */
    fun loadExamUrl() {
        examConfig?.let { config ->
            val url = config.startUrl ?: config.moodleUrl
            Log.d(TAG, "Loading exam URL: $url")
            loadUrl(url)
        }
    }
    
    /**
     * Check if URL is allowed.
     */
    fun isUrlAllowed(url: String): Boolean {
        return examConfig?.isUrlAllowed(url) ?: false
    }
    
    /**
     * Clear all browsing data.
     */
    fun clearBrowsingData() {
        clearCache(true)
        clearHistory()
        clearFormData()
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
    }
    
    /**
     * Inject SEB JavaScript for Moodle detection.
     */
    fun injectSebScript() {
        val script = """
            (function() {
                window.SafeExamBrowser = {
                    version: '1.0',
                    security: {
                        configKey: '${examConfig?.examSessionId ?: ""}'  ,
                        browserExamKey: '${generateBrowserExamKey()}'
                    }
                };
                
                // Notify Moodle that SEB is present
                if (typeof window.sebReady === 'function') {
                    window.sebReady();
                }
                
                // Detect exam submission
                var originalSubmit = HTMLFormElement.prototype.submit;
                HTMLFormElement.prototype.submit = function() {
                    if (this.action && this.action.includes('attempt.php')) {
                        window.$JS_INTERFACE_NAME.onExamSubmit();
                    }
                    return originalSubmit.apply(this, arguments);
                };
            })();
        """.trimIndent()
        
        evaluateJavascript(script, null)
    }
    
    /**
     * Generate browser exam key for Moodle SEB plugin.
     */
    private fun generateBrowserExamKey(): String {
        // This would normally be generated based on the exam config
        // For now, return a placeholder
        return examConfig?.examSessionId ?: "default-key"
    }
    
    /**
     * Handle keyboard visibility.
     */
    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
    
    /**
     * JavaScript interface for communication between WebView and app.
     */
    inner class WebAppInterface {
        
        @JavascriptInterface
        fun onExamSubmit() {
            Log.d(TAG, "Exam submitted via JavaScript")
            listener?.onExamSubmitted()
        }
        
        @JavascriptInterface
        fun onExamStart() {
            Log.d(TAG, "Exam started via JavaScript")
            listener?.onExamStarted()
        }
        
        @JavascriptInterface
        fun log(message: String) {
            Log.d(TAG, "JS Log: $message")
        }
        
        @JavascriptInterface
        fun getConfigKey(): String {
            return examConfig?.examSessionId ?: ""
        }
        
        @JavascriptInterface
        fun getBrowserExamKey(): String {
            return generateBrowserExamKey()
        }
    }
}

/**
 * Custom WebViewClient for URL filtering and security.
 */
class SecureWebViewClient(
    private val config: ExamConfig,
    private val listener: SecureWebViewListener
) : WebViewClient() {
    
    companion object {
        private const val TAG = "SecureWebViewClient"
    }
    
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        Log.d(TAG, "Navigation request: $url")
        
        // Check if URL is allowed
        if (!config.isUrlAllowed(url)) {
            Log.w(TAG, "Blocked navigation to: $url")
            listener.onUrlBlocked(url)
            return true
        }
        
        // Block downloads
        if (url.endsWith(".pdf") || url.endsWith(".doc") || url.endsWith(".zip")) {
            if (!config.allowDownloads) {
                Log.w(TAG, "Blocked download: $url")
                listener.onUrlBlocked(url)
                return true
            }
        }
        
        return false
    }
    
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "Page started: $url")
        listener.onPageStarted(url)
    }
    
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished: $url")
        listener.onPageFinished(url)
        
        // Inject SEB script after page loads
        (view as? SecureWebView)?.injectSebScript()
        
        // Check for exam submission indicators
        checkForExamSubmission(view, url)
    }
    
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            Log.e(TAG, "Error loading page: ${error.description}")
            listener.onError(error.description?.toString() ?: "Unknown error")
        }
    }
    
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        Log.e(TAG, "SSL Error: ${error.primaryError}")
        // IMPORTANT: Do NOT proceed with SSL errors in production
        // This would bypass certificate validation
        handler.cancel()
        listener.onError("SSL Certificate Error: Connection is not secure")
    }
    
    private fun checkForExamSubmission(view: WebView, url: String) {
        // Check URL patterns that indicate exam submission
        val submissionPatterns = listOf(
            "review.php",
            "summary.php",
            "submitted",
            "finish"
        )
        
        if (submissionPatterns.any { url.contains(it, ignoreCase = true) }) {
            // Inject JavaScript to check for submission confirmation
            view.evaluateJavascript(
                "(function() { " +
                "var submitted = document.querySelector('.submitbtns') !== null || " +
                "document.body.innerHTML.includes('Your attempt has been submitted'); " +
                "return submitted; " +
                "})()"
            ) { result ->
                if (result == "true") {
                    listener.onExamSubmitted()
                }
            }
        }
    }
}
