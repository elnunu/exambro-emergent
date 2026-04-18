package com.secureexam.browser.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ExamSession.
 */
class ExamSessionTest {
    
    @Test
    fun `isActive returns true when state is ACTIVE`() {
        val session = ExamSession(
            id = "test",
            config = ExamConfig.empty(),
            state = ExamSessionState.ACTIVE
        )
        
        assertTrue(session.isActive)
    }
    
    @Test
    fun `isActive returns false when state is not ACTIVE`() {
        val session = ExamSession(
            id = "test",
            config = ExamConfig.empty(),
            state = ExamSessionState.IDLE
        )
        
        assertFalse(session.isActive)
    }
    
    @Test
    fun `start sets state and startTime`() {
        val session = ExamSession(
            id = "test",
            config = ExamConfig.empty(),
            state = ExamSessionState.IDLE
        )
        
        val started = session.start()
        
        assertEquals(ExamSessionState.ACTIVE, started.state)
        assertNotNull(started.startTime)
    }
    
    @Test
    fun `end sets state and endTime`() {
        val session = ExamSession(
            id = "test",
            config = ExamConfig.empty(),
            state = ExamSessionState.ACTIVE,
            startTime = System.currentTimeMillis()
        )
        
        val ended = session.end()
        
        assertEquals(ExamSessionState.ENDED, ended.state)
        assertNotNull(ended.endTime)
    }
    
    @Test
    fun `addViolation appends to list`() {
        val session = ExamSession(
            id = "test",
            config = ExamConfig.empty()
        )
        
        val violation = Violation(
            sessionId = "test",
            type = ViolationType.SCREENSHOT_ATTEMPT,
            description = "Test"
        )
        
        val updated = session.addViolation(violation)
        
        assertEquals(1, updated.violations.size)
        assertEquals(violation, updated.violations.first())
    }
    
    @Test
    fun `duration calculates correctly`() {
        val startTime = System.currentTimeMillis() - 10000 // 10 seconds ago
        val endTime = System.currentTimeMillis()
        
        val session = ExamSession(
            id = "test",
            config = ExamConfig.empty(),
            startTime = startTime,
            endTime = endTime
        )
        
        assertTrue(session.duration >= 10000)
        assertTrue(session.duration < 11000)
    }
}
