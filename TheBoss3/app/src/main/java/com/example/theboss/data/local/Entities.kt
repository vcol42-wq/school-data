package com.example.theboss.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val teacherName: String,
    val lastGrade: String? = null,
    val lastAssignment: String? = null
)

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: String,
    val type: String, // شفهي، نشاط، واجبات، امتحانات
    val score: Double,
    val maxScore: Double,
    val date: Long
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val subjectName: String = "",
    val title: String,
    val description: String,
    val dueDate: Long = 0L,
    val dueDateString: String = "",
    val isCompleted: Boolean = false,
    val isHot: Boolean = false, // Hot Badge (واجب جديد 🔥)
    val isPrivateTutoring: Boolean = false,
    val teacherName: String = ""
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = 0L,
    val dateString: String = "", // "yyyy-MM-dd"
    val status: String, // غياب، تأخير، حضور
    val periodNumber: Int = 1, // Lesson Period (1 to 7)
    val subject: String = "",
    val note: String? = null
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderRole: String, // "student" or "teacher"
    val receiverId: String,
    val subjectName: String,
    val messageText: String,
    val createdAt: String,
    val isRead: Boolean = false
)

