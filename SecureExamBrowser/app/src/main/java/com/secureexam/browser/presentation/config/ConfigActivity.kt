package com.secureexam.browser.presentation.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.secureexam.browser.R
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.databinding.ActivityConfigBinding
import com.secureexam.browser.domain.model.ExamConfig
import com.secureexam.browser.domain.repository.ExamRepository
import com.secureexam.browser.presentation.exam.ExamActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for configuring exam settings.
 * Teacher mode: Set Moodle URL and exit password.
 * Student mode: Enter exam token and start exam.
 */
@AndroidEntryPoint
class ConfigActivity : AppCompatActivity() {
    
    @Inject
    lateinit var examRepository: ExamRepository
    
    @Inject
    lateinit var securePreferences: SecurePreferences
    
    private lateinit var binding: ActivityConfigBinding
    
    private var isTeacherMode = true
    
    private val sebFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadSebConfig(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply security flag
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadSavedConfig()
    }
    
    private fun setupUI() {
        // Mode toggle
        binding.toggleMode.check(R.id.btnTeacherMode)
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isTeacherMode = checkedId == R.id.btnTeacherMode
                updateModeUI()
            }
        }
        
        // Button listeners
        binding.btnSaveConfig.setOnClickListener {
            saveConfig()
        }
        
        binding.btnStartExam.setOnClickListener {
            startExam()
        }
        
        binding.btnImportSeb.setOnClickListener {
            importSebConfig()
        }
        
        binding.btnClearConfig.setOnClickListener {
            clearConfig()
        }
        
        updateModeUI()
    }
    
    private fun updateModeUI() {
        if (isTeacherMode) {
            // Teacher mode - show password fields, hide token
            binding.tilExitPassword.visibility = View.VISIBLE
            binding.tilConfirmPassword.visibility = View.VISIBLE
            binding.tilExamToken.visibility = View.GONE
            binding.btnSaveConfig.visibility = View.VISIBLE
            binding.btnImportSeb.visibility = View.VISIBLE
            binding.btnClearConfig.visibility = View.VISIBLE
        } else {
            // Student mode - show token, hide password fields
            binding.tilExitPassword.visibility = View.GONE
            binding.tilConfirmPassword.visibility = View.GONE
            binding.tilExamToken.visibility = View.VISIBLE
            binding.btnSaveConfig.visibility = View.GONE
            binding.btnImportSeb.visibility = View.GONE
            binding.btnClearConfig.visibility = View.GONE
        }
    }
    
    private fun loadSavedConfig() {
        lifecycleScope.launch {
            val config = examRepository.loadConfig()
            config?.let {
                binding.etMoodleUrl.setText(it.moodleUrl)
                // Don't show password, it's hashed
            }
        }
    }
    
    private fun saveConfig() {
        val moodleUrl = binding.etMoodleUrl.text?.toString()?.trim() ?: ""
        val exitPassword = binding.etExitPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""
        
        // Validation
        if (moodleUrl.isEmpty()) {
            binding.tilMoodleUrl.error = getString(R.string.error_url_required)
            return
        }
        
        if (!moodleUrl.startsWith("http://") && !moodleUrl.startsWith("https://")) {
            binding.tilMoodleUrl.error = getString(R.string.error_invalid_url)
            return
        }
        
        if (exitPassword.isEmpty()) {
            binding.tilExitPassword.error = getString(R.string.error_password_required)
            return
        }
        
        if (exitPassword.length < 6) {
            binding.tilExitPassword.error = getString(R.string.error_password_weak)
            return
        }
        
        if (exitPassword != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            return
        }
        
        // Clear errors
        binding.tilMoodleUrl.error = null
        binding.tilExitPassword.error = null
        binding.tilConfirmPassword.error = null
        
        // Save config
        lifecycleScope.launch {
            val config = ExamConfig(
                moodleUrl = moodleUrl,
                exitPasswordHash = examRepository.hashPassword(exitPassword)
            )
            examRepository.saveConfig(config)
            
            Toast.makeText(this@ConfigActivity, "Configuration saved", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun startExam() {
        lifecycleScope.launch {
            val config = examRepository.loadConfig()
            
            if (config == null || config.moodleUrl.isEmpty()) {
                Toast.makeText(
                    this@ConfigActivity,
                    "Please configure the Moodle URL first",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            
            // In student mode, check for token
            if (!isTeacherMode) {
                val token = binding.etExamToken.text?.toString()?.trim() ?: ""
                if (token.isEmpty()) {
                    binding.tilExamToken.error = "Please enter the exam token"
                    return@launch
                }
                // Update config with token
                val updatedConfig = config.copy(examToken = token)
                examRepository.saveConfig(updatedConfig)
            }
            
            // Show confirmation dialog
            showStartExamConfirmation()
        }
    }
    
    private fun showStartExamConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Start Exam")
            .setMessage(
                "You are about to enter exam mode.\n\n" +
                "• The device will be locked to this app\n" +
                "• All violations will be logged\n" +
                "• Only the invigilator can exit\n\n" +
                "Are you ready to begin?"
            )
            .setPositiveButton("Start Exam") { _, _ ->
                launchExamActivity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun launchExamActivity() {
        val intent = Intent(this, ExamActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun importSebConfig() {
        sebFilePickerLauncher.launch("*/*")
    }
    
    private fun loadSebConfig(uri: Uri) {
        lifecycleScope.launch {
            try {
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (content != null) {
                    val sebConfig = examRepository.parseSebConfig(content)
                    if (sebConfig != null) {
                        binding.etMoodleUrl.setText(sebConfig.startUrl ?: sebConfig.sebServerUrl)
                        Toast.makeText(
                            this@ConfigActivity,
                            "SEB config imported successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ConfigActivity,
                            "Failed to parse SEB config file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ConfigActivity,
                    "Error loading SEB config: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun clearConfig() {
        AlertDialog.Builder(this)
            .setTitle("Clear Configuration")
            .setMessage("This will delete all saved settings. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch {
                    examRepository.clearConfig()
                    binding.etMoodleUrl.setText("")
                    binding.etExitPassword.setText("")
                    binding.etConfirmPassword.setText("")
                    Toast.makeText(
                        this@ConfigActivity,
                        "Configuration cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
