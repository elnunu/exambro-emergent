package com.secureexam.browser.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Violation.
 */
class ViolationTest {
    
    @Test
    fun `severity is set correctly for screenshot attempt`() {
        val violation = Violation(
            sessionId = "session1",
            type = ViolationType.SCREENSHOT_ATTEMPT,
            description = "Test"
        )
        
        assertEquals(4, violation.severity)
    }
    
    @Test
    fun `severity is set correctly for screen mirroring`() {
        val violation = Violation(
            sessionId = "session1",
            type = ViolationType.SCREEN_MIRROR_DETECTED,
            description = "Test"
        )
        
        assertEquals(5, violation.severity) // Critical
    }
    
    @Test
    fun `severity is set correctly for volume button`() {
        val violation = Violation(
            sessionId = "session1",
            type = ViolationType.VOLUME_BUTTON_PRESS,
            description = "Test"
        )
        
        assertEquals(1, violation.severity) // Minor
    }
    
    @Test
    fun `timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val violation = Violation(
            sessionId = "session1",
            type = ViolationType.UNKNOWN,
            description = "Test"
        )
        val after = System.currentTimeMillis()
        
        assertTrue(violation.timestamp >= before)
        assertTrue(violation.timestamp <= after)
    }
}
