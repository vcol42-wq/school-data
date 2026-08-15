package com.school.system.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DiyalaSchoolApi {

    @POST("api/sync/verify-otp")
    suspend fun verifyOtp(@Body request: OtpRequest): Response<OtpResponse>

    @POST("api/sync/upload-grades")
    suspend fun uploadGrades(@Body request: UploadGradesRequest): Response<SyncResponse>

    @retrofit2.http.GET("api/sync/get-students-by-token")
    suspend fun getStudentsByToken(@retrofit2.http.Query("token") token: String): Response<OtpResponse>

    @POST("api/sync/request-pairing")
    suspend fun requestPairing(@Body request: PairingRequest): Response<PairingResponse>

    @retrofit2.http.GET("api/sync/download-roster")
    suspend fun downloadRoster(
        @retrofit2.http.Query("schoolId") schoolId: String,
        @retrofit2.http.Query("token") token: String
    ): Response<OtpResponse>

    @retrofit2.http.GET("api/sync/simple-classes")
    suspend fun getSimpleClasses(): Response<SimpleClassesResponse>

    @retrofit2.http.GET("api/sync/simple-download-roster")
    suspend fun downloadSimpleRoster(
        @retrofit2.http.Query("grade") grade: String,
        @retrofit2.http.Query("section") section: String
    ): Response<SimpleRosterResponse>

    @retrofit2.http.GET("api/health")
    suspend fun checkHealth(): Response<Map<String, String>>

    @POST("api/ai-assistant")
    suspend fun askAiAssistant(@Body request: AiAssistantRequest): Response<AiAssistantResponse>
}

data class AiAssistantRequest(
    val query: String,
    val studentsCount: Int,
    val staffCount: Int,
    val schoolName: String,
    val userApiKey: String? = null
)

data class AiAssistantResponse(
    val success: Boolean,
    val responseText: String?,
    val action: String?,
    val targetView: String?,
    val targetTheme: String?,
    val searchQuery: String?,
    val error: String?
)

// Request & Response Data Transfer Objects (DTOs)

data class PairingRequest(
    val schoolId: String,
    val teacherName: String,
    val grade: String,
    val section: String,
    val subject: String,
    val pairingCode: String,
    val email: String? = null
)

data class PairingResponse(
    val success: Boolean,
    val warning: Boolean,
    val token: String?,
    val status: String?,
    val message: String
)

data class OtpRequest(val code: String)

data class OtpResponse(
    val success: Boolean,
    val token: String,
    val teacherName: String,
    val classes: List<ClassDto>,
    val students: List<StudentDto>
)

data class ClassDto(
    val grade: String,
    val section: String,
    val subject: String
)

data class StudentDto(
    val recordNumber: String,
    val fullName: String,
    val grade: String,
    val section: String,
    val historicalAbsences: Int
)

data class UploadGradesRequest(
    val schoolId: String,
    val token: String,
    val grade: String,
    val section: String,
    val subject: String,
    val gradesList: List<StudentGradeSyncDto>
)

data class StudentGradeSyncDto(
    val recordNumber: String,
    val marks: StudentMarksDto,
    val absencesCount: Int
)

data class StudentMarksDto(
    val m1Daily: List<Float>,
    val m1Written: Float,
    val m1MonthAvg: Float,
    val m2Daily: List<Float>,
    val m2Written: Float,
    val m2MonthAvg: Float,
    val term1Avg: Float,
    val midtermOral: List<Float>,
    val midtermScore: Float,
    val midtermTotal: Float,
    val midtermFinalGrade: Float,
    val m3Daily: List<Float>,
    val m3Written: Float,
    val m3MonthAvg: Float,
    val m4Daily: List<Float>,
    val m4Written: Float,
    val m4MonthAvg: Float,
    val term2Avg: Float,
    val annualAverage: Float,
    val finalOral: List<Float>,
    val finalWrittenD1: Float,
    val finalWrittenD2: Float?,
    val finalExamTotal: Float,
    val finalGrade: Float,
    val result: String,
    val status: String
)

data class SyncResponse(
    val success: Boolean,
    val message: String
)

data class SimpleClassesResponse(
    val success: Boolean,
    val classes: List<SimpleClassDto>
)

data class SimpleClassDto(
    val grade: String,
    val section: String
)

data class SimpleRosterResponse(
    val success: Boolean,
    val students: List<StudentDto>
)

