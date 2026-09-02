package com.school.system.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_alarms")
data class LessonAlarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1=Sunday, 2=Monday, ..., 5=Thursday
    val lessonIndex: Int, // 1 to 6
    val lessonName: String = "",
    val startTime: String, // HH:mm
    val endTime: String,   // HH:mm
    val isEnabled: Boolean = true
)

data class BellSettings(
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val lessonDuration: Int = 40,
    val breakDuration: Int = 10,
    val ringtoneUri: String? = null,
    val isGlobalEnabled: Boolean = true
)
