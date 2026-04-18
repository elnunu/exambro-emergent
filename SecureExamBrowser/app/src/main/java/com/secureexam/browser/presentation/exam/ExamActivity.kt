package com.secureexam.browser.presentation.exam

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.secureexam.browser.R
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.data.local.ViolationDao
import com.secureexam.browser.databinding.ActivityExamBinding
import com.secureexam.browser.domain.model.ExamConfig
import com.secureexam.browser.domain.model.ViolationType
import com.secureexam.browser.domain.repository.ExamRepository
import com.secureexam.browser.domain.repository.ViolationRepository
import com.secureexam.browser.presentation.config.ConfigActivity
import com.secureexam.browser.receiver.ScreenStateReceiver
import com.secureexam.browser.security.KioskLockService
import com.secureexam.browser.security.KioskManager
import com.secureexam.browser.security.SecurityChecker
import com.secureexam.browser.security.ViolationLogger
import com.secureexam.browser.webview.SecureWebViewListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main exam activity with kiosk mode and secure WebView.
 * This is the locked-down exam interface.
 */
@AndroidEntryPoint
class ExamActivity : AppCompatActivity(), SecureWebViewListener, ScreenStateReceiver.ScreenStateListener {
    
    @Inject
    lateinit var kioskManager: KioskManager
    
    @Inject
    lateinit var securityChecker: SecurityChecker
    
    @Inject
    lateinit var violationLogger: ViolationLogger
    
    @Inject
    lateinit var examRepository: ExamRepository
    
    @Inject
    lateinit var violationRepository: ViolationRepository
    
    @Inject
    lateinit var securePreferences: SecurePreferences
    
    @Inject
    lateinit var violationDao: ViolationDao
    
    private lateinit var binding: ActivityExamBinding
    
    private var examConfig: ExamConfig? = null
    private var sessionId: String? = null
    private var exitDialog: AlertDialog? = null
    private val screenStateReceiver = ScreenStateReceiver()
    
    private var failedPasswordAttempts = 0
    private val MAX_PASSWORD_ATTEMPTS = 5
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize violation logger
        initializeSession()
        
        // Apply security flags FIRST
        applySecurityFlags()
        
        // Start kiosk mode
        kioskManager.startKioskMode(this)
        
        // Start kiosk lock service
        startKioskService()
        
        // Setup UI
        setupUI()
        
        // Load exam
        loadExam()
        
        // Register screen state receiver
        registerScreenStateReceiver()
    }
    
    private fun initializeSession() {
        lifecycleScope.launch {
            examConfig = examRepository.loadConfig()
            examConfig?.let { config ->
                val session = examRepository.startSession(config)
                sessionId = session.id
                violationLogger.initialize(violationDao, session.id)
            }
        }
    }
    
    private fun applySecurityFlags() {
        window.apply {
            // Prevent screenshots and screen recording
            addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            
            // Keep screen on
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Fullscreen
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            
            // Show when locked
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
            }
        }
    }
    
    private fun startKioskService() {
        val serviceIntent = Intent(this, KioskLockService::class.java).apply {
            action = KioskLockService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    private fun stopKioskService() {
        val serviceIntent = Intent(this, KioskLockService::class.java).apply {
            action = KioskLockService.ACTION_STOP
        }
        startService(serviceIntent)
    }
    
    private fun setupUI() {
        binding.btnExit.setOnClickListener {
            showExitDialog()
        }
        
        binding.btnRetry.setOnClickListener {
            loadExam()
        }
    }
    
    private fun loadExam() {
        // Show loading
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.errorOverlay.visibility = View.GONE
        
        examConfig?.let { config ->
            // Initialize secure WebView
            binding.webView.initialize(config, this)
            binding.webView.loadExamUrl()
        } ?: run {
            // No config - show error
            showError("No exam configuration found")
        }
    }
    
    private fun showExitDialog() {
        if (exitDialog?.isShowing == true) return
        
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_exit_exam, null)
        
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.tilPassword)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val tvError = dialogView.findViewById<TextView>(R.id.tvError)
        
        exitDialog = AlertDialog.Builder(this, R.style.Theme_SecureExamBrowser_Dialog)
            .setTitle(R.string.exam_exit_title)
            .setView(dialogView)
            .setPositiveButton(R.string.exam_exit_confirm) { _, _ ->
                // Handled manually
            }
            .setNegativeButton(R.string.exam_exit_cancel) { dialog, _ ->
                dialog.dismiss()
                violationLogger.logKioskExitAttempt()
            }
            .setCancelable(false)
            .create()
        
        exitDialog?.show()
        
        // Override positive button to validate password
        exitDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val password = etPassword.text?.toString() ?: ""
            
            lifecycleScope.launch {
                if (examRepository.validateExitPassword(password)) {
                    exitDialog?.dismiss()
                    exitExam()
                } else {
                    failedPasswordAttempts++
                    tvError.visibility = View.VISIBLE
                    tvError.text = "Incorrect password ($failedPasswordAttempts/$MAX_PASSWORD_ATTEMPTS)"
                    violationLogger.logKioskExitAttempt()
                    
                    if (failedPasswordAttempts >= MAX_PASSWORD_ATTEMPTS) {
                        exitDialog?.dismiss()
                        Toast.makeText(
                            this@ExamActivity,
                            "Too many failed attempts. Contact your invigilator.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun exitExam() {
        lifecycleScope.launch {
            // End session
            examRepository.endSession()
            violationLogger.clearSession()
            
            // Stop kiosk mode
            kioskManager.stopKioskMode(this@ExamActivity)
            
            // Stop kiosk service
            stopKioskService()
            
            // Clear WebView data
            binding.webView.clearBrowsingData()
            
            // Navigate to config
            startActivity(Intent(this@ExamActivity, ConfigActivity::class.java))
            finish()
        }
    }
    
    private fun showError(message: String) {
        binding.loadingOverlay.visibility = View.GONE
        binding.errorOverlay.visibility = View.VISIBLE
        binding.tvError.text = message
    }
    
    private fun registerScreenStateReceiver() {
        screenStateReceiver.listener = this
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }
    
    // SecureWebViewListener implementation
    
    override fun onPageStarted(url: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
    }
    
    override fun onPageFinished(url: String) {
        binding.loadingOverlay.visibility = View.GONE
        binding.errorOverlay.visibility = View.GONE
    }
    
    override fun onProgressChanged(progress: Int) {
        // Could update a progress bar
    }
    
    override fun onUrlBlocked(url: String) {
        violationLogger.logExternalUrlBlocked(url)
        Toast.makeText(this, getString(R.string.error_domain_blocked), Toast.LENGTH_SHORT).show()
    }
    
    override fun onError(message: String) {
        showError(message)
    }
    
    override fun onExamSubmitted() {
        // Exam was submitted - auto-exit kiosk mode
        lifecycleScope.launch {
            Toast.makeText(
                this@ExamActivity,
                getString(R.string.exam_submitted),
                Toast.LENGTH_LONG
            ).show()
            
            // Wait a moment then exit
            kotlinx.coroutines.delay(2000)
            exitExam()
        }
    }
    
    override fun onExamStarted() {
        // Exam started - ensure kiosk mode is active
        if (!kioskManager.isInLockTaskMode()) {
            kioskManager.startKioskMode(this)
        }
    }
    
    // ScreenStateListener implementation
    
    override fun onScreenOn() {
        // Ensure kiosk mode when screen turns on
        if (!kioskManager.isInLockTaskMode()) {
            kioskManager.startKioskMode(this)
        }
    }
    
    override fun onScreenOff() {
        // Log screen off during exam
        violationLogger.logViolation(
            ViolationType.POWER_BUTTON_PRESS,
            "Screen was turned off during exam"
        )
    }
    
    override fun onUserPresent() {
        // Device unlocked - ensure we're back in kiosk mode
        if (!kioskManager.isInLockTaskMode()) {
            kioskManager.startKioskMode(this)
        }
    }
    
    // Override back button
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Check if WebView can go back
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            // Don't allow back navigation
            violationLogger.logViolation(
                ViolationType.NAVIGATION_BLOCKED,
                "Back button pressed but blocked"
            )
            Toast.makeText(this, getString(R.string.kiosk_exit_blocked), Toast.LENGTH_SHORT).show()
        }
    }
    
    // Override key events
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // Block these keys
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_SEARCH -> {
                violationLogger.logAppSwitchAttempt()
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                violationLogger.logViolation(
                    ViolationType.VOLUME_BUTTON_PRESS,
                    "Volume button pressed"
                )
                true // Block volume buttons
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    // Prevent window focus loss
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            // Window lost focus - try to regain it
            violationLogger.logAppSwitchAttempt()
            
            // Bring activity back to front
            val intent = Intent(this, ExamActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        } else {
            // Ensure immersive mode
            kioskManager.startKioskMode(this)
        }
    }
    
    // Prevent activity from being destroyed
    override fun onPause() {
        super.onPause()
        // Activity paused - log potential violation
        if (!isFinishing) {
            violationLogger.logAppSwitchAttempt()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure kiosk mode is active
        kioskManager.startKioskMode(this)
        
        // Run security checks
        lifecycleScope.launch {
            val summary = securityChecker.runAllChecks()
            
            // Log any new violations
            if (summary.screenMirroringActive) {
                violationLogger.logScreenMirrorDetected()
            }
            if (summary.externalDisplayConnected) {
                violationLogger.logExternalDisplayDetected()
            }
            if (summary.vpnActive) {
                violationLogger.logVpnDetected()
            }
            if (summary.usbDebuggingEnabled) {
                violationLogger.logUsbDebugDetected()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister receiver
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Not registered
        }
        
        // Dismiss dialog if showing
        exitDialog?.dismiss()
    }
    
    // Block multi-window mode
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        if (isInMultiWindowMode) {
            violationLogger.logSplitScreenAttempt()
            // Exit multi-window mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // The system handles this, but log the violation
            }
        }
    }
    
    // Block picture-in-picture
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            violationLogger.logPipAttempt()
        }
    }
}
