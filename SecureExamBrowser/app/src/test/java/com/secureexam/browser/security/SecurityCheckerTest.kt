package com.secureexam.browser.security

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.junit.Assert.*

/**
 * Unit tests for SecurityChecker.
 */
@RunWith(MockitoJUnitRunner::class)
class SecurityCheckerTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var rootDetector: RootDetector
    
    @Mock
    private lateinit var screenMirrorDetector: ScreenMirrorDetector
    
    private lateinit var securityChecker: SecurityChecker
    
    @Before
    fun setup() {
        securityChecker = SecurityChecker(context, rootDetector, screenMirrorDetector)
    }
    
    @Test
    fun `isDeviceRooted returns true when root detected`() {
        whenever(rootDetector.isRooted()).thenReturn(true)
        
        assertTrue(securityChecker.isDeviceRooted())
    }
    
    @Test
    fun `isDeviceRooted returns false when no root detected`() {
        whenever(rootDetector.isRooted()).thenReturn(false)
        
        assertFalse(securityChecker.isDeviceRooted())
    }
    
    @Test
    fun `isScreenMirroringActive delegates to detector`() {
        whenever(screenMirrorDetector.isScreenMirroringActive()).thenReturn(true)
        
        assertTrue(securityChecker.isScreenMirroringActive())
    }
    
    @Test
    fun `runAllChecks returns correct summary`() {
        whenever(rootDetector.isRooted()).thenReturn(false)
        whenever(screenMirrorDetector.isScreenMirroringActive()).thenReturn(false)
        
        val summary = securityChecker.runAllChecks()
        
        assertFalse(summary.isRooted)
        assertFalse(summary.screenMirroringActive)
    }
}
