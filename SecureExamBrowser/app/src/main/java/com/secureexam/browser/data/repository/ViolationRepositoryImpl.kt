package com.secureexam.browser.data.repository

import com.secureexam.browser.data.local.ViolationDao
import com.secureexam.browser.data.remote.MoodleApiService
import com.secureexam.browser.data.remote.dto.ViolationDto
import com.secureexam.browser.data.remote.dto.ViolationReportRequest
import com.secureexam.browser.domain.model.Violation
import com.secureexam.browser.domain.model.ViolationType
import com.secureexam.browser.domain.repository.ViolationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ViolationRepository.
 */
@Singleton
class ViolationRepositoryImpl @Inject constructor(
    private val violationDao: ViolationDao,
    private val moodleApiService: MoodleApiService
) : ViolationRepository {
    
    override suspend fun logViolation(
        sessionId: String,
        type: ViolationType,
        description: String,
        details: String?
    ): Violation {
        val violation = Violation(
            sessionId = sessionId,
            type = type,
            description = description,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        val id = violationDao.insert(violation)
        return violation.copy(id = id)
    }
    
    override fun getViolationsForSession(sessionId: String): Flow<List<Violation>> {
        return violationDao.getViolationsForSession(sessionId)
    }
    
    override suspend fun getUnsyncedViolations(): List<Violation> {
        return violationDao.getUnsyncedViolations()
    }
    
    override suspend fun markAsSynced(violationId: Long) {
        violationDao.markAsSynced(violationId)
    }
    
    override suspend fun syncToMoodle(moodleUrl: String, token: String): Boolean {
        return try {
            val unsyncedViolations = getUnsyncedViolations()
            if (unsyncedViolations.isEmpty()) return true
            
            // Group by session
            val bySession = unsyncedViolations.groupBy { it.sessionId }
            
            bySession.forEach { (sessionId, violations) ->
                val request = ViolationReportRequest(
                    quizId = 0, // Would be set from actual quiz context
                    cmId = 0,
                    sessionId = sessionId,
                    violations = violations.map { v ->
                        ViolationDto(
                            type = v.type.name,
                            timestamp = v.timestamp,
                            description = v.description,
                            details = v.details,
                            severity = v.severity
                        )
                    }
                )
                
                val response = moodleApiService.reportViolation(
                    token = token,
                    request = request
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    violations.forEach { markAsSynced(it.id) }
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getViolationCount(sessionId: String): Int {
        return violationDao.getViolationCount(sessionId)
    }
    
    override suspend fun clearViolations(sessionId: String) {
        violationDao.clearViolations(sessionId)
    }
}
