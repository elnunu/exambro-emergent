package com.secureexam.browser.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.secureexam.browser.domain.model.ExamConfig
import com.secureexam.browser.domain.model.ExamSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure encrypted storage for sensitive exam data.
 * Uses Android Keystore for encryption.
 */
@Singleton
class SecurePreferences @Inject constructor(
    private val context: Context
) {
    private val gson = Gson()
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save exam configuration securely.
     */
    fun saveExamConfig(config: ExamConfig) {
        val json = gson.toJson(config)
        securePrefs.edit().putString(KEY_EXAM_CONFIG, json).apply()
    }
    
    /**
     * Load exam configuration.
     */
    fun loadExamConfig(): ExamConfig? {
        val json = securePrefs.getString(KEY_EXAM_CONFIG, null)
        return json?.let { gson.fromJson(it, ExamConfig::class.java) }
    }
    
    /**
     * Clear exam configuration.
     */
    fun clearExamConfig() {
        securePrefs.edit().remove(KEY_EXAM_CONFIG).apply()
    }
    
    /**
     * Save current exam session.
     */
    fun saveExamSession(session: ExamSession) {
        val json = gson.toJson(session)
        securePrefs.edit().putString(KEY_EXAM_SESSION, json).apply()
    }
    
    /**
     * Load current exam session.
     */
    fun loadExamSession(): ExamSession? {
        val json = securePrefs.getString(KEY_EXAM_SESSION, null)
        return json?.let { gson.fromJson(it, ExamSession::class.java) }
    }
    
    /**
     * Clear exam session.
     */
    fun clearExamSession() {
        securePrefs.edit().remove(KEY_EXAM_SESSION).apply()
    }
    
    /**
     * Save exit password hash.
     */
    fun saveExitPasswordHash(hash: String) {
        securePrefs.edit().putString(KEY_EXIT_PASSWORD, hash).apply()
    }
    
    /**
     * Get exit password hash.
     */
    fun getExitPasswordHash(): String? {
        return securePrefs.getString(KEY_EXIT_PASSWORD, null)
    }
    
    /**
     * Check if device admin setup is complete.
     */
    fun isDeviceAdminSetupComplete(): Boolean {
        return securePrefs.getBoolean(KEY_ADMIN_SETUP_COMPLETE, false)
    }
    
    /**
     * Mark device admin setup as complete.
     */
    fun setDeviceAdminSetupComplete(complete: Boolean) {
        securePrefs.edit().putBoolean(KEY_ADMIN_SETUP_COMPLETE, complete).apply()
    }
    
    /**
     * Check if this is the first launch.
     */
    fun isFirstLaunch(): Boolean {
        val isFirst = securePrefs.getBoolean(KEY_FIRST_LAUNCH, true)
        if (isFirst) {
            securePrefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }
        return isFirst
    }
    
    /**
     * Clear all secure preferences.
     */
    fun clearAll() {
        securePrefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "secure_exam_prefs"
        private const val KEY_EXAM_CONFIG = "exam_config"
        private const val KEY_EXAM_SESSION = "exam_session"
        private const val KEY_EXIT_PASSWORD = "exit_password_hash"
        private const val KEY_ADMIN_SETUP_COMPLETE = "admin_setup_complete"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}
