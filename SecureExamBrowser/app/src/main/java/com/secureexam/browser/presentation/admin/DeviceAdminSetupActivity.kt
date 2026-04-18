package com.secureexam.browser.presentation.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.databinding.ActivityDeviceAdminSetupBinding
import com.secureexam.browser.presentation.security.SecurityCheckActivity
import com.secureexam.browser.security.KioskManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity for setting up device admin privileges.
 */
@AndroidEntryPoint
class DeviceAdminSetupActivity : AppCompatActivity() {
    
    @Inject
    lateinit var kioskManager: KioskManager
    
    @Inject
    lateinit var securePreferences: SecurePreferences
    
    private lateinit var binding: ActivityDeviceAdminSetupBinding
    
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || kioskManager.isDeviceAdminActive()) {
            securePreferences.setDeviceAdminSetupComplete(true)
            navigateToSecurityCheck()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceAdminSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
        
        // Check if already admin
        if (kioskManager.isDeviceAdminActive()) {
            navigateToSecurityCheck()
        }
    }
    
    private fun setupListeners() {
        binding.btnEnableAdmin.setOnClickListener {
            requestDeviceAdmin()
        }
        
        binding.btnSkip.setOnClickListener {
            // Skip device admin setup - limited functionality
            securePreferences.setDeviceAdminSetupComplete(true)
            navigateToSecurityCheck()
        }
    }
    
    private fun requestDeviceAdmin() {
        val intent = kioskManager.run {
            android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    android.content.ComponentName(this@DeviceAdminSetupActivity, com.secureexam.browser.receiver.DeviceAdminReceiver::class.java)
                )
                putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Secure Exam Browser requires device administrator privileges to enable kiosk mode during exams. This ensures exam integrity by preventing access to other apps."
                )
            }
        }
        deviceAdminLauncher.launch(intent)
    }
    
    private fun navigateToSecurityCheck() {
        startActivity(Intent(this, SecurityCheckActivity::class.java))
        finish()
    }
}
