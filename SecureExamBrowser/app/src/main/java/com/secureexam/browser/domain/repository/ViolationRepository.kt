package com.secureexam.browser.domain.repository

import com.secureexam.browser.domain.model.Violation
import com.secureexam.browser.domain.model.ViolationType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for violation logging and reporting.
 */
interface ViolationRepository {
    
    /**
     * Log a new violation.
     */
    suspend fun logViolation(
        sessionId: String,
        type: ViolationType,
        description: String,
        details: String? = null
    ): Violation
    
    /**
     * Get all violations for a session.
     */
    fun getViolationsForSession(sessionId: String): Flow<List<Violation>>
    
    /**
     * Get all unsynced violations.
     */
    suspend fun getUnsyncedViolations(): List<Violation>
    
    /**
     * Mark violation as synced to Moodle.
     */
    suspend fun markAsSynced(violationId: Long)
    
    /**
     * Sync violations to Moodle server.
     */
    suspend fun syncToMoodle(moodleUrl: String, token: String): Boolean
    
    /**
     * Get violation count for current session.
     */
    suspend fun getViolationCount(sessionId: String): Int
    
    /**
     * Clear all violations for a session.
     */
    suspend fun clearViolations(sessionId: String)
}
