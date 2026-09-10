package com.example.theboss.data.remote

import com.google.gson.annotations.SerializedName

data class School(
    @SerializedName("id") val id: String,
    @SerializedName("school_code") val schoolCode: String? = null,
    @SerializedName("school_name") val schoolName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("pairing_code") val pairingCode: String? = null,
    @SerializedName("config") val config: Map<String, Any>? = null
)

data class JoinRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("role") val role: String = "student",
    @SerializedName("full_name") val fullName: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("class_name") val className: String? = null,
    @SerializedName("section_name") val sectionName: String? = null,
    @SerializedName("status") val status: String = "approved"
)

data class GradeDto(
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("student_record_number") val studentRecordNumber: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("marks") val marks: Map<String, Any>
)

// نموذج جدول إعدادات التطبيق (app_config) في Supabase
data class AppConfigDto(
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: String,
    @SerializedName("school_id") val schoolId: String? = null
)

data class TimetableDto(
    @SerializedName("day") val day: String,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("teacher_name") val teacherName: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String
)

data class AssignmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("subject_id") val subjectId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("due_date") val dueDate: Long
)

data class DirectiveDto(
    @SerializedName("id") val id: String,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("target_role") val targetRole: String = "all",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DailyAssignmentDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("teacher_id") val teacherId: String,
    @SerializedName("class_name") val className: String,
    @SerializedName("section") val section: String,
    @SerializedName("subject_name") val subjectName: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("is_private_tutoring") val isPrivateTutoring: Boolean = false,
    @SerializedName("student_record_number") val studentRecordNumber: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DirectMessageDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("sender_role") val senderRole: String, // 'teacher' or 'student'
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("subject_name") val subjectName: String? = null,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false
)

data class TeacherAssignmentDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("teacher_id") val teacherId: String,
    @SerializedName("class_name") val className: String,
    @SerializedName("section") val section: String,
    @SerializedName("subject_name") val subjectName: String
)

data class TeacherDto(
    @SerializedName("id") val id: String,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("name") val name: String,
    @SerializedName("specialization") val specialization: String? = null
)

data class AttendanceDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("student_record_number") val studentRecordNumber: String,
    @SerializedName("date_string") val dateString: String,
    @SerializedName("status") val status: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("period_number") val periodNumber: Int = 1,
    @SerializedName("teacher_id") val teacherId: String? = null
)


