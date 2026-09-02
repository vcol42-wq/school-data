package com.principal.system.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teachers_overview")
data class TeacherOverviewEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val subject: String,
    val assignedClasses: String = "",
    val lastAssignmentTitle: String? = null,
    val lastAssignmentDate: String? = null,
    val gradesEnteredCount: Int = 0,
    val totalStudentsExpected: Int = 0,
    val gradeCompletionRate: Float = 0f,
    val pendingMessagesCount: Int = 0
)

@Entity(tableName = "attendance_summary")
data class AttendanceSummaryEntity(
    @PrimaryKey val dateString: String,
    val totalStudents: Int = 0,
    val absentStudentsCount: Int = 0,
    val attendanceRatePercent: Float = 100f,
    val period1Absences: Int = 0,
    val period2Absences: Int = 0,
    val period3Absences: Int = 0,
    val period4Absences: Int = 0,
    val period5Absences: Int = 0,
    val period6Absences: Int = 0,
    val period7Absences: Int = 0
)

@Entity(tableName = "critical_absences")
data class CriticalStudentAbsenceEntity(
    @PrimaryKey val studentRecordNumber: String,
    val fullName: String,
    val className: String,
    val section: String,
    val totalAbsenceDays: Int,
    val guardianPhone: String? = null,
    val lastAbsentDate: String = ""
)

@Entity(tableName = "broadcasts")
data class BroadcastEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val targetAudience: String = "all", // "all", "teachers", "students"
    val priority: String = "normal", // "urgent", "normal"
    val createdAt: String
)
