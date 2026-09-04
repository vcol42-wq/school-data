package com.school.system.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.gson.annotations.SerializedName
import com.school.system.data.SyncRepository
import com.school.system.data.dao.ConfigDao
import com.school.system.data.dao.StudentDao
import com.school.system.data.local.SecureKeyStorage
import com.school.system.data.StudentMarksDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Data Transfer Objects for the secure_upload_grades Supabase RPC
data class SecureUploadPayload(
    @SerializedName("p_grade") val grade: String,
    @SerializedName("p_section") val section: String,
    @SerializedName("p_subject") val subject: String,
    @SerializedName("p_secret_code") val secretCode: String,
    @SerializedName("p_device_fingerprint") val deviceFingerprint: String,
    @SerializedName("p_grades_payload") val gradesPayload: List<SupabaseGradeDto>,
    @SerializedName("p_school_id") val schoolId: String? = null
)

data class SupabaseGradeDto(
    @SerializedName("school_id") val school_id: String,
    @SerializedName("student_record_number") val student_record_number: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("section") val section: String,
    @SerializedName("marks") val marks: StudentMarksDto,
    @SerializedName("teacher_id") val teacher_id: String? = null
)

data class SecureUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

sealed class SecureUploadResult {
    data class Success(val message: String) : SecureUploadResult()
    data class InvalidPin(val reason: String) : SecureUploadResult()
    data class ClassLocked(val reason: String) : SecureUploadResult()
    data class Failure(val errorMessage: String) : SecureUploadResult()
}

interface SecureGradesApi {
    @POST("rest/v1/rpc/secure_upload_grades")
    @Headers("Content-Type: application/json")
    suspend fun secureUploadGrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body payload: SecureUploadPayload
    ): Response<SecureUploadResponse>
}

@Singleton
class GradesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val studentDao: StudentDao,
    private val configDao: ConfigDao,
    private val secureKeyStorage: SecureKeyStorage
) {
    private val tag = "GradesRepository"

    private fun getDeviceFingerprint(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEV"
        val model = Build.MODEL ?: "GENERIC_MODEL"
        val manufacturer = Build.MANUFACTURER ?: "GENERIC_MANUFACTURER"
        return "DEV_${manufacturer}_${model}_$androidId"
    }

    private fun createApiClient(baseUrl: String): SecureGradesApi {
        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SecureGradesApi::class.java)
    }

    /**
     * تنفيذ عملية رفع درجات الشعبة بصلاحيات مقفلة بيومترياً ومحمية بالرمز السري.
     */
    suspend fun uploadGradesSecurely(
        grade: String,
        section: String,
        subject: String,
        secretPin: String
    ): SecureUploadResult = withContext(Dispatchers.IO) {
        val subjectKey = secureKeyStorage.buildSubjectKey(grade, section, subject)

        try {
            val currentConfig = configDao.getConfig().first()
            val rawUrl = currentConfig?.cloudUrl?.trim().takeIf { !it.isNullOrEmpty() } ?: SyncRepository.DEFAULT_SUPABASE_URL
            val apiKey = currentConfig?.cloudKey?.trim().takeIf { !it.isNullOrEmpty() } ?: SyncRepository.DEFAULT_ANON_KEY
            val schoolId = currentConfig?.schoolId?.trim().takeIf { !it.isNullOrEmpty() } ?: "SCH-VCOL-6072"
            val authHeader = "Bearer $apiKey"

            // 1. جلب بيانات الطلاب محلياً وتحويلها إلى DTO
            val studentsList = studentDao.getStudentsListForClass(grade.trim(), section.trim(), subject.trim())
            if (studentsList.isEmpty()) {
                return@withContext SecureUploadResult.Failure("لا يوجد طلاب مسجلين في هذه الشعبة لرفع درجاتهم.")
            }

            val gradesPayload = studentsList.map { student ->
                val cleanRec = if (student.recordNumber.isNotBlank()) student.recordNumber.trim() else "std_${student.id}"
                val marksDto = StudentMarksDto(
                    m1Daily = student.marks.m1Daily,
                    m1Written = student.marks.m1Written,
                    m1MonthAvg = student.marks.m1MonthAvg,
                    m2Daily = student.marks.m2Daily,
                    m2Written = student.marks.m2Written,
                    m2MonthAvg = student.marks.m2MonthAvg,
                    term1Avg = student.marks.term1Avg,
                    midtermOral = student.marks.midtermOral,
                    midtermScore = student.marks.midtermScore,
                    midtermTotal = student.marks.midtermTotal,
                    midtermFinalGrade = student.marks.midtermFinalGrade,
                    m3Daily = student.marks.m3Daily,
                    m3Written = student.marks.m3Written,
                    m3MonthAvg = student.marks.m3MonthAvg,
                    m4Daily = student.marks.m4Daily,
                    m4Written = student.marks.m4Written,
                    m4MonthAvg = student.marks.m4MonthAvg,
                    term2Avg = student.marks.term2Avg,
                    annualAverage = student.marks.annualAverage,
                    finalOral = student.marks.finalOral,
                    finalWrittenD1 = student.marks.finalWrittenD1,
                    finalWrittenD2 = student.marks.finalWrittenD2,
                    finalExamTotal = student.marks.finalExamTotal,
                    finalGrade = student.marks.finalGrade,
                    result = student.marks.result,
                    status = student.marks.status
                )

                SupabaseGradeDto(
                    school_id = schoolId,
                    student_record_number = cleanRec,
                    subject = subject.trim(),
                    grade = grade.trim(),
                    section = section.trim(),
                    marks = marksDto,
                    teacher_id = currentConfig?.userEmail
                )
            }.distinctBy { "${it.school_id}__${it.student_record_number}__${it.subject}" }

            val payload = SecureUploadPayload(
                grade = grade.trim(),
                section = section.trim(),
                subject = subject.trim(),
                secretCode = secretPin.trim(),
                deviceFingerprint = getDeviceFingerprint(),
                gradesPayload = gradesPayload,
                schoolId = schoolId
            )

            val api = createApiClient(rawUrl)
            val response = api.secureUploadGrades(
                apiKey = apiKey,
                auth = authHeader,
                schoolId = schoolId,
                payload = payload
            )

            if (response.isSuccessful) {
                val body = response.body()
                val msg = body?.message ?: "تم اعتماد الدرجات ورفعها بنجاح ☁️✓"
                Log.d(tag, "Grades upload successful: $msg")
                // حفظ الرمز بعد التأكد من صحته وقبوله بالسيرفر
                secureKeyStorage.saveSubjectPin(subjectKey, secretPin)
                return@withContext SecureUploadResult.Success(msg)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Log.e(tag, "Upload failed with HTTP ${response.code()}: $errorBody")

                // فحص خطأ الرفض الأمني (رمز غير صحيح أو غير مصرح)
                if (errorBody.contains("رفض أمني") ||
                    errorBody.contains("غير صحيح") ||
                    errorBody.contains("غير مصرح") ||
                    errorBody.contains("P0001")
                ) {
                    // مسح الرمز القديم الملغى فوراً من التخزين المشفر
                    secureKeyStorage.clearSubjectPin(subjectKey)
                    return@withContext SecureUploadResult.InvalidPin(
                        "تم تحديث أو تغيير رمز اعتماد هذه المادة من الإدارة، يرجى إدخال الرمز الجديد"
                    )
                }

                // فحص قفل الشعبة من قبل الإدارة
                if (errorBody.contains("إجراء مرفوض") ||
                    errorBody.contains("تم إغلاق هذه الشعبة") ||
                    errorBody.contains("مقفلة")
                ) {
                    return@withContext SecureUploadResult.ClassLocked(
                        "إجراء مرفوض: تم إغلاق هذه الشعبة رسمياً من قبل إدارة المدرسة."
                    )
                }

                return@withContext SecureUploadResult.Failure("فشل الرفع الأمني: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception in uploadGradesSecurely: ${e.message}", e)
            return@withContext SecureUploadResult.Failure("خطأ في الاتصال بالسحابة: ${e.localizedMessage ?: e.message}")
        }
    }
}
