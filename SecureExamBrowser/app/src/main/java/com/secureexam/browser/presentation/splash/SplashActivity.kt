package com.secureexam.browser.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.domain.model.ExamSessionState
import com.secureexam.browser.presentation.admin.DeviceAdminSetupActivity
import com.secureexam.browser.presentation.exam.ExamActivity
import com.secureexam.browser.presentation.security.SecurityCheckActivity
import com.secureexam.browser.security.KioskManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash activity that handles initial routing.
 * Checks for active sessions, device admin status, etc.
 */
@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    @Inject
    lateinit var securePreferences: SecurePreferences
    
    @Inject
    lateinit var kioskManager: KioskManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while determining route
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        
        lifecycleScope.launch {
            // Brief delay for splash visibility
            delay(500)
            
            // Determine where to navigate
            navigateToNextScreen()
            
            keepSplash = false
            finish()
        }
    }
    
    private fun navigateToNextScreen() {
        // Check for active exam session first
        val session = securePreferences.loadExamSession()
        if (session != null && session.state == ExamSessionState.ACTIVE) {
            // Resume active exam
            startActivity(Intent(this, ExamActivity::class.java))
            return
        }
        
        // Check if device admin setup is needed
        if (securePreferences.isFirstLaunch() && !kioskManager.isDeviceAdminActive()) {
            startActivity(Intent(this, DeviceAdminSetupActivity::class.java))
            return
        }
        
        // Go to security check
        startActivity(Intent(this, SecurityCheckActivity::class.java))
    }
}
