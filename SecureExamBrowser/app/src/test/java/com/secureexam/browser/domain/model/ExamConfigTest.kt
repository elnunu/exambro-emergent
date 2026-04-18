package com.secureexam.browser.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ExamConfig.
 */
class ExamConfigTest {
    
    @Test
    fun `getBaseDomain extracts domain correctly`() {
        val config = ExamConfig(
            moodleUrl = "https://moodle.example.edu/mod/quiz/",
            exitPasswordHash = "hash"
        )
        
        assertEquals("moodle.example.edu", config.getBaseDomain())
    }
    
    @Test
    fun `isUrlAllowed returns true for same domain`() {
        val config = ExamConfig(
            moodleUrl = "https://moodle.example.edu/",
            exitPasswordHash = "hash"
        )
        
        assertTrue(config.isUrlAllowed("https://moodle.example.edu/mod/quiz/attempt.php"))
    }
    
    @Test
    fun `isUrlAllowed returns true for subdomain`() {
        val config = ExamConfig(
            moodleUrl = "https://moodle.example.edu/",
            exitPasswordHash = "hash"
        )
        
        assertTrue(config.isUrlAllowed("https://cdn.moodle.example.edu/image.jpg"))
    }
    
    @Test
    fun `isUrlAllowed returns false for different domain`() {
        val config = ExamConfig(
            moodleUrl = "https://moodle.example.edu/",
            exitPasswordHash = "hash"
        )
        
        assertFalse(config.isUrlAllowed("https://google.com/"))
    }
    
    @Test
    fun `isUrlAllowed returns true for whitelisted URLs`() {
        val config = ExamConfig(
            moodleUrl = "https://moodle.example.edu/",
            exitPasswordHash = "hash",
            allowedUrls = listOf("cdn.example.com")
        )
        
        assertTrue(config.isUrlAllowed("https://cdn.example.com/resource.js"))
    }
    
    @Test
    fun `empty factory creates config with empty strings`() {
        val config = ExamConfig.empty()
        
        assertEquals("", config.moodleUrl)
        assertEquals("", config.exitPasswordHash)
    }
}
