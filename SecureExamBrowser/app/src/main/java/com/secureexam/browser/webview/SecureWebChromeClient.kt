package com.secureexam.browser.webview

import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.util.Log

/**
 * Custom WebChromeClient for handling JavaScript dialogs and progress.
 */
class SecureWebChromeClient(
    private val listener: SecureWebViewListener
) : WebChromeClient() {
    
    companion object {
        private const val TAG = "SecureWebChromeClient"
    }
    
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        listener.onProgressChanged(newProgress)
    }
    
    override fun onJsAlert(
        view: WebView,
        url: String,
        message: String,
        result: JsResult
    ): Boolean {
        Log.d(TAG, "JS Alert: $message")
        // Let the default handler show the alert
        return false
    }
    
    override fun onJsConfirm(
        view: WebView,
        url: String,
        message: String,
        result: JsResult
    ): Boolean {
        Log.d(TAG, "JS Confirm: $message")
        // Let the default handler show the confirm
        return false
    }
    
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d(TAG, "Console: ${consoleMessage.message()} [${consoleMessage.lineNumber()}]")
        return true
    }
}
