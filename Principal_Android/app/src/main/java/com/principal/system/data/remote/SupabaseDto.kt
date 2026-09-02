package com.principal.system.data.remote

import com.google.gson.annotations.SerializedName

data class SupabaseSchoolDto(
    @SerializedName("id") val id: String,
    @SerializedName("school_code") val schoolCode: String,
    @SerializedName("school_name") val schoolName: String
)

data class SupabaseTeacherDto(
    @SerializedName("id") val id: String,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("teacher_id") val teacherId: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class SupabaseAssignmentDto(
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

data class SupabaseAttendanceDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("student_record_number") val studentRecordNumber: String,
    @SerializedName("date_string") val dateString: String,
    @SerializedName("status") val status: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("period_number") val periodNumber: Int = 1,
    @SerializedName("teacher_id") val teacherId: String? = null
)

data class SupabaseGradeDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("student_record_number") val studentRecordNumber: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("teacher_id") val teacherId: String? = null,
    @SerializedName("marks") val marks: Map<String, Any>? = null
)

data class SupabaseStudentDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("record_number") val recordNumber: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("class_name") val className: String,
    @SerializedName("section") val section: String,
    @SerializedName("parent_phone") val parentPhone: String? = null
)

data class SupabaseDirectMessageDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("sender_role") val senderRole: String,
    @SerializedName("receiver_id") val receiverId: String,
    @SerializedName("subject_name") val subjectName: String? = null,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false
)

data class SupabaseBroadcastDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("target_audience") val targetAudience: String = "all",
    @SerializedName("priority") val priority: String = "normal",
    @SerializedName("created_at") val createdAt: String? = null
)
