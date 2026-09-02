package com.example.theboss.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 1. جدول الملاحظات النصية والصوتية
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "audio_path")
    val audioPath: String? = null, // مسار الملف الصوتي المسجل محلياً

    @ColumnInfo(name = "audio_duration_seconds")
    val audioDurationSeconds: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

// 2. جدول المهام والواجبات اليومية
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "task_title")
    val title: String,

    @ColumnInfo(name = "due_date")
    val dueDate: Long, // تاريخ التسليم أو الإنجاز بالمللي ثانية

    @ColumnInfo(name = "priority")
    val priority: Int = 1, // 1: منخفض، 2: متوسط، 3: عالي

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "subject_tag")
    val subjectTag: String? = null // وسم المادة (رياضيات، فيزياء...)
)

// 3. جدول الامتحانات والعداد التنازلي
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "subject_name")
    val subjectName: String,

    @ColumnInfo(name = "exam_timestamp")
    val examTimestamp: Long, // موعد وتاريخ الامتحان

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "target_grade")
    val targetGrade: Int? = 100 // الدرجة المستهدفة
)

// 4. جدول التنبيهات والمؤقتات المجدولة
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "trigger_time")
    val triggerTime: Long, // وقت انطلاق التنبيه بالمللي ثانية

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "delay_minutes")
    val delayMinutes: Int = 0
)
