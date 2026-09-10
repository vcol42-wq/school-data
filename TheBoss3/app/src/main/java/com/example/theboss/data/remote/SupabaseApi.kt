package com.example.theboss.data.remote

import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {
    
    @GET("schools")
    suspend fun getSchools(
        @Query("id") idFilter: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<School>>

    @GET("schools")
    suspend fun getSchoolByCode(
        @Query("pairing_code") pairingCodeFilter: String? = null,
        @Query("id") idFilter: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<School>>

    @POST("join_requests")
    @Headers("Prefer: return=representation")
    suspend fun submitJoinRequest(
        @Body request: JoinRequest
    ): Response<List<JoinRequest>>

    @GET("join_requests")
    suspend fun checkRequestStatus(
        @Query("device_id") deviceFilter: String,
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<JoinRequest>>

    @GET("grades")
    suspend fun getStudentGrades(
        @Query("student_record_number") recordFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<GradeDto>>

    // جلب إعدادات التطبيق (مثل مفتاح Gemini API) من جدول app_config
    @GET("app_config")
    suspend fun getAppConfig(
        @Query("key") keyFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<AppConfigDto>>

    // جلب الجدول الدراسي
    @GET("timetable")
    suspend fun getTimetable(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TimetableDto>>

    // جلب تعليمات الأساتذة والواجبات
    @GET("assignments")
    suspend fun getTeacherInstructions(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<AssignmentDto>>

    @GET("directives")
    suspend fun getStudentDirectives(
        @Query("school_id") schoolFilter: String,
        @Query("target_role") roleFilter: String? = null,
        @Query("is_active") activeFilter: String = "eq.true",
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<DirectiveDto>>

    // جلب الواجبات والدروس اليومية
    @GET("daily_assignments")
    suspend fun getDailyAssignments(
        @Query("school_id") schoolFilter: String,
        @Query("class_name") classFilter: String? = null,
        @Query("section") sectionFilter: String? = null,
        @Query("or") orFilter: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<DailyAssignmentDto>>

    // جلب سجل الحضور والغياب (بما في ذلك رقم الحصة)
    @GET("attendance")
    suspend fun getAttendance(
        @Query("school_id") schoolFilter: String,
        @Query("student_record_number") recordFilter: String,
        @Query("order") order: String = "date_string.desc",
        @Query("select") select: String = "*"
    ): Response<List<AttendanceDto>>

    // إرسال رسالة مباشرة / سؤال للأستاذ
    @POST("direct_messages")
    @Headers("Prefer: return=representation")
    suspend fun sendDirectMessage(
        @Body message: DirectMessageDto
    ): Response<List<DirectMessageDto>>

    // جلب المحادثات والرسائل المباشرة
    @GET("direct_messages")
    suspend fun getDirectMessages(
        @Query("school_id") schoolFilter: String,
        @Query("or") orFilter: String? = null,
        @Query("order") order: String = "created_at.asc",
        @Query("select") select: String = "*"
    ): Response<List<DirectMessageDto>>

    // جلب المعلمين المسندين
    @GET("teacher_assignments")
    suspend fun getTeacherAssignments(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TeacherAssignmentDto>>

    // جلب بيانات المعلمين
    @GET("teachers")
    suspend fun getTeachers(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<TeacherDto>>
}

