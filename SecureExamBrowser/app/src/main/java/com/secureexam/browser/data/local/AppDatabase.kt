package com.secureexam.browser.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.secureexam.browser.domain.model.Violation

/**
 * Room database for local data persistence.
 */
@Database(
    entities = [Violation::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun violationDao(): ViolationDao
}
