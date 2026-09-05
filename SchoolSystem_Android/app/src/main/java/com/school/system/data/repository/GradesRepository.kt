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
    private val secureKeyStorage: SecureKeyStorage,
    private val syncRepository: SyncRepository
) {
    private val tag = "GradesRepository"

    private fun getDeviceFingerprint(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEV"
        val model = Build.MODEL ?: "GENERIC_MODEL"
        val manufacturer = Build.MANUFACTURER ?: "GENERIC_MANUFACTURER"
        return "DEV_${manufacturer}_${model}_$androidId"
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
            val schoolId = currentConfig?.schoolId?.trim().takeIf { !it.isNullOrEmpty() && it != "school_01" } ?: "SCH-VCOL-6072"

            // 1. جلب بيانات الطلاب محلياً والتأكد من وجودهم
            val studentsList = studentDao.getStudentsListForClass(grade.trim(), section.trim(), subject.trim())
            if (studentsList.isEmpty()) {
                return@withContext SecureUploadResult.Failure("لا يوجد طلاب مسجلين في هذه الشعبة لرفع درجاتهم.")
            }

            // 2. التحقق من الرمز وقفل الشعبة من جدول subject_assignments بالسحابة
            try {
                val api = syncRepository.getApi(rawUrl)
                val cleanSecret = secretPin.trim()
                val localPairingCode = currentConfig?.pairingCode?.trim() ?: ""

                // 2.1 التحقق من رمز الاقتران الموحد للمدرسة (Master Pairing Code)
                var isMasterAuthorized = localPairingCode.isNotEmpty() && cleanSecret == localPairingCode
                if (!isMasterAuthorized) {
                    try {
                        val schoolRes = api.getSchools(apiKey, "Bearer $apiKey", schoolId, "eq.$schoolId")
                        val cloudPairing = schoolRes.body()?.firstOrNull()?.pairing_code?.trim() ?: ""
                        if (cloudPairing.isNotEmpty() && cloudPairing == cleanSecret) {
                            isMasterAuthorized = true
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Could not check cloud pairing code: ${e.message}")
                    }
                }

                // 2.2 جلب الإسنادات الخاصة بالمدرسة بدون تقييد صارم للنص لتفادي اختلافات التسمية (الأول vs الأول المتوسط)
                val subRes = api.getSubjectAssignments(
                    apiKey = apiKey,
                    auth = "Bearer $apiKey",
                    schoolId = schoolId,
                    schoolFilter = "eq.$schoolId",
                    gradeFilter = null,
                    sectionFilter = null,
                    subjectFilter = null
                )
                if (subRes.isSuccessful && !subRes.body().isNullOrEmpty()) {
                    val allAssignments = subRes.body()!!
                    val assignment = allAssignments.find { a ->
                        syncRepository.isGradeMatch(a.grade, grade) &&
                        syncRepository.standardizeSectionName(a.section) == syncRepository.standardizeSectionName(section) &&
                        syncRepository.isSubjectMatch(a.subject, subject)
                    }

                    if (assignment != null) {
                        if (assignment.is_locked) {
                            return@withContext SecureUploadResult.ClassLocked(
                                "إجراء مرفوض: تم إغلاق هذه الشعبة رسمياً من قبل إدارة المدرسة."
                            )
                        }
                        val cloudPin = assignment.secret_code?.trim() ?: ""
                        if (cloudPin.isNotEmpty()) {
                            val matchesSpecificPin = cloudPin == cleanSecret
                            val matchesTeacherPin = allAssignments.any { 
                                it.secret_code?.trim() == cleanSecret && 
                                (it.teacher_name?.trim() == assignment.teacher_name?.trim() || it.teacher_name.isNullOrBlank())
                            }

                            if (!matchesSpecificPin && !matchesTeacherPin && !isMasterAuthorized) {
                                secureKeyStorage.clearSubjectPin(subjectKey)
                                return@withContext SecureUploadResult.InvalidPin(
                                    "رمز اعتماد المادة المدخل غير مطابق للرمز المعتمد في جدول الإدارة."
                                )
                            }
                        }
                    } else if (!isMasterAuthorized) {
                        // في حال عدم وجود المادة بالسحابة ولكن الرمز يطابق رمز معتمد لأي مادة أخرى بالمدرسة
                        val matchesAnyKnownCode = allAssignments.any { it.secret_code?.trim() == cleanSecret }
                        if (!matchesAnyKnownCode && cleanSecret.length !in 4..8) {
                            secureKeyStorage.clearSubjectPin(subjectKey)
                            return@withContext SecureUploadResult.InvalidPin(
                                "رمز اعتماد المادة المدخل غير مطابق للرمز المعتمد في جدول الإدارة."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Warning checking cloud assignment PIN: ${e.message}")
            }

            // 3. رفع الدرجات وحالات الغياب مباشرة إلى سحابة Supabase عبر syncRepository
            val syncSuccess = syncRepository.syncGradesAndAttendance(
                schoolId = schoolId,
                teacherId = currentConfig?.userEmail ?: currentConfig?.managerName,
                targetGrade = grade,
                targetSection = section,
                targetSubject = subject
            )

            if (syncSuccess) {
                // حفظ الرمز بعد التأكد من صحته ونجاح الرفع
                secureKeyStorage.saveSubjectPin(subjectKey, secretPin)
                Log.d(tag, "Grades upload successful for $subject ($grade - $section)")
                return@withContext SecureUploadResult.Success("تم اعتماد درجات ($subject) ورفعها للسحابة بنجاح ☁️✓")
            } else {
                return@withContext SecureUploadResult.Failure("تعذر إتمام رفع الدرجات للسحابة. يرجى التحقق من اتصال الإنترنت والمحاولة مجدداً.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception in uploadGradesSecurely: ${e.message}", e)
            return@withContext SecureUploadResult.Failure("خطأ في الاتصال بالسحابة: ${e.localizedMessage ?: e.message}")
        }
    }
}
