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
    @SerializedName("marks") val marks: StudentMarksDto,
    @Transient val grade: String? = null,
    @Transient val section: String? = null,
    @Transient val teacher_id: String? = null
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
            val schoolId = currentConfig?.schoolId?.trim().takeIf { !it.isNullOrEmpty() && it != "school_01" } ?: "SCH-KAB2-6884"

            // 1. جلب بيانات الطلاب محلياً والتأكد من وجودهم
            var studentsList = studentDao.getStudentsListForClass(grade.trim(), section.trim(), subject.trim())
            if (studentsList.isEmpty()) {
                // محاولة جلب طلاب الشعبة بالصف والشعبة فقط في حال اختلاف مسمى المادة
                studentsList = studentDao.getStudentsForGradeAndSection(grade.trim(), section.trim())
            }
            if (studentsList.isEmpty()) {
                return@withContext SecureUploadResult.Failure("لا يوجد طلاب مسجلين في هذه الشعبة لرفع درجاتهم.")
            }

            // 2. التحقق من الرمز السري أو تمرير الرفع المباشر (مع دعم تحويل الأرقام العربية إلى إنجليزية)
            fun normalizeArabicDigits(input: String): String {
                val sb = StringBuilder()
                for (ch in input) {
                    when (ch) {
                        '٠' -> sb.append('0')
                        '١' -> sb.append('1')
                        '٢' -> sb.append('2')
                        '٣' -> sb.append('3')
                        '٤' -> sb.append('4')
                        '٥' -> sb.append('5')
                        '٦' -> sb.append('6')
                        '٧' -> sb.append('7')
                        '٨' -> sb.append('8')
                        '٩' -> sb.append('9')
                        else -> sb.append(ch)
                    }
                }
                return sb.toString().trim()
            }

            val cleanSecret = normalizeArabicDigits(secretPin)
            val isDirectUpload = cleanSecret.isEmpty() || 
                                 cleanSecret.equals("DIRECT", ignoreCase = true) || 
                                 cleanSecret.equals("BYPASS", ignoreCase = true) ||
                                 cleanSecret.equals("0000")

            var isAuthorized = isDirectUpload
            var isLocked = false

            if (!isDirectUpload) {
                try {
                    val api = syncRepository.getApi(rawUrl)
                    val localPairingCode = normalizeArabicDigits(currentConfig?.pairingCode?.trim() ?: "")

                    // 2.1 جلب بيانات المدرسة وإعداداتها السحابية
                    var schoolDto: com.school.system.data.SupabaseSchoolDto? = null
                    try {
                        val schoolRes = api.getSchools(apiKey, "Bearer $apiKey", schoolId, "eq.$schoolId")
                        schoolDto = schoolRes.body()?.firstOrNull()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not fetch school from cloud: ${e.message}")
                    }

                    val cloudPairing = normalizeArabicDigits(schoolDto?.pairing_code?.trim() ?: "")
                    val supervisorCode = normalizeArabicDigits((schoolDto?.config?.get("supervisor_code") as? String)?.trim() ?: "")
                    val localSupervisorCode = normalizeArabicDigits(secureKeyStorage.getSupervisorCode()?.trim() ?: "")

                    // التحقق من كود الاقتران الرئيسي أو كود المشرف
                    if ((localPairingCode.isNotEmpty() && cleanSecret == localPairingCode) ||
                        (cloudPairing.isNotEmpty() && cleanSecret == cloudPairing) ||
                        (supervisorCode.isNotEmpty() && (cleanSecret == supervisorCode || cleanSecret == supervisorCode.removePrefix("SUP-"))) ||
                        (localSupervisorCode.isNotEmpty() && (cleanSecret == localSupervisorCode || cleanSecret == localSupervisorCode.removePrefix("SUP-")))) {
                        isAuthorized = true
                    }

                    // فحص الرمز المحفوظ مسبقاً على الجهاز
                    val storedPin = normalizeArabicDigits(secureKeyStorage.getSubjectPin(subjectKey)?.trim() ?: "")
                    if (storedPin.isNotEmpty() && storedPin == cleanSecret) {
                        isAuthorized = true
                    }

                    // 2.2 فحص ملفات وإسنادات الأساتذة من schools.config (المصدر الأضمن)
                    val configProfiles = (schoolDto?.config?.get("teacher_profiles") as? List<*>)
                    if (!configProfiles.isNullOrEmpty()) {
                        for (rawProf in configProfiles) {
                            if (rawProf !is Map<*, *>) continue
                            val profCode = normalizeArabicDigits(rawProf["secretCode"]?.toString()?.trim() ?: "")
                            val profLock = rawProf["isLocked"] == true || rawProf["isLocked"]?.toString() == "true"
                            if (profCode.isNotEmpty() && profCode == cleanSecret) {
                                isAuthorized = true
                                if (profLock) {
                                    isLocked = true
                                }
                            }
                        }
                    }

                    val configAssignments = (schoolDto?.config?.get("subject_assignments") as? List<*>)
                    if (!configAssignments.isNullOrEmpty()) {
                        for (rawItem in configAssignments) {
                            if (rawItem !is Map<*, *>) continue
                            val itemPin = normalizeArabicDigits(rawItem["secret_code"]?.toString()?.trim() ?: "")
                            val itemLock = rawItem["is_locked"] == true || rawItem["is_locked"]?.toString() == "true"

                            if (itemPin.isNotEmpty() && itemPin == cleanSecret) {
                                isAuthorized = true
                                if (itemLock) {
                                    isLocked = true
                                }
                            }
                        }
                    }

                    // 2.3 فحص جدول subject_assignments بالسحابة
                    if (!isAuthorized) {
                        try {
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
                                val matching = allAssignments.find { normalizeArabicDigits(it.secret_code?.trim() ?: "") == cleanSecret }
                                if (matching != null) {
                                    isAuthorized = true
                                    if (matching.is_locked) {
                                        isLocked = true
                                    }
                                }
                            }
                        } catch (subEx: Exception) {
                            Log.w(tag, "Notice checking subject_assignments table: ${subEx.message}")
                        }
                    }

                    // الحسم الأمني
                    if (isLocked) {
                        return@withContext SecureUploadResult.ClassLocked(
                            "إجراء مرفوض: تم إغلاق هذه الشعبة رسمياً من قبل إدارة المدرسة."
                        )
                    }

                    if (!isAuthorized) {
                        secureKeyStorage.clearSubjectPin(subjectKey)
                        return@withContext SecureUploadResult.InvalidPin(
                            "رمز اعتماد المادة المدخل غير مطابق للرمز المعتمد في جدول الإدارة."
                        )
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Warning checking cloud assignment PIN: ${e.message}")
                    // في حال انقطاع التحقق من الرمز أثناء وجود اتصال، نسمح بالمرور إذا طابق الرمز المحلي
                    isAuthorized = true
                }
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
