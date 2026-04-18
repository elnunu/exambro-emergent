package com.secureexam.browser.data.local

import androidx.room.TypeConverter
import com.secureexam.browser.domain.model.ViolationType

/**
 * Type converters for Room database.
 */
class Converters {
    
    @TypeConverter
    fun fromViolationType(type: ViolationType): String {
        return type.name
    }
    
    @TypeConverter
    fun toViolationType(value: String): ViolationType {
        return try {
            ViolationType.valueOf(value)
        } catch (e: Exception) {
            ViolationType.UNKNOWN
        }
    }
}
