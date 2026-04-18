package com.secureexam.browser.data.repository

import com.google.gson.Gson
import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.domain.model.ExamConfig
import com.secureexam.browser.domain.model.ExamSession
import com.secureexam.browser.domain.model.ExamSessionState
import com.secureexam.browser.domain.model.SebConfig
import com.secureexam.browser.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ExamRepository.
 */
@Singleton
class ExamRepositoryImpl @Inject constructor(
    private val securePreferences: SecurePreferences
) : ExamRepository {
    
    private val gson = Gson()
    private val _currentSession = MutableStateFlow<ExamSession?>(null)
    
    init {
        // Restore session if exists
        securePreferences.loadExamSession()?.let { session ->
            _currentSession.value = session
        }
    }
    
    override suspend fun saveConfig(config: ExamConfig) {
        securePreferences.saveExamConfig(config)
        securePreferences.saveExitPasswordHash(config.exitPasswordHash)
    }
    
    override suspend fun loadConfig(): ExamConfig? {
        return securePreferences.loadExamConfig()
    }
    
    override suspend fun clearConfig() {
        securePreferences.clearExamConfig()
    }
    
    override suspend fun parseSebConfig(content: String): SebConfig? {
        return try {
            // SEB configs can be in various formats:
            // 1. Plain JSON
            // 2. Base64 encoded
            // 3. Compressed and encrypted (requires SEB key)
            
            // Try plain JSON first
            try {
                return gson.fromJson(content, SebConfig::class.java)
            } catch (e: Exception) {
                // Not plain JSON
            }
            
            // Try Base64 decoded
            try {
                val decoded = android.util.Base64.decode(content, android.util.Base64.DEFAULT)
                val decodedString = String(decoded, Charsets.UTF_8)
                return gson.fromJson(decodedString, SebConfig::class.java)
            } catch (e: Exception) {
                // Not Base64
            }
            
            // Encrypted SEB files would need the SEB encryption key
            // For now, return null for encrypted configs
            null
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun validateExitPassword(password: String): Boolean {
        val storedHash = securePreferences.getExitPasswordHash() ?: return false
        val inputHash = hashPassword(password)
        return storedHash == inputHash
    }
    
    override fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.fold("") { str, byte -> str + "%02x".format(byte) }
    }
    
    override fun getCurrentSession(): Flow<ExamSession?> {
        return _currentSession.asStateFlow()
    }
    
    override suspend fun startSession(config: ExamConfig): ExamSession {
        val session = ExamSession(
            id = UUID.randomUUID().toString(),
            config = config,
            state = ExamSessionState.ACTIVE,
            startTime = System.currentTimeMillis()
        )
        _currentSession.value = session
        securePreferences.saveExamSession(session)
        return session
    }
    
    override suspend fun endSession() {
        _currentSession.value?.let { session ->
            val endedSession = session.end()
            _currentSession.value = endedSession
            securePreferences.saveExamSession(endedSession)
        }
        // Clear after a delay or keep for reporting
        _currentSession.value = null
        securePreferences.clearExamSession()
    }
    
    override suspend fun updateSessionState(session: ExamSession) {
        _currentSession.value = session
        securePreferences.saveExamSession(session)
    }
}
