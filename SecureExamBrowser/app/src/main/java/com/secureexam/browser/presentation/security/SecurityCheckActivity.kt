package com.secureexam.browser.presentation.security

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.secureexam.browser.R
import com.secureexam.browser.databinding.ActivitySecurityCheckBinding
import com.secureexam.browser.domain.model.SecurityCheckResult
import com.secureexam.browser.domain.model.SecurityCheckType
import com.secureexam.browser.domain.model.SecurityStatus
import com.secureexam.browser.domain.usecase.RunSecurityChecksUseCase
import com.secureexam.browser.presentation.config.ConfigActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity that runs security checks before allowing exam access.
 */
@AndroidEntryPoint
class SecurityCheckActivity : AppCompatActivity() {
    
    @Inject
    lateinit var runSecurityChecks: RunSecurityChecksUseCase
    
    private lateinit var binding: ActivitySecurityCheckBinding
    
    private val checkViews = mutableMapOf<SecurityCheckType, View>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply security flag
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        binding = ActivitySecurityCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        runChecks()
    }
    
    private fun setupUI() {
        binding.btnContinue.setOnClickListener {
            navigateToConfig()
        }
        
        binding.btnRetry.setOnClickListener {
            runChecks()
        }
        
        binding.btnSettings.setOnClickListener {
            // Open device settings
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        
        // Create check item views
        val checks = listOf(
            SecurityCheckType.ROOT_DETECTION to "Root Detection",
            SecurityCheckType.DEVELOPER_OPTIONS to "Developer Options",
            SecurityCheckType.USB_DEBUGGING to "USB Debugging",
            SecurityCheckType.VPN_DETECTION to "VPN Detection",
            SecurityCheckType.MOCK_LOCATION to "Mock Location",
            SecurityCheckType.SCREEN_MIRRORING to "Screen Mirroring",
            SecurityCheckType.EXTERNAL_DISPLAY to "External Display",
            SecurityCheckType.EMULATOR_DETECTION to "Emulator Detection"
        )
        
        binding.llChecks.removeAllViews()
        
        for ((type, name) in checks) {
            val view = createCheckItemView(name)
            checkViews[type] = view
            binding.llChecks.addView(view)
        }
    }
    
    private fun createCheckItemView(name: String): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_security_check, binding.llChecks, false)
        
        view.findViewById<TextView>(R.id.tvCheckName).text = name
        view.findViewById<ImageView>(R.id.ivStatus).visibility = View.INVISIBLE
        view.findViewById<ProgressBar>(R.id.progressCheck).visibility = View.VISIBLE
        
        return view
    }
    
    private fun runChecks() {
        // Reset UI
        binding.progressIndicator.visibility = View.VISIBLE
        binding.btnContinue.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
        binding.btnSettings.visibility = View.GONE
        binding.tvStatus.visibility = View.GONE
        binding.tvSubtitle.text = getString(R.string.security_checking)
        
        // Reset all check items to loading
        checkViews.values.forEach { view ->
            view.findViewById<ImageView>(R.id.ivStatus).visibility = View.INVISIBLE
            view.findViewById<ProgressBar>(R.id.progressCheck).visibility = View.VISIBLE
        }
        
        lifecycleScope.launch {
            // Run checks with slight delay between each for visual feedback
            val results = mutableListOf<SecurityCheckResult>()
            
            for (type in checkViews.keys) {
                delay(300) // Visual feedback
                val result = runSecurityChecks.runCheck(type)
                results.add(result)
                updateCheckItem(type, result)
            }
            
            // Final status
            val status = SecurityStatus.fromResults(results)
            showResults(status)
        }
    }
    
    private fun updateCheckItem(type: SecurityCheckType, result: SecurityCheckResult) {
        val view = checkViews[type] ?: return
        
        val ivStatus = view.findViewById<ImageView>(R.id.ivStatus)
        val progressCheck = view.findViewById<ProgressBar>(R.id.progressCheck)
        
        progressCheck.visibility = View.GONE
        ivStatus.visibility = View.VISIBLE
        
        if (result.passed) {
            ivStatus.setImageResource(R.drawable.ic_check_circle)
            ivStatus.setColorFilter(ContextCompat.getColor(this, R.color.success))
        } else {
            ivStatus.setImageResource(R.drawable.ic_error_circle)
            ivStatus.setColorFilter(
                ContextCompat.getColor(
                    this,
                    if (result.blocking) R.color.error else R.color.warning
                )
            )
        }
    }
    
    private fun showResults(status: SecurityStatus) {
        binding.progressIndicator.visibility = View.GONE
        binding.tvStatus.visibility = View.VISIBLE
        
        if (status.canProceed) {
            // All critical checks passed
            binding.tvSubtitle.text = getString(R.string.security_passed)
            binding.tvSubtitle.setTextColor(ContextCompat.getColor(this, R.color.success))
            binding.ivShield.setColorFilter(ContextCompat.getColor(this, R.color.success))
            
            if (status.allChecksPassed) {
                binding.tvStatus.text = "Your device meets all security requirements."
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            } else {
                val warnings = status.results.filter { !it.passed && !it.blocking }
                binding.tvStatus.text = "Warning: ${warnings.size} non-critical check(s) failed.\n" +
                        "You can proceed, but some features may be limited."
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))
            }
            
            binding.btnContinue.visibility = View.VISIBLE
        } else {
            // Critical checks failed
            binding.tvSubtitle.text = getString(R.string.security_failed)
            binding.tvSubtitle.setTextColor(ContextCompat.getColor(this, R.color.error))
            binding.ivShield.setColorFilter(ContextCompat.getColor(this, R.color.error))
            
            val failedChecks = status.failedBlockingChecks.joinToString("\n") { "• ${it.checkName}" }
            binding.tvStatus.text = "The following security issues must be resolved:\n\n$failedChecks"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            
            binding.btnRetry.visibility = View.VISIBLE
            binding.btnSettings.visibility = View.VISIBLE
        }
    }
    
    private fun navigateToConfig() {
        startActivity(Intent(this, ConfigActivity::class.java))
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // Re-run checks when returning from settings
    }
}
