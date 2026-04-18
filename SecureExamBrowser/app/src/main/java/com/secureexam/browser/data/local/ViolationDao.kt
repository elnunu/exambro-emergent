package com.secureexam.browser.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.secureexam.browser.domain.model.Violation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Violation entity.
 */
@Dao
interface ViolationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(violation: Violation): Long
    
    @Update
    suspend fun update(violation: Violation)
    
    @Query("SELECT * FROM violations WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getViolationsForSession(sessionId: String): Flow<List<Violation>>
    
    @Query("SELECT * FROM violations WHERE syncedToMoodle = 0")
    suspend fun getUnsyncedViolations(): List<Violation>
    
    @Query("UPDATE violations SET syncedToMoodle = 1 WHERE id = :violationId")
    suspend fun markAsSynced(violationId: Long)
    
    @Query("SELECT COUNT(*) FROM violations WHERE sessionId = :sessionId")
    suspend fun getViolationCount(sessionId: String): Int
    
    @Query("DELETE FROM violations WHERE sessionId = :sessionId")
    suspend fun clearViolations(sessionId: String)
    
    @Query("DELETE FROM violations")
    suspend fun clearAll()
}
