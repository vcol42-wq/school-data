package com.school.system.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.school.system.data.dao.*
import com.school.system.data.model.*
import com.school.system.utils.TeacherNotificationHelper
import com.school.system.widget.DailyScheduleWidgetProvider
import com.school.system.widget.FullScheduleWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// PostgREST DTOs matching Supabase PostgreSQL tables
data class SupabaseTeacherDto(
    val id: String,
    val school_id: String = "",
    val name: String,
    val email: String? = null,
    val specialization: String? = null
)

data class SupabaseAssignmentDto(
    val id: String = "",
    val school_id: String,
    val teacher_id: String,
    val class_name: String,
    val section: String,
    val subject_name: String,
    val secret_code: String? = null
)

data class SupabaseSubjectAssignmentDto(
    val id: Long? = null,
    val school_id: String,
    val grade: String,
    val section: String,
    val subject: String,
    val secret_code: String? = null,
    val is_locked: Boolean = false,
    val teacher_name: String? = null
)

data class SchoolClassSubjectItem(
    val grade: String,
    val section: String,
    val subject: String,
    val teacherName: String? = null,
    val teacherId: String? = null,
    var isSelected: Boolean = false
)


data class SupabaseStudentDto(
    val school_id: String? = null,
    val record_number: String? = null,
    val first_name: String? = null,
    val second_name: String? = null,
    val third_name: String? = null,
    val fourth_name: String? = null,
    val title_name: String? = null,
    val full_name: String? = null,
    val current_grade: String? = null,
    val section: String? = null,
    val absences_count: Int? = 0,
    val status: String? = "مستمر"
)

data class SupabaseGradeDto(
    val school_id: String,
    val student_record_number: String,
    val subject: String,
    val marks: StudentMarksDto, // Maps directly to PostgreSQL jsonb column via Gson
    val grade: String? = null,
    val section: String? = null,
    val teacher_id: String? = null
)

data class SupabaseAttendanceDto(
    val school_id: String,
    val student_record_number: String,
    val date: String,
    val status: String = "absent",
    @Transient val date_string: String? = null,
    @Transient val subject: String? = null,
    @Transient val period_number: Int = 1
)

data class SupabaseDailyAssignmentDto(
    val id: String? = null,
    val school_id: String,
    val teacher_id: String,
    val class_name: String,
    val section: String,
    val subject_name: String,
    val title: String,
    val description: String?,
    val due_date: String?,
    val is_private_tutoring: Boolean = false,
    val student_record_number: String? = null,
    val created_at: String? = null
)

data class SupabaseDirectMessageDto(
    val id: String? = null,
    val school_id: String,
    val sender_id: String,
    val sender_role: String, // 'teacher' or 'student'
    val receiver_id: String,
    val subject_name: String?,
    val message_text: String,
    val created_at: String? = null,
    val is_read: Boolean = false
)

data class SupabaseSchoolDto(
    val id: String,
    val name: String,
    val pairing_code: String,
    val admin_email: String,
    val config: Map<String, Any>? = null
)

data class SupabaseScheduleDto(
    val id: String,
    val schedule_map: Map<String, Any>?
)

data class SupabaseDirectiveDto(
    val id: String = "",
    val school_id: String = "",
    val title: String = "",
    val content: String = "",
    val target_role: String = "all",
    val is_active: Boolean = true,
    val created_at: String? = null
)

data class SupabaseJoinRequestDto(
    val school_id: String,
    val role: String = "teacher",
    val full_name: String,
    val class_name: String? = null,
    val section: String? = null,
    val subject_name: String? = null,
    val status: String = "approved",
    val pairing_code_attempt: String? = null
)

interface SupabaseApi {
    @GET("rest/v1/schools")
    suspend fun getSchools(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String? = null,
        @Query("id") idFilter: String? = null,
        @Query("pairing_code") pairingCodeFilter: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseSchoolDto>>

    @GET("rest/v1/teachers")
    suspend fun getAllTeachers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String
    ): Response<List<SupabaseTeacherDto>>

    @GET("rest/v1/teachers")
    suspend fun getTeachers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("or") idOrNameFilter: String
    ): Response<List<SupabaseTeacherDto>>

    @GET("rest/v1/teacher_assignments")
    suspend fun getTeacherAssignments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("teacher_id") teacherFilter: String? = null
    ): Response<List<SupabaseAssignmentDto>>

    @GET("rest/v1/subject_assignments")
    suspend fun getSubjectAssignments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("grade") gradeFilter: String? = null,
        @Query("section") sectionFilter: String? = null,
        @Query("subject") subjectFilter: String? = null,
        @Query("secret_code") secretCodeFilter: String? = null
    ): Response<List<SupabaseSubjectAssignmentDto>>

    @GET("rest/v1/classes")
    suspend fun getClasses(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/subjects")
    suspend fun getSubjects(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/students")
    suspend fun getStudents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("current_grade") gradeFilter: String? = null,
        @Query("section") sectionFilter: String? = null
    ): Response<List<SupabaseStudentDto>>

    @POST("rest/v1/students")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertStudents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body students: List<SupabaseStudentDto>
    ): Response<Void>

    @DELETE("rest/v1/grades")
    suspend fun deleteGrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("subject") subjectFilter: String? = null
    ): Response<Void>

    @POST("rest/v1/grades")
    suspend fun insertGrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body grades: List<SupabaseGradeDto>
    ): Response<Void>

    @DELETE("rest/v1/attendance")
    suspend fun deleteAttendance(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String
    ): Response<Void>

    @POST("rest/v1/attendance")
    suspend fun insertAttendance(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body attendance: List<SupabaseAttendanceDto>
    ): Response<Void>

    @POST("rest/v1/daily_assignments")
    @Headers("Prefer: return=representation")
    suspend fun insertDailyAssignment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body assignment: SupabaseDailyAssignmentDto
    ): Response<List<SupabaseDailyAssignmentDto>>

    @POST("rest/v1/daily_assignments")
    @Headers("Prefer: return=minimal")
    suspend fun insertDailyAssignmentSimple(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body assignment: SupabaseDailyAssignmentDto
    ): Response<Void>

    @GET("rest/v1/daily_assignments")
    suspend fun getDailyAssignments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("teacher_id") teacherFilter: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseDailyAssignmentDto>>

    @POST("rest/v1/direct_messages")
    @Headers("Prefer: return=representation")
    suspend fun sendDirectMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body message: SupabaseDirectMessageDto
    ): Response<List<SupabaseDirectMessageDto>>

    @GET("rest/v1/direct_messages")
    suspend fun getDirectMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("or") orFilter: String? = null,
        @Query("order") order: String = "created_at.asc"
    ): Response<List<SupabaseDirectMessageDto>>

    @GET("rest/v1/students")
    suspend fun getStudentByName(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("full_name") nameFilter: String,
        @Query("current_grade") gradeFilter: String,
        @Query("section") sectionFilter: String
    ): Response<List<SupabaseStudentDto>>

    @GET("rest/v1/grades")
    suspend fun getStudentGrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("student_record_number") recordFilter: String
    ): Response<List<SupabaseGradeDto>>

    @GET("rest/v1/attendance")
    suspend fun getStudentAttendance(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("student_record_number") recordFilter: String
    ): Response<List<SupabaseAttendanceDto>>

    @GET("rest/v1/schools")
    suspend fun getSchools(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("pairing_code") pairingCodeFilter: String
    ): Response<List<SupabaseSchoolDto>>

    @GET("rest/v1/schedules")
    suspend fun getSchedules(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("or") orFilter: String? = null,
        @Query("id") idFilter: String? = null
    ): Response<List<SupabaseScheduleDto>>

    @POST("rest/v1/join_requests")
    @Headers("Prefer: return=representation")
    suspend fun insertJoinRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Body request: SupabaseJoinRequestDto
    ): Response<List<SupabaseJoinRequestDto>>

    @GET("rest/v1/directives")
    suspend fun getDirectives(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("target_role") roleFilter: String = "in.(all,teacher,teachers,staff)",
        @Query("is_active") activeFilter: String = "eq.true",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseDirectiveDto>>

    @POST("rest/v1/directives")
    suspend fun sendDirective(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body directive: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<SupabaseDirectiveDto>>

    @DELETE("rest/v1/directives")
    suspend fun deleteDirective(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("id") idFilter: String
    ): Response<Unit>
}

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configDao: ConfigDao,
    private val studentDao: StudentDao,
    private val packageDao: ClassPackageDao,
    private val absenceDao: AbsenceDao
) {
    // Default compile-time connection credentials.
    // Can be dynamically set/overridden by QR code configurations.
    companion object {
        const val DEFAULT_SUPABASE_URL = "https://pexehlvkpdhmpukjydwd.supabase.co/"
        const val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY"
    }

    fun getApi(url: String): SupabaseApi {
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        val baseUrl = if (formattedUrl.endsWith("/")) formattedUrl else "$formattedUrl/"

        val client = OkHttpClient.Builder()
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }

    private fun isValidSupabaseUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim().lowercase()
        // Reject known placeholder/invalid URLs
        if (trimmed.contains("your-supabase-project")) return false
        if (trimmed.contains("placeholder")) return false
        if (trimmed.contains("example.com")) return false
        
        // Must be a valid URL format and contain supabase.co or be a local/private development IP
        return trimmed.startsWith("http") && (
            trimmed.contains("supabase.co") || 
            trimmed.contains("localhost") || 
            trimmed.contains("192.168.") ||
            trimmed.contains(".app") || // For Railway/Vercel/etc
            trimmed.contains(".run")
        )
    }

    private suspend fun resolveCredentials(providedUrl: String?, providedKey: String?): Pair<String, String> {
        val currentConfig = configDao.getConfig().first()
        
        // Prioritize provided URL, then stored config URL, then production default
        // But validate each one before using it
        val url = when {
            !providedUrl.isNullOrEmpty() && isValidSupabaseUrl(providedUrl) -> providedUrl
            isValidSupabaseUrl(currentConfig?.cloudUrl) -> currentConfig!!.cloudUrl
            else -> DEFAULT_SUPABASE_URL
        }
        
        val key = when {
            !providedKey.isNullOrEmpty() -> providedKey
            !currentConfig?.cloudKey.isNullOrEmpty() -> currentConfig!!.cloudKey
            else -> DEFAULT_ANON_KEY
        }
        
        return Pair(url, key)
    }

    /**
     * استرجاع أقفال رصد الدرجات حسب الأشهر والفترات المحددة من إدارة المدرسة
     */
    suspend fun fetchGradeLocks(schoolId: String): Map<String, Boolean> {
        if (schoolId.isBlank()) return emptyMap()
        return try {
            val (url, key) = resolveCredentials(null, null)
            val api = getApi(url)
            val cleanSchoolId = schoolId.trim()
            val response = api.getSchools(
                apiKey = key,
                auth = "Bearer $key",
                schoolId = cleanSchoolId,
                idFilter = "eq.$cleanSchoolId"
            )
            if (response.isSuccessful) {
                val school = response.body()?.firstOrNull()
                val config = school?.config
                val locksObj = config?.get("grade_locks")
                if (locksObj is Map<*, *>) {
                    locksObj.mapNotNull { (k, v) ->
                        val keyStr = k?.toString() ?: return@mapNotNull null
                        val boolVal = when (v) {
                            is Boolean -> v
                            is String -> v.toBoolean()
                            is Number -> v.toInt() == 1
                            else -> false
                        }
                        keyStr to boolVal
                    }.toMap()
                } else {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun verifySchoolByPairingCode(code: String): SupabaseSchoolDto? {
        return try {
            val clean = code.trim()
            if (clean.isBlank()) return null
            val (url, key) = resolveCredentials(null, null)
            val api = getApi(url)

            // 1. Check pairing_code
            var response = api.getSchools(
                apiKey = key,
                auth = "Bearer $key",
                pairingCodeFilter = "eq.$clean"
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                return response.body()!!.first()
            }

            // 2. Check idFilter
            response = api.getSchools(
                apiKey = key,
                auth = "Bearer $key",
                idFilter = "eq.$clean"
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                return response.body()!!.first()
            }

            // 3. Fallback: all schools match
            val allResponse = api.getSchools(apiKey = key, auth = "Bearer $key")
            if (allResponse.isSuccessful && !allResponse.body().isNullOrEmpty()) {
                return allResponse.body()!!.firstOrNull { s ->
                    s.pairing_code.equals(clean, ignoreCase = true) || s.id.equals(clean, ignoreCase = true)
                }
            }
            null
        } catch (e: Exception) {
            Log.e("SyncRepository", "verifySchoolByPairingCode error: ${e.message}")
            null
        }
    }

    /**
     * Normalizes Arabic strings to ensure proper matches
     */
    fun normalizeArabic(str: String?): String {
        if (str == null) return ""
        return str.trim()
            .replace("[أإآ]".toRegex(), "ا")
            .replace("ة".toRegex(), "ه")
            .replace("ى".toRegex(), "ي")
            .replace("^الصف\\s+".toRegex(), "")
            .replace("(^|\\s)ال".toRegex(), "$1")
            .replace("\\s+".toRegex(), "")
    }

    /**
     * القائمة الرسمية للمواد الدراسية المعتمدة وزارياً
     */
    val STANDARD_APPROVED_SUBJECTS = listOf(
        "التربية الإسلامية",
        "اللغة العربية",
        "اللغة الإنكليزية",
        "الرياضيات",
        "الاجتماعيات",
        "الأحياء",
        "الكيمياء",
        "الفيزياء",
        "الحاسوب",
        "النشاط البدني",
        "التربية الفنية",
        "التربية الأخلاقية",
        "العلوم",
        "العلوم العامة",
        "التاريخ",
        "الجغرافيا",
        "التربية الوطنية",
        "الاقتصاد",
        "الفلسفة وعلم النفس",
        "علم الأرض",
        "اللغة الفرنسية",
        "النشيد والموسيقى"
    )

    fun isApprovedStandardSubject(name: String): Boolean {
        val norm = normalizeArabic(name.trim()).lowercase()
        return STANDARD_APPROVED_SUBJECTS.any { normalizeArabic(it).lowercase() == norm }
    }

    /**
     * Standardizes Subject Name considering all common aliases, variations, and misspellings:
     * - If the subject corresponds to an official ministry curriculum subject, it reverts strictly to the approved spelling.
     * - If it is a new/custom curriculum subject outside the approved list, it is preserved cleanly.
     */
    fun standardizeSubjectName(raw: String?): String {
        if (raw.isNullOrBlank()) return "المادة العامة"
        val s = raw.trim()

        // استبعاد الأحرف المفردة التي تشير إلى شعبة وتم تمريرها خطأ كمادة
        if (s.length <= 1 || s == "أ" || s == "ب" || s == "ج" || s == "د" || s == "هـ") {
            return "المادة العامة"
        }

        val norm = normalizeArabic(s).lowercase()
        val rawLower = s.lowercase().replace("[^a-z0-9\u0600-\u06FF]".toRegex(), "")

        // 1. اللغة الإنكليزية: جميع التسميات والرموز
        if (norm.contains("انكل") || norm.contains("انجل") || norm.contains("انقل") ||
            rawLower.contains("engl") || rawLower.startsWith("eng") || rawLower.endsWith("eng") ||
            rawLower == "e" || rawLower == "en" || rawLower == "el" ||
            rawLower.contains("english") || norm.contains("انكلش") || norm.contains("انجلش") ||
            norm.contains("انجليز") || norm.contains("انكليز")
        ) {
            return "اللغة الإنكليزية"
        }

        // 2. الأحياء
        if (norm.contains("احيا") || norm.contains("علماحياء") || norm.contains("علوماحياء") ||
            norm.contains("بايو") || rawLower.contains("bio")
        ) {
            return "الأحياء"
        }

        // 3. التربية الإسلامية
        if (norm.contains("اسلام") || norm.contains("قران") || norm.contains("دين") ||
            norm.contains("عقيد") || norm.contains("فقه") || norm.contains("شريع")
        ) {
            return "التربية الإسلامية"
        }

        // 4. الكيمياء
        if (norm.contains("كيم") || norm.contains("كمي") || rawLower.contains("chem")) {
            return "الكيمياء"
        }

        // 5. الفيزياء
        if (norm.contains("فيز") || rawLower.contains("phys")) {
            return "الفيزياء"
        }

        // 6. النشاط البدني
        if (norm == "رياضه" || norm.contains("تربيهرياض") || norm.contains("العاب") ||
            norm.contains("بدني") || rawLower == "pe" || rawLower.contains("sport")
        ) {
            return "النشاط البدني"
        }

        // 7. الرياضيات
        if ((norm.contains("رياض") && !norm.contains("بدني") && !norm.contains("العاب") && !norm.contains("تربيه")) ||
            norm.contains("حساب") || norm.contains("جبر") || norm.contains("هندس") ||
            norm.contains("تفاضل") || norm.contains("تكامل") || rawLower.contains("math")
        ) {
            return "الرياضيات"
        }

        // 8. التربية الفنية
        if (norm.contains("فني") || norm.contains("رسم") || norm.contains("فنون") || rawLower.contains("art")) {
            return "التربية الفنية"
        }

        // 9. التربية الأخلاقية
        if (norm.contains("اخلاق")) {
            return "التربية الأخلاقية"
        }

        // 10. الحاسوب
        if (norm.contains("حاس") || norm.contains("كمبيوتر") || norm.contains("كومبيوتر") ||
            norm.contains("برمج") || norm.contains("معلومات") || rawLower == "it" || rawLower.contains("computer")
        ) {
            return "الحاسوب"
        }

        // 11. اللغات والمواد التخصصية
        if (norm.contains("فرنس") || rawLower.contains("french")) {
            return "اللغة الفرنسية"
        }
        if (norm.contains("ارض") || norm.contains("جيولوج")) {
            return "علم الأرض"
        }
        if (norm.contains("فلسف") || norm.contains("علمنفس") || (norm.contains("نفس") && norm.contains("علم"))) {
            return "الفلسفة وعلم النفس"
        }
        if (norm.contains("اقتصاد")) {
            return "الاقتصاد"
        }
        if (norm.contains("تاريخ")) {
            return "التاريخ"
        }
        if (norm.contains("جغراف")) {
            return "الجغرافيا"
        }
        if (norm.contains("وطني") && !norm.contains("لغ")) {
            return "التربية الوطنية"
        }
        if (norm.contains("اجتماع") || norm.contains("دراساتاجتماعي")) {
            return "الاجتماعيات"
        }
        if (norm.contains("موسيق") || norm.contains("نشيد")) {
            return "النشيد والموسيقى"
        }

        // 12. العلوم / العلوم العامة
        if (norm == "علوم" || norm.contains("علوماسام") || norm.contains("علومعام") || rawLower.contains("science")) {
            return "العلوم"
        }

        // 13. اللغة العربية
        if (norm.contains("عرب") || norm.contains("قواعد") || norm.contains("ادب") ||
            norm.contains("نصوص") || norm.contains("بلاغ") || norm.contains("املا") ||
            norm.contains("انشا") || norm.contains("قراءه") || norm.contains("مطالع")
        ) {
            return "اللغة العربية"
        }

        // 14. إذا كانت مادة مخصصة جديدة خارج المواد المعتمدة
        return s
    }

    /**
     * Standardizes Grade level (الأول إلى السادس) and stage/track (ابتدائي، متوسط، علمي، أدبي، صناعي، تجاري، أخرى)
     */
    fun standardizeGradeName(gradeStr: String?): String {
        if (gradeStr.isNullOrBlank()) return "الأول"
        val clean = gradeStr.trim().replace("^(الصف|صف)\\s+".toRegex(), "").trim()
        val norm = normalizeArabic(clean).lowercase()

        var base = "الأول"
        if (norm.contains("سادس") || norm.contains("6") || norm.contains("٦")) base = "السادس"
        else if (norm.contains("خامس") || norm.contains("5") || norm.contains("٥")) base = "الخامس"
        else if (norm.contains("رابع") || norm.contains("4") || norm.contains("٤")) base = "الرابع"
        else if (norm.contains("ثالث") || norm.contains("3") || norm.contains("٣")) base = "الثالث"
        else if (norm.contains("ثاني") || norm.contains("2") || norm.contains("٢")) base = "الثاني"
        else if (norm.contains("اول") || norm.contains("1") || norm.contains("١")) base = "الأول"

        var branch = ""
        if (norm.contains("احيائ") || norm.contains("تطبيق") || norm.contains("علم")) branch = "العلمي"
        else if (norm.contains("ادب")) branch = "الأدبي"
        else if (norm.contains("صناع")) branch = "الصناعي"
        else if (norm.contains("تجار")) branch = "التجاري"
        else if (norm.contains("متوسط")) branch = "المتوسط"
        else if (norm.contains("اعداد") || norm.contains("ثانوي")) branch = "الإعدادي"
        else if (norm.contains("ابتدائ")) branch = "الابتدائي"

        if (branch.isEmpty()) {
            if (base == "الأول" || base == "الثاني" || base == "الثالث") {
                branch = "المتوسط"
            } else if (base == "الرابع" || base == "الخامس" || base == "السادس") {
                branch = "الإعدادي"
            }
        }

        return if (branch.isNotEmpty()) "$base $branch" else base
    }

    /**
     * Checks if two grade strings represent the same grade level and stage
     */
    fun isGradeMatch(g1: String?, g2: String?): Boolean {
        if (g1.isNullOrBlank() || g2.isNullOrBlank()) return false
        val std1 = standardizeGradeName(g1)
        val std2 = standardizeGradeName(g2)
        if (std1 == std2) return true

        fun extractBase(g: String): String {
            val norm = normalizeArabic(g)
            return when {
                norm.contains("سادس") || norm.contains("6") || norm.contains("٦") -> "سادس"
                norm.contains("خامس") || norm.contains("5") || norm.contains("٥") -> "خامس"
                norm.contains("رابع") || norm.contains("4") || norm.contains("٤") -> "رابع"
                norm.contains("ثالث") || norm.contains("3") || norm.contains("٣") -> "ثالث"
                norm.contains("ثاني") || norm.contains("2") || norm.contains("٢") -> "ثاني"
                norm.contains("اول") || norm.contains("1") || norm.contains("١") -> "اول"
                else -> norm
            }
        }

        val b1 = extractBase(g1)
        val b2 = extractBase(g2)
        if (b1 != b2) return false

        val n1 = normalizeArabic(g1)
        val n2 = normalizeArabic(g2)
        val hasBranch1 = n1.contains("متوسط") || n1.contains("اعداد") || n1.contains("ابتدائ") || n1.contains("ثانوي") || n1.contains("علم") || n1.contains("ادب")
        val hasBranch2 = n2.contains("متوسط") || n2.contains("اعداد") || n2.contains("ابتدائ") || n2.contains("ثانوي") || n2.contains("علم") || n2.contains("ادب")

        if (!hasBranch1 || !hasBranch2) return true
        return std1 == std2
    }

    /**
     * Standardizes Section (أ، ب، ج، د، هـ، و، ز، ح، ...)
     */
    fun standardizeSectionName(secStr: String?): String {
        if (secStr.isNullOrBlank()) return "أ"
        val clean = secStr.trim().replace("^(شعبة|الشعبة|ش)\\s*".toRegex(), "").trim()
        val lower = clean.lowercase()

        // Strip stage and grade names first so "الصف الأول ب" converts to "ب" properly
        val letterOnly = clean
            .replace("(الصف|صف|الأول|الاول|الثاني|الثالث|الرابع|الخامس|السادس|المتوسط|الإعدادي|الاعدادي|الابتدائي|العلمي|الأدبي|الادبي)".toRegex(), "")
            .trim()
        val target = if (letterOnly.isNotBlank()) letterOnly else clean
        val targetLower = target.lowercase()

        if (target == "ا" || target == "أ" || target == "إ" || target == "آ" || targetLower == "a" || targetLower == "1" || targetLower == "١") return "أ"
        if (target == "ب" || targetLower == "b" || targetLower == "2" || targetLower == "٢") return "ب"
        if (target == "ج" || targetLower == "c" || targetLower == "3" || targetLower == "٣") return "ج"
        if (target == "د" || targetLower == "d" || targetLower == "4" || targetLower == "٤") return "د"
        if (target == "ه" || target == "هـ" || targetLower == "e" || targetLower == "5" || targetLower == "٥") return "هـ"
        if (target == "و" || targetLower == "f" || targetLower == "6" || targetLower == "٦") return "و"
        if (target == "ز" || targetLower == "z" || targetLower == "7" || targetLower == "٧") return "ز"
        if (target == "ح" || targetLower == "h" || targetLower == "8" || targetLower == "٨") return "ح"
        if (target == "ط" || targetLower == "9" || targetLower == "٩") return "ط"
        if (target == "خ") return "خ"
        return if (target.length == 1 && target[0].isLetter()) target else "أ"
    }

    /**
     * Checks if two subject strings represent the same subject
     */
    fun isSubjectMatch(s1: String?, s2: String?): Boolean {
        if (s1.isNullOrBlank() || s2.isNullOrBlank()) return false
        val std1 = standardizeSubjectName(s1)
        val std2 = standardizeSubjectName(s2)
        if (std1 == std2) return true
        val n1 = normalizeArabic(s1)
        val n2 = normalizeArabic(s2)
        return n1 == n2 || (n1.length >= 3 && n2.contains(n1)) || (n2.length >= 3 && n1.contains(n2))
    }


    /**
     * Verifies school_id and pairingCode/teacherId against Supabase 'teachers' table
     */
    suspend fun verifySchoolAndTeacher(
        schoolId: String,
        teacherInput: String,
        pairingCode: String,
        providedUrl: String? = null,
        providedKey: String? = null
    ): PairingResult {
        return try {
            val (url, apiKey) = resolveCredentials(providedUrl, providedKey)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            // 1. Fetch School Name from Supabase
            var schoolName = "مدرسة سحابية"
            try {
                val schoolRes = api.getSchools(apiKey, authHeader, schoolId, "eq.$schoolId")
                if (schoolRes.isSuccessful && !schoolRes.body().isNullOrEmpty()) {
                    schoolName = schoolRes.body()!!.first().name
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 2. Look up teacher in Supabase strictly by name or ID
            var matchedTeacher: SupabaseTeacherDto? = null
            val cleanInput = teacherInput.trim()

            // 2.1 التحقق مما إذا كان المدخل كود المشرف العام / المدير / جهة رقابية
            val isSupervisor = cleanInput.startsWith("SUP-", ignoreCase = true) ||
                               cleanInput.contains("مشرف") ||
                               cleanInput.equals("supervisor", ignoreCase = true) ||
                               cleanInput.startsWith("SUPERVISOR:", ignoreCase = true)

            if (isSupervisor) {
                val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                configDao.saveConfig(
                    currentConfig.copy(
                        schoolId = schoolId,
                        schoolName = schoolName,
                        managerName = "المشرف العام / المدير",
                        syncSealToken = "__supervisor__",
                        role = "supervisor",
                        cloudUrl = url,
                        cloudKey = apiKey,
                        pairingCode = pairingCode,
                        isVerified = true,
                        isActivated = true
                    )
                )

                val rosterSuccess = downloadRoster(schoolId, "__supervisor__", url, apiKey)
                return if (rosterSuccess) {
                    PairingResult(
                        success = true, 
                        warning = false, 
                        message = "تم تسجيل دخول المشرف العام بنجاح! تم تنزيل كافة الشعب والصفوف للاطلاع والمتابعة (وضع القراءة فقط) ✓"
                    )
                } else {
                    PairingResult(
                        success = true, 
                        warning = true, 
                        message = "تم تسجيل دخول المشرف العام. لم يتم العثور على جداول مرفوعة للمدرسة في السحابة حتى الآن."
                    )
                }
            }

            var targetName = cleanInput
            var targetPin = ""

            if (cleanInput.startsWith("TEACHER:", ignoreCase = true)) {
                val parts = cleanInput.split(":")
                if (parts.size >= 4) {
                    targetPin = parts[1].trim()
                    targetName = parts[3].trim()
                } else if (parts.size == 3) {
                    targetPin = parts[1].trim()
                    if (!parts[2].startsWith("SCH-", ignoreCase = true)) {
                        targetName = parts[2].trim()
                    }
                } else if (parts.size == 2) {
                    targetPin = parts[1].trim()
                    targetName = ""
                }
            }

            if (targetPin.isBlank() && cleanInput.length in 4..10 && cleanInput.all { it.isDigit() || it == '-' }) {
                targetPin = cleanInput
            }

            // Query all teachers of this school to perform match
            val allTeachersRes = try {
                api.getAllTeachers(
                    apiKey = apiKey,
                    auth = authHeader,
                    schoolId = schoolId,
                    schoolFilter = "eq.$schoolId"
                )
            } catch (e: Exception) { null }

            val allTeachers = if (allTeachersRes?.isSuccessful == true) {
                allTeachersRes.body() ?: emptyList()
            } else emptyList()

            // 1. Try matching teacher by Name or ID
            if (targetName.isNotBlank() && allTeachers.isNotEmpty()) {
                val normInput = normalizeArabic(targetName.replace("^(أ\\.|أستاذ\\s*)\\s*".toRegex(), "").trim())
                matchedTeacher = allTeachers.find { t ->
                    val normTeacherName = normalizeArabic(t.name.replace("^(أ\\.|أستاذ\\s*)\\s*".toRegex(), "").trim())
                    t.id == targetName ||
                    normTeacherName == normInput ||
                    (normInput.length >= 3 && normTeacherName.contains(normInput)) ||
                    (normTeacherName.length >= 3 && normInput.contains(normTeacherName))
                }
            }

            // 1.5 Check schools.config.teacher_profiles (The gold standard source of truth for unified PIN)
            if (matchedTeacher == null) {
                try {
                    val schoolRes = api.getSchools(apiKey = apiKey, auth = authHeader, schoolId = schoolId, idFilter = "eq.$schoolId")
                    if (schoolRes.isSuccessful && !schoolRes.body().isNullOrEmpty()) {
                        val schoolDto = schoolRes.body()!!.firstOrNull()
                        val configProfiles = (schoolDto?.config?.get("teacher_profiles") as? List<*>)
                        if (!configProfiles.isNullOrEmpty()) {
                            val pinCandidates = listOf(targetPin, pairingCode, cleanInput).filter { it.isNotBlank() }
                            for (rawProf in configProfiles) {
                                if (rawProf !is Map<*, *>) continue
                                val profPin = normalizeArabic(rawProf["secretCode"]?.toString()?.trim() ?: "")
                                val profName = rawProf["teacherName"]?.toString()?.trim() ?: ""
                                val profSpec = rawProf["specialization"]?.toString()?.trim() ?: ""
                                val profId = rawProf["teacherId"]?.toString()?.trim() ?: ""
                                val isExempt = rawProf["isExempt"] == true || rawProf["isExempt"]?.toString() == "true"
                                if (isExempt) continue

                                for (pin in pinCandidates) {
                                    if (profPin.isNotBlank() && profPin == normalizeArabic(pin)) {
                                        matchedTeacher = SupabaseTeacherDto(
                                            id = profId.ifBlank { "tch_${profName.hashCode()}" },
                                            name = profName.ifBlank { "أستاذ المادة" },
                                            specialization = profSpec
                                        )
                                        break
                                    }
                                }
                                if (matchedTeacher != null) break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Notice checking config teacher_profiles: ${e.message}")
                }
            }

            // 2. If not matched, lookup by secret_code (PIN) in subject_assignments & teacher_assignments!
            if (matchedTeacher == null) {
                val pinCandidates = listOf(targetPin, pairingCode, cleanInput).filter { it.isNotBlank() }
                
                // 2.1 Check subject_assignments with strict sanity filtering (reject single letters / exempt titles)
                try {
                    val subAssignRes = api.getSubjectAssignments(apiKey, authHeader, schoolId, "eq.$schoolId")
                    if (subAssignRes.isSuccessful && !subAssignRes.body().isNullOrEmpty()) {
                        val subAssignments = subAssignRes.body()!!.filter { s ->
                            val subj = (s.subject ?: "").trim()
                            val grd = (s.grade ?: "").trim()
                            subj.length > 1 &&
                            !subj.matches("^[أ-يa-zA-Z]$".toRegex()) &&
                            !subj.contains("مفرغ") &&
                            !subj.contains("إدارة") &&
                            !subj.contains("تفرغ") &&
                            !subj.contains("شاغر") &&
                            (grd.contains("المتوسط") || grd.contains("الإعدادي") || grd.contains("الابتدائي"))
                        }
                        for (pin in pinCandidates) {
                            val foundSub = subAssignments.find { (it.secret_code ?: "").trim() == pin.trim() }
                            if (foundSub != null) {
                                val tName = foundSub.teacher_name?.trim() ?: ""
                                if (tName.isNotBlank() && allTeachers.isNotEmpty()) {
                                    val normTName = normalizeArabic(tName)
                                    matchedTeacher = allTeachers.find { normalizeArabic(it.name) == normTName || it.name.contains(tName) }
                                }
                                if (matchedTeacher == null) {
                                    matchedTeacher = SupabaseTeacherDto(
                                        id = tName.ifBlank { "tch_${foundSub.id ?: 1}" },
                                        name = tName.ifBlank { "أستاذ المادة" },
                                        specialization = foundSub.subject
                                    )
                                }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Warning fetching subject_assignments: ${e.message}")
                }

                // 2.2 Check teacher_assignments
                if (matchedTeacher == null && allTeachers.isNotEmpty()) {
                    val assignRes = try {
                        api.getTeacherAssignments(apiKey, authHeader, schoolId, null)
                    } catch (e: Exception) { null }

                    val assignments = if (assignRes?.isSuccessful == true) {
                        assignRes.body() ?: emptyList()
                    } else emptyList()

                    for (pin in pinCandidates) {
                        val foundAssign = assignments.find { (it.secret_code ?: "").trim() == pin.trim() }
                        if (foundAssign != null) {
                            matchedTeacher = allTeachers.find { it.id == foundAssign.teacher_id }
                            if (matchedTeacher != null) break
                        }
                    }
                }
            }

            if (matchedTeacher != null) {
                // Save settings in Room database config
                val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                configDao.saveConfig(
                    currentConfig.copy(
                        schoolId = schoolId,
                        schoolName = schoolName,
                        managerName = matchedTeacher.name,
                        syncSealToken = matchedTeacher.id, // Stores verified Teacher ID
                        cloudUrl = url,
                        cloudKey = apiKey,
                        pairingCode = pairingCode,
                        isVerified = true,
                        isActivated = true
                    )
                )

                // Register presence in join_requests in Supabase
                try {
                    api.insertJoinRequest(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = schoolId,
                        request = SupabaseJoinRequestDto(
                            school_id = schoolId,
                            role = "teacher",
                            full_name = matchedTeacher.name,
                            class_name = "الأول",
                            section = "أ",
                            subject_name = matchedTeacher.specialization?.ifBlank { "عام" } ?: "عام",
                            status = "approved",
                            pairing_code_attempt = pairingCode
                        )
                    )
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Non-blocking presence registration: ${e.message}")
                }

                // Download Roster exclusively for this registered teacher
                val rosterSuccess = downloadRoster(schoolId, matchedTeacher.id, url, apiKey)
                if (rosterSuccess) {
                    PairingResult(success = true, warning = false, message = "تم التحقق من الأستاذ (${matchedTeacher.name}) وتنزيل الشعب والمواد الموكلة له بنجاح!")
                } else {
                    PairingResult(success = true, warning = true, message = "تم ربط حسابك (${matchedTeacher.name}) بنجاح. لم يتم العثور على حصص مخصصة لك في جدول الإدارة حتى الآن.")
                }
            } else {
                // 3. Fallback: If pairing code or school QR is valid, pair with school directly
                val isSchoolCodeValid = pairingCode.isNotBlank() || cleanInput.startsWith("SCH-", ignoreCase = true) || cleanInput.length in 4..10
                if (isSchoolCodeValid) {
                    val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                    val savedName = currentConfig.managerName.ifBlank { "كادر المدرسة" }
                    val teacherIdToken = if (allTeachers.isNotEmpty()) allTeachers.first().id else "__all__"
                    
                    configDao.saveConfig(
                        currentConfig.copy(
                            schoolId = schoolId,
                            schoolName = schoolName,
                            managerName = savedName,
                            syncSealToken = "__school_paired__",
                            cloudUrl = url,
                            cloudKey = apiKey,
                            pairingCode = pairingCode.ifBlank { cleanInput },
                            isVerified = true,
                            isActivated = true
                        )
                    )

                    try {
                        api.insertJoinRequest(
                            apiKey = apiKey,
                            auth = authHeader,
                            schoolId = schoolId,
                            request = SupabaseJoinRequestDto(
                                school_id = schoolId,
                                role = "teacher",
                                full_name = savedName,
                                class_name = "عام",
                                section = "أ",
                                subject_name = "عام",
                                status = "approved",
                                pairing_code_attempt = pairingCode.ifBlank { cleanInput }
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("SyncRepository", "Non-blocking presence registration: ${e.message}")
                    }

                    // Pair successfully without flooding the phone with all 30+ school classes
                    PairingResult(
                        success = true, 
                        warning = false, 
                        message = "تم اقتران مدرسة ($schoolName) بنجاح! يمكنك الآن اختيار وتنزيل شعبك وموادك فقط 📥"
                    )
                } else {
                    // Clear any lingering data from previous logins
                    packageDao.clearAll()
                    studentDao.clearAll()

                    // Unregistered teachers or empty input
                    PairingResult(
                        success = false, 
                        warning = true, 
                        message = if (cleanInput.isBlank()) {
                            "يرجى كتابة اسمك الثلاثي في حقل (اسم المعلم) في شاشة الضبط ثم إعادة مسح الباركود أو المزامنة."
                        } else {
                            "عذراً، لم يتم العثور على المعلم (${cleanInput}) في كادر المدرسة بالسحابة. يرجى التأكد من كتابة الاسم كما مسجل لدى الإدارة."
                        }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PairingResult(success = false, warning = false, message = "فشل الاتصال بالسحابة: ${e.localizedMessage}")
        }
    }

    /**
     * Downloads teacher assignments and assigned students from Supabase.
     * Enforces strict teacher-exclusive assignment (no cross-teacher downloads) and max 60 students per section.
     */
    suspend fun downloadRoster(
        schoolId: String,
        teacherId: String,
        providedUrl: String? = null,
        providedKey: String? = null
    ): Boolean {
        if (teacherId.isBlank()) return false
        return try {
            val (url, apiKey) = resolveCredentials(providedUrl, providedKey)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            val isSupervisor = teacherId == "__supervisor__" || teacherId == "__all__" || teacherId == "__school_paired__"

            // 0. Resolve teacher name & ID for robust matching
            val allTeachers = try { 
                api.getAllTeachers(apiKey, authHeader, schoolId, "eq.$schoolId").body() ?: emptyList() 
            } catch(e: Exception) { emptyList() }
            val matchedT = allTeachers.find { it.id == teacherId || normalizeArabic(it.name) == normalizeArabic(teacherId) || it.name.contains(teacherId) }
            val teacherName = matchedT?.name ?: teacherId

            // 1. Fetch assignments: If supervisor or all, fetch ALL assignments without teacher filter!
            val assignmentsResponse = try { 
                if (isSupervisor) {
                    api.getTeacherAssignments(apiKey, authHeader, schoolId, null)
                } else {
                    api.getTeacherAssignments(apiKey, authHeader, schoolId, "eq.$teacherId")
                }
            } catch (e: Exception) { null }
            
            var assignments = if (assignmentsResponse?.isSuccessful == true) {
                assignmentsResponse.body() ?: emptyList()
            } else emptyList()

            // Fallback: If filtered query returned empty, try fetching all assignments and match locally by ID or Name
            if (assignments.isEmpty() && !isSupervisor) {
                val allAssignRes = try {
                    api.getTeacherAssignments(apiKey, authHeader, schoolId, null)
                } catch (e: Exception) { null }

                if (allAssignRes?.isSuccessful == true && !allAssignRes.body().isNullOrEmpty()) {
                    val allList = allAssignRes.body()!!
                    val filtered = allList.filter { 
                        it.teacher_id == teacherId || 
                        it.teacher_id == matchedT?.id ||
                        normalizeArabic(it.teacher_id) == normalizeArabic(teacherName) ||
                        normalizeArabic(it.teacher_id) == normalizeArabic(teacherId)
                    }
                    assignments = if (filtered.isNotEmpty()) filtered else allList
                }
            }

            // If empty, fallback to subject_assignments (where desktop app also stores assignments)
            if (assignments.isEmpty()) {
                val subAssignRes = try {
                    api.getSubjectAssignments(apiKey, authHeader, schoolId, "eq.$schoolId")
                } catch (e: Exception) { null }

                if (subAssignRes?.isSuccessful == true && !subAssignRes.body().isNullOrEmpty()) {
                    val subList = subAssignRes.body()!!
                    assignments = if (isSupervisor) {
                        subList.map { 
                            SupabaseAssignmentDto(
                                school_id = schoolId,
                                teacher_id = it.teacher_name ?: "__all__",
                                class_name = it.grade,
                                section = it.section,
                                subject_name = it.subject,
                                secret_code = it.secret_code
                            )
                        }
                    } else {
                        val filtered = subList.filter { 
                            val tName = it.teacher_name?.trim() ?: ""
                            tName == teacherId.trim() || 
                            tName == teacherName.trim() ||
                            normalizeArabic(tName) == normalizeArabic(teacherId) ||
                            normalizeArabic(tName) == normalizeArabic(teacherName) ||
                            (tName.length >= 3 && normalizeArabic(teacherName).contains(normalizeArabic(tName)))
                        }
                        (if (filtered.isNotEmpty()) filtered else subList).map {
                            SupabaseAssignmentDto(
                                school_id = schoolId,
                                teacher_id = it.teacher_name ?: teacherId,
                                class_name = it.grade,
                                section = it.section,
                                subject_name = it.subject,
                                secret_code = it.secret_code
                            )
                        }
                    }
                }
            }

            val studentsResponse = try { 
                api.getStudents(apiKey, authHeader, schoolId, "eq.$schoolId") 
            } catch (e: Exception) { null }
            val studentsList = if (studentsResponse?.isSuccessful == true) {
                studentsResponse.body() ?: emptyList()
            } else emptyList()

            if (assignments.isEmpty()) {
                // Check if local packages already exist before deciding what to do
                val localPkgs = packageDao.getAllPackagesList()
                if (localPkgs.isNotEmpty()) {
                    // Reconstruct assignments from existing packages
                    assignments = localPkgs.map { pkg ->
                        SupabaseAssignmentDto(
                            school_id = schoolId,
                            teacher_id = teacherId,
                            class_name = pkg.grade,
                            section = pkg.section,
                            subject_name = pkg.subject
                        )
                    }
                } else if (studentsList.isNotEmpty()) {
                    // Reconstruct assignments directly from cloud students list
                    assignments = studentsList.mapNotNull { s ->
                        if (s.current_grade.isNullOrBlank()) null
                        else Pair(standardizeGradeName(s.current_grade), standardizeSectionName(s.section))
                    }.distinct().map { (grd, sec) ->
                        SupabaseAssignmentDto(
                            school_id = schoolId,
                            teacher_id = teacherId,
                            class_name = grd,
                            section = sec,
                            subject_name = "عام"
                        )
                    }
                } else {
                    return false
                }
            }

            // Save old student marks to merge back
            val existingStudents = studentDao.getAllStudentsList()

            // Clear old local packages and students for clean synchronization
            packageDao.clearAll()
            studentDao.clearAll()

            // 2. Insert packages ONLY for this teacher's assigned classes & subjects
            assignments.forEach { assign ->
                val stdGrd = standardizeGradeName(assign.class_name)
                val stdSec = standardizeSectionName(assign.section)
                val stdSubj = standardizeSubjectName(assign.subject_name)

                packageDao.insertPackage(
                    ClassPackage(
                        grade = stdGrd,
                        section = stdSec,
                        subject = stdSubj,
                        iconName = "yrd"
                    )
                )
            }

            // 3. Insert students strictly for this teacher's assigned classes & subjects
            val maxStudentsPerSection = 100
            val collator = java.text.Collator.getInstance(java.util.Locale("ar")).apply {
                strength = java.text.Collator.PRIMARY
            }

            assignments.forEach { assign ->
                val targetGradeStd = standardizeGradeName(assign.class_name)
                val targetSecStd = standardizeSectionName(assign.section)
                val targetSubjStd = standardizeSubjectName(assign.subject_name)

                val matchedStudents = studentsList.filter { stdDto ->
                    val g = stdDto.current_grade ?: ""
                    val s = stdDto.section ?: ""
                    isGradeMatch(g, targetGradeStd) &&
                    (standardizeSectionName(s) == targetSecStd || targetSecStd == "الكل" || (targetSecStd == "اللغة" && standardizeSectionName(s) == "أ"))
                }.sortedWith { s1, s2 ->
                    val n1 = s1.full_name?.trim()?.replace("^\\d+[\\.\\-\\s]+".toRegex(), "") ?: ""
                    val n2 = s2.full_name?.trim()?.replace("^\\d+[\\.\\-\\s]+".toRegex(), "") ?: ""
                    collator.compare(n1, n2)
                }.take(maxStudentsPerSection)

                matchedStudents.forEach { stdDto ->
                    val recNum = stdDto.record_number?.takeIf { it.isNotBlank() } ?: stdDto.full_name?.hashCode()?.toString() ?: "0"
                    val fName = stdDto.full_name?.trim()?.takeIf { it.isNotBlank() } ?: "طالب"
                    val newStudent = Student(
                        recordNumber = recNum,
                        fullName = fName,
                        grade = targetGradeStd,
                        section = targetSecStd,
                        subject = targetSubjStd,
                        historicalAbsences = stdDto.absences_count ?: 0
                    )

                    val matchedOld = existingStudents.find { oldStd ->
                        isGradeMatch(oldStd.grade, targetGradeStd) &&
                        standardizeSectionName(oldStd.section) == targetSecStd &&
                        standardizeSubjectName(oldStd.subject) == targetSubjStd &&
                        ((oldStd.recordNumber.isNotBlank() && oldStd.recordNumber == recNum) ||
                         (normalizeArabic(oldStd.fullName) == normalizeArabic(fName)))
                    }

                    if (matchedOld != null) {
                        studentDao.insertStudent(newStudent.copy(marks = matchedOld.marks))
                    } else {
                        studentDao.insertStudent(newStudent)
                    }
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Retrieves all available class-subject pairings in the school from Supabase
     */
    suspend fun getSchoolAvailableClasses(
        schoolId: String,
        providedUrl: String? = null,
        providedKey: String? = null
    ): List<SchoolClassSubjectItem> {
        return try {
            val (url, apiKey) = resolveCredentials(providedUrl, providedKey)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            val resultMap = mutableMapOf<String, SchoolClassSubjectItem>()

            // 1. Fetch from teacher_assignments
            try {
                val assignRes = api.getTeacherAssignments(apiKey, authHeader, schoolId, null)
                if (assignRes.isSuccessful && !assignRes.body().isNullOrEmpty()) {
                    assignRes.body()!!.forEach { a ->
                        val stdGrd = standardizeGradeName(a.class_name)
                        val stdSec = standardizeSectionName(a.section)
                        val stdSubj = standardizeSubjectName(a.subject_name)
                        val key = "$stdGrd-$stdSec-$stdSubj"
                        resultMap[key] = SchoolClassSubjectItem(
                            grade = stdGrd,
                            section = stdSec,
                            subject = stdSubj,
                            teacherId = a.teacher_id,
                            teacherName = a.teacher_id
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "getTeacherAssignments error: ${e.message}")
            }

            // 2. Fetch from subject_assignments (where desktop records teacher_name and codes)
            try {
                val subRes = api.getSubjectAssignments(apiKey, authHeader, schoolId, "eq.$schoolId")
                if (subRes.isSuccessful && !subRes.body().isNullOrEmpty()) {
                    subRes.body()!!.forEach { s ->
                        val rawSub = (s.subject ?: "").trim()
                        if (rawSub.length <= 1 || rawSub.matches("^[أ-يa-zA-Z]$".toRegex()) || rawSub.contains("مفرغ") || rawSub.contains("إدارة") || rawSub.contains("تفرغ") || rawSub.contains("شاغر")) return@forEach

                        val stdGrd = standardizeGradeName(s.grade)
                        val stdSec = standardizeSectionName(s.section)
                        val stdSubj = standardizeSubjectName(rawSub)
                        if (stdSubj.isBlank() || stdSubj.length <= 1) return@forEach

                        val key = "$stdGrd-$stdSec-$stdSubj"
                        val existing = resultMap[key]
                        val tName = s.teacher_name?.takeIf { it.isNotBlank() } ?: existing?.teacherName
                        resultMap[key] = SchoolClassSubjectItem(
                            grade = stdGrd,
                            section = stdSec,
                            subject = stdSubj,
                            teacherName = tName,
                            teacherId = existing?.teacherId
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "getSubjectAssignments error: ${e.message}")
            }

            // 3. Resolve teacher names and specializations from teachers table
            var teacherMap = mapOf<String, SupabaseTeacherDto>()
            var teacherBySpec = mapOf<String, SupabaseTeacherDto>()
            try {
                val teachersRes = api.getAllTeachers(apiKey, authHeader, schoolId, "eq.$schoolId")
                if (teachersRes.isSuccessful && !teachersRes.body().isNullOrEmpty()) {
                    val tList = teachersRes.body()!!
                    teacherMap = tList.associateBy { it.id }
                    teacherBySpec = tList.filter { !it.specialization.isNullOrBlank() }
                        .associateBy { standardizeSubjectName(it.specialization!!) }

                    resultMap.values.forEach { item ->
                        if (item.teacherId != null && teacherMap.containsKey(item.teacherId)) {
                            val realName = teacherMap[item.teacherId]?.name
                            if (!realName.isNullOrBlank()) {
                                resultMap["${item.grade}-${item.section}-${item.subject}"] = item.copy(teacherName = realName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "getAllTeachers error: ${e.message}")
            }

            // 4. Fetch distinct grades and sections from students table to ensure no section (ب, ج, د) is omitted!
            try {
                val studentsRes = api.getStudents(apiKey, authHeader, schoolId, "eq.$schoolId")
                val distinctFromStudents = if (studentsRes.isSuccessful && !studentsRes.body().isNullOrEmpty()) {
                    studentsRes.body()!!.mapNotNull { s ->
                        val g = s.current_grade?.takeIf { it.isNotBlank() }
                        val sec = s.section?.takeIf { it.isNotBlank() }
                        if (g != null && sec != null) Pair(standardizeGradeName(g), standardizeSectionName(sec)) else null
                    }.distinct()
                } else emptyList()

                val coreSubjects = listOf("الرياضيات", "اللغة العربية", "اللغة الإنجليزية", "التربية الإسلامية", "العلوم", "الاجتماعيات", "الحاسوب")
                
                distinctFromStudents.forEach { (grd, sec) ->
                    coreSubjects.forEach { subj ->
                        val stdGrd = standardizeGradeName(grd)
                        val stdSec = standardizeSectionName(sec)
                        val stdSubj = standardizeSubjectName(subj)
                        val key = "$stdGrd-$stdSec-$stdSubj"
                        if (!resultMap.containsKey(key)) {
                            val matchedTeacher = teacherBySpec[stdSubj]
                            resultMap[key] = SchoolClassSubjectItem(
                                grade = stdGrd,
                                section = stdSec,
                                subject = stdSubj,
                                teacherName = matchedTeacher?.name,
                                teacherId = matchedTeacher?.id
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "Error discovering classes from students: ${e.message}")
            }

            resultMap.values.sortedWith(
                compareBy<SchoolClassSubjectItem> { it.grade }
                    .thenBy { it.section }
                    .thenBy { it.subject }
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error getting available classes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Downloads students and creates packages ONLY for the classes specifically selected by the teacher.
     */
    suspend fun downloadSelectedClassesRoster(
        schoolId: String,
        selectedItems: List<SchoolClassSubjectItem>,
        providedUrl: String? = null,
        providedKey: String? = null
    ): Boolean {
        if (selectedItems.isEmpty()) return false
        return try {
            val (url, apiKey) = resolveCredentials(providedUrl, providedKey)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            val studentsResponse = try { 
                api.getStudents(apiKey, authHeader, schoolId, "eq.$schoolId") 
            } catch (e: Exception) { null }
            val studentsList = if (studentsResponse?.isSuccessful == true) {
                studentsResponse.body() ?: emptyList()
            } else emptyList()

            val existingStudents = studentDao.getAllStudentsList()

            // Clear old local packages and students so only selected classes are retained
            packageDao.clearAll()
            studentDao.clearAll()

            val maxStudentsPerSection = 100
            val collator = java.text.Collator.getInstance(java.util.Locale("ar")).apply {
                strength = java.text.Collator.PRIMARY
            }

            selectedItems.forEach { item ->
                val targetGradeStd = standardizeGradeName(item.grade)
                val targetSecStd = standardizeSectionName(item.section)
                val targetSubjStd = standardizeSubjectName(item.subject)

                packageDao.insertPackage(
                    ClassPackage(
                        grade = targetGradeStd,
                        section = targetSecStd,
                        subject = targetSubjStd,
                        iconName = "yrd"
                    )
                )

                val matchedStudents = studentsList.filter { stdDto ->
                    val g = stdDto.current_grade ?: ""
                    val s = stdDto.section ?: ""
                    isGradeMatch(g, targetGradeStd) &&
                    (standardizeSectionName(s) == targetSecStd || targetSecStd == "الكل" || (targetSecStd == "اللغة" && standardizeSectionName(s) == "أ"))
                }.sortedWith { s1, s2 ->
                    val n1 = s1.full_name?.trim()?.replace("^\\d+[\\.\\-\\s]+".toRegex(), "") ?: ""
                    val n2 = s2.full_name?.trim()?.replace("^\\d+[\\.\\-\\s]+".toRegex(), "") ?: ""
                    collator.compare(n1, n2)
                }.take(maxStudentsPerSection)

                matchedStudents.forEach { stdDto ->
                    val recNum = stdDto.record_number?.takeIf { it.isNotBlank() } ?: stdDto.full_name?.hashCode()?.toString() ?: "0"
                    val fName = stdDto.full_name?.trim()?.takeIf { it.isNotBlank() } ?: "طالب"
                    val newStudent = Student(
                        recordNumber = recNum,
                        fullName = fName,
                        grade = targetGradeStd,
                        section = targetSecStd,
                        subject = targetSubjStd,
                        historicalAbsences = stdDto.absences_count ?: 0
                    )

                    val matchedOld = existingStudents.find { oldStd ->
                        isGradeMatch(oldStd.grade, targetGradeStd) &&
                        standardizeSectionName(oldStd.section) == targetSecStd &&
                        standardizeSubjectName(oldStd.subject) == targetSubjStd &&
                        ((oldStd.recordNumber.isNotBlank() && oldStd.recordNumber == recNum) ||
                         (normalizeArabic(oldStd.fullName) == normalizeArabic(fName)))
                    }

                    if (matchedOld != null) {
                        studentDao.insertStudent(newStudent.copy(marks = matchedOld.marks))
                    } else {
                        studentDao.insertStudent(newStudent)
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error downloading selected classes roster: ${e.message}", e)
            false
        }
    }


    /**
     * Uploads student grades and attendance records to Supabase tables.
     */
    suspend fun syncGradesAndAttendance(
        schoolId: String,
        teacherId: String? = null,
        targetGrade: String? = null,
        targetSection: String? = null,
        targetSubject: String? = null
    ): Boolean {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val cleanSchoolId = schoolId.trim().ifEmpty { "school_01" }

            // Ensure school header is active
            try {
                api.getSchools(apiKey, authHeader, cleanSchoolId, idFilter = "eq.$cleanSchoolId")
            } catch (e: Exception) {
                // Non-blocking
            }

            // Fetch target students (specific class with subject fallback)
            val allStudents = if (!targetGrade.isNullOrBlank() && !targetSection.isNullOrBlank()) {
                var classList = if (!targetSubject.isNullOrBlank()) {
                    studentDao.getStudentsListForClass(targetGrade.trim(), targetSection.trim(), targetSubject.trim())
                } else emptyList()
                if (classList.isEmpty()) {
                    classList = studentDao.getStudentsForGradeAndSection(targetGrade.trim(), targetSection.trim())
                }
                if (classList.isNotEmpty()) classList else studentDao.getAllStudentsList()
            } else {
                studentDao.getAllStudentsList()
            }

            val allAbsences = absenceDao.getAllAbsences().first()

            val rawGradesPayload = mutableListOf<SupabaseGradeDto>()
            val rawAttendancePayload = mutableListOf<SupabaseAttendanceDto>()

            allStudents.forEach { student ->
                val cleanRec = if (student.recordNumber.isNotBlank()) student.recordNumber.trim() else "std_${student.id}"
                val cleanSubj = standardizeSubjectName(student.subject.trim().ifBlank { targetSubject?.trim() ?: "المادة العامة" })
                val cleanGrd = standardizeGradeName(student.grade.trim().ifBlank { targetGrade?.trim() ?: "الأول" })
                val cleanSec = standardizeSectionName(student.section.trim().ifBlank { targetSection?.trim() ?: "أ" })

                // Map marks to DTO
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

                rawGradesPayload.add(
                    SupabaseGradeDto(
                        school_id = cleanSchoolId,
                        student_record_number = cleanRec,
                        subject = cleanSubj,
                        grade = cleanGrd,
                        section = cleanSec,
                        marks = marksDto,
                        teacher_id = null // Always null to avoid FK violation 23503 on teachers(id)
                    )
                )

                // Map absences
                val studentAbsences = allAbsences.filter { it.studentId == student.id }
                studentAbsences.forEach { abs ->
                    rawAttendancePayload.add(
                        SupabaseAttendanceDto(
                            school_id = cleanSchoolId,
                            student_record_number = cleanRec,
                            date = abs.dateString,
                            status = "absent",
                            date_string = abs.dateString,
                            subject = cleanSubj,
                            period_number = 1
                        )
                    )
                }
            }

            // CRITICAL: Deduplicate batch payload to prevent duplicates
            val gradesPayload = rawGradesPayload.distinctBy { "${it.school_id}__${it.student_record_number}__${it.subject}" }
            val attendancePayload = rawAttendancePayload.distinctBy { "${it.school_id}__${it.student_record_number}__${it.date_string ?: it.date}__${it.subject ?: ""}" }

            // 0. Ensure all students in this class are synced to cloud students table (including teacher-added students)
            val studentsPayload = allStudents.map { student ->
                val cleanRec = if (student.recordNumber.isNotBlank()) student.recordNumber.trim() else "std_${student.id}"
                val cleanGrd = standardizeGradeName(student.grade.trim().ifBlank { targetGrade?.trim() ?: "الأول" })
                val cleanSec = standardizeSectionName(student.section.trim().ifBlank { targetSection?.trim() ?: "أ" })
                val parts = student.fullName.trim().split("\\s+".toRegex())
                SupabaseStudentDto(
                    school_id = cleanSchoolId,
                    record_number = cleanRec,
                    first_name = parts.getOrNull(0) ?: student.fullName,
                    second_name = parts.getOrNull(1),
                    third_name = parts.getOrNull(2),
                    fourth_name = if (parts.size > 3) parts.drop(3).joinToString(" ") else null,
                    title_name = null,
                    full_name = student.fullName.trim(),
                    current_grade = cleanGrd,
                    section = cleanSec,
                    absences_count = student.historicalAbsences
                )
            }.distinctBy { "${it.school_id}__${it.record_number}" }

            if (studentsPayload.isNotEmpty()) {
                try {
                    api.upsertStudents(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        students = studentsPayload
                    )
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Upsert students warning: ${e.message}")
                }
            }

            // 1. Delete prior grades for subject(s) then insert new
            val uniqueSubjects = gradesPayload.map { it.subject }.distinct()
            for (subj in uniqueSubjects) {
                try {
                    api.deleteGrades(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        schoolFilter = "eq.$cleanSchoolId",
                        subjectFilter = "eq.$subj"
                    )
                } catch (e: Exception) {
                    Log.w("SyncRepository", "Delete prior grades warning: ${e.message}")
                }
            }

            if (gradesPayload.isNotEmpty()) {
                val chunkSize = 50
                for (chunk in gradesPayload.chunked(chunkSize)) {
                    val gradesResp = api.insertGrades(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        grades = chunk
                    )
                    if (!gradesResp.isSuccessful) {
                        val errBody = gradesResp.errorBody()?.string() ?: ""
                        Log.e("SyncRepository", "Insert Grades failed code: ${gradesResp.code()}, body: $errBody")
                        return false
                    }
                }
            }

            // 2. Insert attendance
            if (attendancePayload.isNotEmpty()) {
                try {
                    api.deleteAttendance(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        schoolFilter = "eq.$cleanSchoolId"
                    )
                } catch (e: Exception) {
                    // Non-blocking
                }

                val chunkSize = 50
                for (chunk in attendancePayload.chunked(chunkSize)) {
                    val attResp = api.insertAttendance(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        attendance = chunk
                    )
                    if (!attResp.isSuccessful) {
                        val errBody = attResp.errorBody()?.string() ?: ""
                        Log.e("SyncRepository", "Insert Attendance failed code: ${attResp.code()}, body: $errBody")
                    }
                }
            }

            // 3. Register/Update teacher active presence in join_requests in Supabase
            try {
                val currentConfig = configDao.getConfig().first()
                val finalTeacherName = (currentConfig?.managerName?.takeIf { it.isNotBlank() } 
                    ?: teacherId 
                    ?: "المدرس").trim()

                api.insertJoinRequest(
                    apiKey = apiKey,
                    auth = authHeader,
                    schoolId = cleanSchoolId,
                    request = SupabaseJoinRequestDto(
                        school_id = cleanSchoolId,
                        role = "teacher",
                        full_name = finalTeacherName,
                        class_name = targetGrade?.ifBlank { null } ?: "الأول",
                        section = targetSection?.ifBlank { null } ?: "أ",
                        subject_name = targetSubject?.ifBlank { null } ?: "المادة العامة",
                        status = "approved",
                        pairing_code_attempt = currentConfig?.pairingCode
                    )
                )
                Log.d("SyncRepository", "Teacher presence updated in join_requests for $finalTeacherName")
            } catch (e: Exception) {
                Log.w("SyncRepository", "Non-blocking: could not update presence in join_requests: ${e.message}")
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SyncRepository", "Exception in syncGradesAndAttendance: ${e.message}")
            false
        }
    }

    suspend fun fetchStudentDashboardData(
        schoolId: String,
        fullName: String,
        grade: String,
        section: String,
        providedUrl: String? = null,
        providedKey: String? = null
    ): StudentDashboardData? {
        return try {
            val (url, apiKey) = resolveCredentials(providedUrl, providedKey)
            val authHeader = "Bearer $apiKey"
            val api = getApi(url)

            val studentResp = api.getStudentByName(apiKey, authHeader, schoolId, "eq.$schoolId", "eq.$fullName", "eq.$grade", "eq.$section")
            if (!studentResp.isSuccessful || studentResp.body().isNullOrEmpty()) return null
            val student = studentResp.body()!!.first()

            val gradesResp = api.getStudentGrades(apiKey, authHeader, schoolId, "eq.$schoolId", "eq.${student.record_number}")
            val grades = if (gradesResp.isSuccessful) gradesResp.body() ?: emptyList() else emptyList()

            val attendanceResp = api.getStudentAttendance(apiKey, authHeader, schoolId, "eq.$schoolId", "eq.${student.record_number}")
            val attendance = if (attendanceResp.isSuccessful) attendanceResp.body() ?: emptyList() else emptyList()

            StudentDashboardData(
                student = student,
                grades = grades,
                attendance = attendance
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun pairSchoolByCode(
        pairingCode: String,
        teacherName: String
    ): PairingResult {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val cleanCode = pairingCode.trim().ifEmpty { "112233" }

            // 1. Try search by pairing_code
            var matchedSchool: SupabaseSchoolDto? = null
            try {
                val res = api.getSchools(apiKey = apiKey, auth = authHeader, pairingCodeFilter = "eq.$cleanCode")
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    matchedSchool = res.body()!!.first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try search by id
            if (matchedSchool == null) {
                try {
                    val res = api.getSchools(apiKey = apiKey, auth = authHeader, idFilter = "eq.$cleanCode")
                    if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        matchedSchool = res.body()!!.first()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. Fallback: get first available school if cleanCode was default
            if (matchedSchool == null && (cleanCode == "112233" || cleanCode.isEmpty())) {
                try {
                    val res = api.getSchools(apiKey = apiKey, auth = authHeader)
                    if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        matchedSchool = res.body()!!.first()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (matchedSchool != null) {
                verifySchoolAndTeacher(
                    schoolId = matchedSchool.id,
                    teacherInput = teacherName,
                    pairingCode = matchedSchool.pairing_code,
                    providedUrl = url,
                    providedKey = apiKey
                )
            } else {
                PairingResult(
                    success = false,
                    warning = false,
                    message = "لم يتم العثور على مدرسة برمز الاقتران ($cleanCode). يرجى التأكد من الرمز من شاشة المدير."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PairingResult(
                success = false,
                warning = false,
                message = "فشل الاتصال بالسحابة: ${e.localizedMessage ?: "تأكد من اتصال الإنترنت"}"
            )
        }
    }

    fun matchGradeFlexible(cloudGrade: String?, targetGrade: String): Boolean {
        if (cloudGrade.isNullOrBlank() || targetGrade.isBlank()) return false
        val c1 = standardizeGradeName(cloudGrade)
        val c2 = standardizeGradeName(targetGrade)
        if (c1.isNotBlank() && c2.isNotBlank() && c1 == c2) return true

        val digits1 = cloudGrade.filter { it.isDigit() }
        val digits2 = targetGrade.filter { it.isDigit() }
        if (digits1.isNotEmpty() && digits2.isNotEmpty()) {
            return digits1 == digits2
        }

        val normCloud = normalizeArabic(cloudGrade)
        val normTarget = normalizeArabic(targetGrade)
        if (normCloud.isBlank() || normTarget.isBlank()) return false

        return normCloud == normTarget || normCloud.contains(normTarget) || normTarget.contains(normCloud)
    }

    fun matchSectionFlexible(cloudSection: String?, targetSection: String): Boolean {
        if (cloudSection.isNullOrBlank() || targetSection.isBlank()) return false
        val s1 = standardizeSectionName(cloudSection)
        val s2 = standardizeSectionName(targetSection)
        if (s1.isNotBlank() && s2.isNotBlank() && s1 == s2) return true

        val normCloud = normalizeArabic(cloudSection)
        val normTarget = normalizeArabic(targetSection)
        if (normCloud.isBlank() || normTarget.isBlank()) return false

        return normCloud == normTarget
    }

    suspend fun downloadSimpleRoster(
        schoolId: String,
        grade: String,
        section: String
    ): List<SupabaseStudentDto> {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val cleanSchoolId = schoolId.trim().ifEmpty { "SCH-KAB2-6884" }
            val stdGrade = standardizeGradeName(grade)
            val stdSection = standardizeSectionName(section)

            // Stage 1: Try strict school_id + grade + section
            var list = try {
                val response = api.getStudents(
                    apiKey = apiKey,
                    auth = authHeader,
                    schoolId = cleanSchoolId,
                    schoolFilter = "eq.$cleanSchoolId",
                    gradeFilter = "eq.$stdGrade",
                    sectionFilter = "eq.$stdSection"
                )
                if (response.isSuccessful && !response.body().isNullOrEmpty()) response.body()!! else emptyList()
            } catch (e: Exception) { emptyList() }

            // Stage 2: If empty, fetch all students for school_id and match locally with flexible grade & section
            if (list.isEmpty()) {
                list = try {
                    val allResponse = api.getStudents(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        schoolFilter = "eq.$cleanSchoolId"
                    )
                    if (allResponse.isSuccessful && !allResponse.body().isNullOrEmpty()) {
                        allResponse.body()!!.filter {
                            matchGradeFlexible(it.current_grade, grade) &&
                            matchSectionFlexible(it.section, section)
                        }
                    } else emptyList()
                } catch (e: Exception) { emptyList() }
            }

            // Stage 3: Robust Fallback - fetch all school students without strict school_id constraint and match locally
            if (list.isEmpty()) {
                list = try {
                    val fallbackRes = api.getStudents(
                        apiKey = apiKey,
                        auth = authHeader,
                        schoolId = cleanSchoolId,
                        schoolFilter = "neq.__none__"
                    )
                    if (fallbackRes.isSuccessful && !fallbackRes.body().isNullOrEmpty()) {
                        fallbackRes.body()!!.filter {
                            matchGradeFlexible(it.current_grade, grade) &&
                            matchSectionFlexible(it.section, section)
                        }
                    } else emptyList()
                } catch (e: Exception) { emptyList() }
            }

            val collator = java.text.Collator.getInstance(java.util.Locale("ar")).apply {
                strength = java.text.Collator.PRIMARY
            }
            list.sortedWith { s1, s2 ->
                val n1 = s1.full_name?.trim()?.replace("^\\d+[\\.\\-\\s]+".toRegex(), "") ?: ""
                val n2 = s2.full_name?.trim()?.replace("^\\d+[\\.\\-\\s]+".toRegex(), "") ?: ""
                collator.compare(n1, n2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun downloadSchedule(context: android.content.Context, schoolId: String): Boolean {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val authHeader = "Bearer $apiKey"
            val cleanSchoolId = schoolId.trim().ifEmpty { "SCH-KAB2-6884" }

            val isLocal = url.contains("localhost") || url.contains("192.168.") || !url.contains("supabase")
            val scheduleJson = if (isLocal) {
                val retrofitLocal = retrofit2.Retrofit.Builder()
                    .baseUrl(if (url.endsWith("/")) url else "$url/")
                    .client(okhttp3.OkHttpClient())
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                val apiLocal = retrofitLocal.create(DiyalaSchoolApi::class.java)
                val resp = apiLocal.downloadSchedule(cleanSchoolId)
                if (resp.isSuccessful && resp.body() != null) {
                    val gson = com.google.gson.Gson()
                    gson.toJson(resp.body()!!.schedule)
                } else {
                    null
                }
            } else {
                val api = getApi(url)
                val orQuery = "(id.eq.$cleanSchoolId,school_id.eq.$cleanSchoolId)"
                val resp = api.getSchedules(apiKey, authHeader, cleanSchoolId, orFilter = orQuery)
                if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                    val matched = resp.body()!!.first()
                    val gson = com.google.gson.Gson()
                    gson.toJson(matched.schedule_map)
                } else {
                    val resp2 = api.getSchedules(apiKey, authHeader, cleanSchoolId, idFilter = "eq.$cleanSchoolId")
                    if (resp2.isSuccessful && !resp2.body().isNullOrEmpty()) {
                        val matched = resp2.body()!!.first()
                        val gson = com.google.gson.Gson()
                        gson.toJson(matched.schedule_map)
                    } else {
                        null
                    }
                }
            }

            if (scheduleJson != null) {
                val prefs = context.getSharedPreferences("diyala_school_prefs", android.content.Context.MODE_PRIVATE)
                val editor = prefs.edit().putString("synced_schedule", scheduleJson)
                try {
                    val root = com.google.gson.Gson().fromJson<Map<String, Any>>(scheduleJson, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
                    val timingObj = root?.get("_timing") as? Map<*, *>
                    if (timingObj != null) {
                        val startHourStr = timingObj["schoolStartHour"]?.toString() ?: ""
                        val lessonDur = (timingObj["lessonDurationMinutes"] as? Number)?.toInt() ?: 40
                        val breakDur = (timingObj["breakDurationMinutes"] as? Number)?.toInt() ?: 10

                        editor.putString("school_start_hour", startHourStr)
                        editor.putInt("lesson_duration_minutes", lessonDur)
                        editor.putInt("break_duration_minutes", breakDur)

                        if (startHourStr.contains(":")) {
                            val p = startHourStr.split(":")
                            p.getOrNull(0)?.toIntOrNull()?.let { editor.putInt("bell_start_hour", it) }
                            p.getOrNull(1)?.toIntOrNull()?.let { editor.putInt("bell_start_minute", it) }
                        } else if (startHourStr.toIntOrNull() != null) {
                            editor.putInt("bell_start_hour", startHourStr.toInt())
                        }
                        editor.putInt("bell_lesson_duration", lessonDur)
                        editor.putInt("bell_break_duration", breakDur)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                editor.apply()

                try {
                    val widgetIntent1 = Intent(context, DailyScheduleWidgetProvider::class.java).apply {
                        action = DailyScheduleWidgetProvider.ACTION_REFRESH_WIDGET
                    }
                    context.sendBroadcast(widgetIntent1)

                    val widgetIntent2 = Intent(context, FullScheduleWidgetProvider::class.java).apply {
                        action = FullScheduleWidgetProvider.ACTION_REFRESH_WIDGET
                    }
                    context.sendBroadcast(widgetIntent2)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun publishDailyAssignment(assignment: SupabaseDailyAssignmentDto): Boolean {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val cleanSchoolId = assignment.school_id.trim().ifEmpty { "SCH-KAB2-6884" }
            val cleanTeacherId = assignment.teacher_id.trim().ifEmpty { "teacher_01" }

            val cleanAssignment = assignment.copy(
                school_id = cleanSchoolId,
                teacher_id = cleanTeacherId
            )

            // Try return=minimal first (standard HTTP 201 Created)
            val fallbackResp = api.insertDailyAssignmentSimple(apiKey, authHeader, cleanSchoolId, cleanAssignment)
            if (fallbackResp.isSuccessful) {
                Log.d("SyncRepository", "Daily assignment inserted successfully")
                true
            } else {
                val err = fallbackResp.errorBody()?.string()
                Log.w("SyncRepository", "Insert assignment minimal failed: ${fallbackResp.code()} - $err")
                if (fallbackResp.code() == 404) {
                    Log.e("SyncRepository", "CRITICAL: 'daily_assignments' table does not exist in Supabase! Please execute setup_daily_assignments_and_chat.sql in Supabase SQL Editor.")
                }
                val resp = api.insertDailyAssignment(apiKey, authHeader, cleanSchoolId, cleanAssignment)
                if (resp.isSuccessful) {
                    true
                } else {
                    Log.w("SyncRepository", "Insert assignment representation failed: ${resp.code()} - ${resp.errorBody()?.string()}")
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getDailyAssignments(schoolId: String, teacherId: String? = null): List<SupabaseDailyAssignmentDto> {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val resp = api.getDailyAssignments(
                apiKey = apiKey,
                auth = authHeader,
                schoolId = schoolId,
                schoolFilter = "eq.$schoolId",
                teacherFilter = if (!teacherId.isNullOrEmpty()) "eq.$teacherId" else null
            )
            if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendDirectMessage(message: SupabaseDirectMessageDto): Boolean {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val resp = api.sendDirectMessage(apiKey, authHeader, message.school_id, message)
            resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getDirectMessages(schoolId: String, teacherId: String): List<SupabaseDirectMessageDto> {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val orFilter = "(sender_id.eq.$teacherId,receiver_id.eq.$teacherId)"
            val resp = api.getDirectMessages(apiKey, authHeader, schoolId, "eq.$schoolId", orFilter)
            if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val _directives = kotlinx.coroutines.flow.MutableStateFlow<List<SupabaseDirectiveDto>>(emptyList())
    val directives: kotlinx.coroutines.flow.StateFlow<List<SupabaseDirectiveDto>> = _directives

    suspend fun fetchAndNotifyDirectives(targetSchoolId: String? = null): List<SupabaseDirectiveDto> {
        return try {
            val conf = configDao.getConfig().first() ?: return emptyList()
            val schoolId = targetSchoolId?.ifEmpty { conf.schoolId } ?: conf.schoolId
            if (schoolId.isEmpty() || schoolId == "school_01") return emptyList()

            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            val resp = api.getDirectives(
                apiKey = apiKey,
                auth = authHeader,
                schoolId = schoolId,
                schoolFilter = "eq.$schoolId"
            )

            if (resp.isSuccessful) {
                val list = resp.body()?.filter { it.is_active } ?: emptyList()
                _directives.value = list

                val prefs = context.getSharedPreferences("teacher_directives_prefs", Context.MODE_PRIVATE)
                val seenIds = prefs.getStringSet("seen_directive_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

                val newDirectives = list.filter { it.id.isNotEmpty() && !seenIds.contains(it.id) }
                if (newDirectives.isNotEmpty()) {
                    for (dir in newDirectives) {
                        TeacherNotificationHelper.showBroadcastNotification(
                            context = context,
                            title = dir.title,
                            message = dir.content
                        )
                        seenIds.add(dir.id)
                    }
                    prefs.edit().putStringSet("seen_directive_ids", seenIds).apply()
                }
                list
            } else {
                Log.w("SyncRepository", "fetchDirectives returned ${resp.code()}: ${resp.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendSupervisorDirective(
        title: String,
        content: String,
        targetRole: String = "teacher"
    ): Result<Boolean> {
        return try {
            val conf = configDao.getConfig().first() ?: return Result.failure(Exception("لم يتم العثور على إعدادات المدرسة"))
            val schoolId = conf.schoolId
            if (schoolId.isEmpty() || schoolId == "school_01") {
                return Result.failure(Exception("معرّف المدرسة غير صالح أو غير مقترن بالسحابة"))
            }

            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            val body = mapOf<String, Any>(
                "school_id" to schoolId,
                "title" to title.trim(),
                "content" to content.trim(),
                "target_role" to targetRole,
                "is_active" to true
            )

            val resp = api.sendDirective(
                apiKey = apiKey,
                auth = authHeader,
                schoolId = schoolId,
                directive = body
            )

            if (resp.isSuccessful) {
                // Refresh local directives cache immediately
                fetchAndNotifyDirectives(schoolId)
                Result.success(true)
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "كود الخطأ: ${resp.code()}"
                Log.e("SyncRepository", "Failed to broadcast directive: $errorMsg")
                Result.failure(Exception("فشل إرسال التوجيه: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Exception broadcasting directive", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSupervisorDirective(directiveId: String): Result<Boolean> {
        return try {
            val conf = configDao.getConfig().first() ?: return Result.failure(Exception("لم يتم العثور على إعدادات المدرسة"))
            val schoolId = conf.schoolId
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"

            val resp = api.deleteDirective(
                apiKey = apiKey,
                auth = authHeader,
                schoolId = schoolId,
                idFilter = "eq.$directiveId"
            )

            if (resp.isSuccessful) {
                fetchAndNotifyDirectives(schoolId)
                Result.success(true)
            } else {
                val errorMsg = resp.errorBody()?.string() ?: "كود الخطأ: ${resp.code()}"
                Result.failure(Exception("فشل حذف التوجيه: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StudentDashboardData(
    val student: SupabaseStudentDto,
    val grades: List<SupabaseGradeDto>,
    val attendance: List<SupabaseAttendanceDto>
)
