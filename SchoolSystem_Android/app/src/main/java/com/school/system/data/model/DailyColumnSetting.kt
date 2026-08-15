package com.school.system.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_column_settings")
data class DailyColumnSetting(
    @PrimaryKey val id: String, // format: "grade_section_subject"
    val columnNames: List<String> = List(30) { "" } // Increased to 30 for safety
)
