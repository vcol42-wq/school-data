package com.school.system.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "column_settings")
data class ColumnSetting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grade: String,
    val section: String,
    val subject: String,
    val columnName: String,
    val isPartOfCalculation: Boolean = false
)
