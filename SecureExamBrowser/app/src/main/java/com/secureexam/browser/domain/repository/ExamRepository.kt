package com.secureexam.browser.domain.repository

import com.secureexam.browser.domain.model.ExamConfig
import com.secureexam.browser.domain.model.ExamSession
import com.secureexam.browser.domain.model.SebConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for exam-related operations.
 */
interface ExamRepository {
    
    /**
     * Save exam configuration to encrypted storage.
     */
    suspend fun saveConfig(config: ExamConfig)
    
    /**
     * Load saved exam configuration.
     */
    suspend fun loadConfig(): ExamConfig?
    
    /**
     * Clear saved configuration.
     */
    suspend fun clearConfig()
    
    /**
     * Parse SEB configuration file.
     */
    suspend fun parseSebConfig(content: String): SebConfig?
    
    /**
     * Validate exit password.
     */
    suspend fun validateExitPassword(password: String): Boolean
    
    /**
     * Hash a password for storage.
     */
    fun hashPassword(password: String): String
    
    /**
     * Get current exam session.
     */
    fun getCurrentSession(): Flow<ExamSession?>
    
    /**
     * Start a new exam session.
     */
    suspend fun startSession(config: ExamConfig): ExamSession
    
    /**
     * End the current exam session.
     */
    suspend fun endSession()
    
    /**
     * Update session state.
     */
    suspend fun updateSessionState(session: ExamSession)
}
