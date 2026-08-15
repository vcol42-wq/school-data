package com.school.system.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_packages")
data class ClassPackage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grade: String,
    val section: String,
    val subject: String,
    val iconName: String
)
