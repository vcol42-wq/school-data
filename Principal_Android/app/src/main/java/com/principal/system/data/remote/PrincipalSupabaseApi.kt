package com.principal.system.data.remote

import retrofit2.Response
import retrofit2.http.*

interface PrincipalSupabaseApi {

    @GET("schools")
    suspend fun getSchoolByCode(
        @Query("pairing_code") codeFilter: String? = null,
        @Query("id") idFilter: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseSchoolDto>>

    @GET("teachers")
    suspend fun getTeachers(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseTeacherDto>>

    @GET("students")
    suspend fun getStudents(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseStudentDto>>

    @GET("daily_assignments")
    suspend fun getDailyAssignments(
        @Query("school_id") schoolFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<SupabaseAssignmentDto>>

    @GET("attendance")
    suspend fun getAttendance(
        @Query("school_id") schoolFilter: String,
        @Query("date_string") dateFilter: String? = null,
        @Query("order") order: String = "date_string.desc",
        @Query("select") select: String = "*"
    ): Response<List<SupabaseAttendanceDto>>

    @GET("grades")
    suspend fun getGrades(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseGradeDto>>

    @GET("direct_messages")
    suspend fun getDirectMessages(
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseDirectMessageDto>>

    @POST("direct_messages")
    @Headers("Prefer: return=representation")
    suspend fun sendBroadcastMessage(
        @Body message: SupabaseDirectMessageDto
    ): Response<List<SupabaseDirectMessageDto>>
}
