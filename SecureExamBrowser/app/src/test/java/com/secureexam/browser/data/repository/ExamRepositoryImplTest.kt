package com.secureexam.browser.data.repository

import com.secureexam.browser.data.local.SecurePreferences
import com.secureexam.browser.domain.model.ExamConfig
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.*

/**
 * Unit tests for ExamRepositoryImpl.
 */
@RunWith(MockitoJUnitRunner::class)
class ExamRepositoryImplTest {
    
    @Mock
    private lateinit var securePreferences: SecurePreferences
    
    private lateinit var repository: ExamRepositoryImpl
    
    @Before
    fun setup() {
        repository = ExamRepositoryImpl(securePreferences)
    }
    
    @Test
    fun `saveConfig stores config in preferences`() = runBlocking {
        val config = ExamConfig(
            moodleUrl = "https://moodle.test.edu",
            exitPasswordHash = "hash"
        )
        
        repository.saveConfig(config)
        
        verify(securePreferences).saveExamConfig(config)
        verify(securePreferences).saveExitPasswordHash("hash")
    }
    
    @Test
    fun `loadConfig retrieves from preferences`() = runBlocking {
        val config = ExamConfig(
            moodleUrl = "https://moodle.test.edu",
            exitPasswordHash = "hash"
        )
        whenever(securePreferences.loadExamConfig()).thenReturn(config)
        
        val result = repository.loadConfig()
        
        assertEquals(config, result)
    }
    
    @Test
    fun `hashPassword produces consistent hash`() {
        val password = "testPassword123"
        
        val hash1 = repository.hashPassword(password)
        val hash2 = repository.hashPassword(password)
        
        assertEquals(hash1, hash2)
    }
    
    @Test
    fun `hashPassword produces different hash for different passwords`() {
        val hash1 = repository.hashPassword("password1")
        val hash2 = repository.hashPassword("password2")
        
        assertNotEquals(hash1, hash2)
    }
    
    @Test
    fun `validateExitPassword returns true for correct password`() = runBlocking {
        val password = "correctPassword"
        val hash = repository.hashPassword(password)
        whenever(securePreferences.getExitPasswordHash()).thenReturn(hash)
        
        assertTrue(repository.validateExitPassword(password))
    }
    
    @Test
    fun `validateExitPassword returns false for wrong password`() = runBlocking {
        val hash = repository.hashPassword("correctPassword")
        whenever(securePreferences.getExitPasswordHash()).thenReturn(hash)
        
        assertFalse(repository.validateExitPassword("wrongPassword"))
    }
}
