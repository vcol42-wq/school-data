package com.school.system.data

import android.util.Log
import com.school.system.data.dao.*
import com.school.system.data.model.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    val school_id: String,
    val name: String,
    val email: String?,
    val specialization: String?
)

data class SupabaseAssignmentDto(
    val id: String,
    val school_id: String,
    val teacher_id: String,
    val class_name: String,
    val section: String,
    val subject_name: String
)

data class SupabaseStudentDto(
    val school_id: String,
    val record_number: String,
    val first_name: String,
    val second_name: String?,
    val third_name: String?,
    val fourth_name: String?,
    val title_name: String?,
    val full_name: String,
    val current_grade: String,
    val section: String,
    val absences_count: Int
)

data class SupabaseGradeDto(
    val school_id: String,
    val student_record_number: String,
    val subject: String,
    val grade: String,
    val section: String,
    val marks: StudentMarksDto, // Maps directly to PostgreSQL jsonb column via Gson
    val teacher_id: String? = null
)

data class SupabaseAttendanceDto(
    val school_id: String,
    val student_record_number: String,
    val date: String,
    val status: String
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
    val admin_email: String
)

data class SupabaseScheduleDto(
    val id: String,
    val schedule_map: Map<String, Any>?
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
        @Query("teacher_id") teacherFilter: String
    ): Response<List<SupabaseAssignmentDto>>

    @GET("rest/v1/students")
    suspend fun getStudents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("current_grade") gradeFilter: String? = null,
        @Query("section") sectionFilter: String? = null
    ): Response<List<SupabaseStudentDto>>

    @DELETE("rest/v1/grades")
    suspend fun deleteGrades(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolId: String,
        @Query("school_id") schoolFilter: String,
        @Query("subject") subjectFilter: String? = null
    ): Response<Void>

    @POST("rest/v1/grades")
    @Headers("Prefer: resolution=merge-duplicates")
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
    @Headers("Prefer: resolution=merge-duplicates")
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
}

@Singleton
class SyncRepository @Inject constructor(
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

    private fun getApi(url: String): SupabaseApi {
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        val baseUrl = if (formattedUrl.endsWith("/")) formattedUrl else "$formattedUrl/"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
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
     * Standardizes Subject Name considering all common aliases and variations:
     * - اللغة الإنكليزية: اللغة الانجليزية / اللغة الإنكليزية / الانكليزية / الانجليزية / انكليزي / انجليزي / English / E / Eng / EN / EL
     * - الأحياء: علم الأحياء / علوم الأحياء / احياء / الاحياء
     * - التربية الإسلامية: اسلامية / الاسلامية / التربية الاسلامية / دين / قرآن
     * - اللغة العربية: عربي / العربي / لغة عربية / اللغة العربية
     * - لغة أخرى: كردي / تركماني / فرنسي / الماني / اسباني / سرياني / لغة...
     * - فيزياء / كيمياء / اجتماعيات / رياضيات / أخرى
     */
    fun standardizeSubjectName(raw: String?): String {
        if (raw.isNullOrBlank()) return "المادة العامة"
        val s = raw.trim()
        val norm = normalizeArabic(s).lowercase()
        val rawLower = s.lowercase().replace("[^a-z0-9\u0600-\u06FF]".toRegex(), "")

        // 1. اللغة الإنكليزية: جميع التسميات (عربي، إنجليزي، انكليزي، أحرف لاتينية)
        if (norm.contains("انكل") || norm.contains("انجل") ||
            rawLower.contains("engl") || rawLower.startsWith("eng") || rawLower.endsWith("eng") ||
            rawLower == "e" || rawLower == "en" || rawLower == "el" ||
            rawLower.contains("english") || norm.contains("انكلش") || norm.contains("انجلش") ||
            norm.contains("انجليز") || norm.contains("انكليز")
        ) {
            return "اللغة الإنكليزية"
        }

        // 2. الأحياء: علم الأحياء / علوم الأحياء / احياء / الأحياء
        if (norm.contains("احياء") || norm.contains("علماحياء") || norm.contains("علوماحياء")) return "الأحياء"

        // 3. التربية الإسلامية: إسلامية / الاسلامية / دين / قرآن
        if (norm.contains("اسلام") || norm.contains("قران") || norm.contains("دين")) return "التربية الإسلامية"

        // 4. اللغة العربية: عربي / العربي
        if (norm.contains("عرب")) return "اللغة العربية"

        // 5. باقي المواد
        if (norm.contains("فيزيا")) return "الفيزياء"
        if (norm.contains("كيميا")) return "الكيمياء"
        if (norm.contains("اجتماع") || norm.contains("تاريخ") || norm.contains("جغرافي") || norm.contains("وطني")) return "الاجتماعيات"
        if (norm.contains("رياض")) return "الرياضيات"

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

        return if (branch.isNotEmpty()) "$base $branch" else base
    }

    /**
     * Standardizes Section (أ، ب، ج، ح، خ، أخرى)
     */
    fun standardizeSectionName(secStr: String?): String {
        if (secStr.isNullOrBlank()) return "أ"
        val clean = secStr.trim().replace("^(شعبة|الشعبة|ش)\\s*".toRegex(), "").trim()
        val lower = clean.lowercase()
        if (clean == "ا" || clean == "أ" || clean == "إ" || clean == "آ" || lower == "a" || lower == "1" || lower == "١") return "أ"
        if (clean == "ب" || lower == "b" || lower == "2" || lower == "٢") return "ب"
        if (clean == "ج" || lower == "c" || lower == "3" || lower == "٣") return "ج"
        if (clean == "ح") return "ح"
        if (clean == "خ") return "خ"
        if (clean == "د" || lower == "d" || lower == "4" || lower == "٤") return "د"
        if (clean == "ه" || clean == "هـ" || lower == "e" || lower == "5" || lower == "٥") return "هـ"
        if (clean == "و" || lower == "f" || lower == "6" || lower == "٦") return "و"
        return clean.ifBlank { "أ" }
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

            if (cleanInput.isNotBlank()) {
                // Query all teachers of this school to perform fuzzy and normalized match
                val allTeachersRes = api.getAllTeachers(
                    apiKey = apiKey,
                    auth = authHeader,
                    schoolId = schoolId,
                    schoolFilter = "eq.$schoolId"
                )

                if (allTeachersRes.isSuccessful && !allTeachersRes.body().isNullOrEmpty()) {
                    val allTeachers = allTeachersRes.body()!!
                    val normInput = normalizeArabic(cleanInput.replace("^(أ\\.|أستاذ\\s*)\\s*".toRegex(), "").trim())
                    
                    matchedTeacher = allTeachers.find { t ->
                        val normTeacherName = normalizeArabic(t.name.replace("^(أ\\.|أستاذ\\s*)\\s*".toRegex(), "").trim())
                        t.id == cleanInput ||
                        normTeacherName == normInput ||
                        (normInput.length >= 3 && normTeacherName.contains(normInput)) ||
                        (normTeacherName.length >= 3 && normInput.contains(normTeacherName))
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

                // Download Roster exclusively for this registered teacher
                val rosterSuccess = downloadRoster(schoolId, matchedTeacher.id, url, apiKey)
                if (rosterSuccess) {
                    PairingResult(success = true, warning = false, message = "تم التحقق من الأستاذ (${matchedTeacher.name}) وتنزيل الشعب والمواد الموكلة له بنجاح!")
                } else {
                    PairingResult(success = true, warning = true, message = "تم ربط حسابك (${matchedTeacher.name}) بنجاح. لم يتم العثور على حصص مخصصة لك في جدول الإدارة حتى الآن.")
                }
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

            // 1. Fetch assignments exclusively for THIS teacher
            val assignmentsResponse = try { 
                api.getTeacherAssignments(apiKey, authHeader, schoolId, "eq.$teacherId") 
            } catch (e: Exception) { null }
            
            val assignments = if (assignmentsResponse?.isSuccessful == true) {
                assignmentsResponse.body() ?: emptyList()
            } else emptyList()

            if (assignments.isEmpty()) {
                // Teacher is registered but has no assigned classes in timetable
                packageDao.clearAll()
                studentDao.clearAll()
                return false
            }

            val studentsResponse = try { 
                api.getStudents(apiKey, authHeader, schoolId, "eq.$schoolId") 
            } catch (e: Exception) { null }
            val studentsList = if (studentsResponse?.isSuccessful == true) {
                studentsResponse.body() ?: emptyList()
            } else emptyList()

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

            // 3. Insert students strictly for this teacher's assigned classes & subjects (Cap: 60 students per section)
            val maxStudentsPerSection = 60
            val collator = java.text.Collator.getInstance(java.util.Locale("ar")).apply {
                strength = java.text.Collator.PRIMARY
            }

            assignments.forEach { assign ->
                val targetGradeStd = standardizeGradeName(assign.class_name)
                val targetSecStd = standardizeSectionName(assign.section)
                val targetSubjStd = standardizeSubjectName(assign.subject_name)

                val matchedStudents = studentsList.filter { stdDto ->
                    standardizeGradeName(stdDto.current_grade) == targetGradeStd &&
                    standardizeSectionName(stdDto.section) == targetSecStd
                }.sortedWith { s1, s2 ->
                    val n1 = s1.full_name.trim().replace("^\\d+[\\.\\-\\s]+".toRegex(), "")
                    val n2 = s2.full_name.trim().replace("^\\d+[\\.\\-\\s]+".toRegex(), "")
                    collator.compare(n1, n2)
                }.take(maxStudentsPerSection)

                matchedStudents.forEach { stdDto ->
                    val newStudent = Student(
                        recordNumber = stdDto.record_number,
                        fullName = stdDto.full_name,
                        grade = targetGradeStd,
                        section = targetSecStd,
                        subject = targetSubjStd,
                        historicalAbsences = stdDto.absences_count
                    )

                    val matchedOld = existingStudents.find { oldStd ->
                        standardizeGradeName(oldStd.grade) == targetGradeStd &&
                        standardizeSectionName(oldStd.section) == targetSecStd &&
                        standardizeSubjectName(oldStd.subject) == targetSubjStd &&
                        ((oldStd.recordNumber.isNotBlank() && oldStd.recordNumber == stdDto.record_number) ||
                         (normalizeArabic(oldStd.fullName) == normalizeArabic(stdDto.full_name)))
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
            val cleanSchoolId = schoolId.trim().ifEmpty { "SCH-VCOL-6072" }

            // Ensure school header is active
            try {
                api.getSchools(apiKey, authHeader, cleanSchoolId, idFilter = "eq.$cleanSchoolId")
            } catch (e: Exception) {
                // Non-blocking
            }

            // Fetch target students (specific class or all)
            val allStudents = if (!targetGrade.isNullOrBlank() && !targetSection.isNullOrBlank() && !targetSubject.isNullOrBlank()) {
                val classList = studentDao.getStudentsListForClass(targetGrade.trim(), targetSection.trim(), targetSubject.trim())
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
                        teacher_id = teacherId
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
                            status = "absent"
                        )
                    )
                }
            }

            // CRITICAL: Deduplicate batch payload to prevent duplicates
            val gradesPayload = rawGradesPayload.distinctBy { "${it.school_id}__${it.student_record_number}__${it.subject}" }
            val attendancePayload = rawAttendancePayload.distinctBy { "${it.school_id}__${it.student_record_number}__${it.date}" }

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

    suspend fun downloadSimpleRoster(
        schoolId: String,
        grade: String,
        section: String
    ): List<SupabaseStudentDto> {
        return try {
            val (url, apiKey) = resolveCredentials(null, null)
            val api = getApi(url)
            val authHeader = "Bearer $apiKey"
            val stdGrade = standardizeGradeName(grade)
            val stdSection = standardizeSectionName(section)

            val response = api.getStudents(
                apiKey = apiKey,
                auth = authHeader,
                schoolId = schoolId,
                schoolFilter = "eq.$schoolId",
                gradeFilter = "eq.$stdGrade",
                sectionFilter = "eq.$stdSection"
            )

            val initialList = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
            val list = if (initialList.isNotEmpty()) {
                initialList
            } else {
                // Robust Fallback: fetch all school students and match with standardizeGradeName & standardizeSectionName
                val allResponse = api.getStudents(
                    apiKey = apiKey,
                    auth = authHeader,
                    schoolId = schoolId,
                    schoolFilter = "eq.$schoolId"
                )
                if (allResponse.isSuccessful && !allResponse.body().isNullOrEmpty()) {
                    allResponse.body()!!.filter {
                        standardizeGradeName(it.current_grade) == stdGrade &&
                        standardizeSectionName(it.section) == stdSection
                    }
                } else {
                    emptyList()
                }
            }

            val collator = java.text.Collator.getInstance(java.util.Locale("ar")).apply {
                strength = java.text.Collator.PRIMARY
            }
            list.sortedWith { s1, s2 ->
                val n1 = s1.full_name.trim().replace("^\\d+[\\.\\-\\s]+".toRegex(), "")
                val n2 = s2.full_name.trim().replace("^\\d+[\\.\\-\\s]+".toRegex(), "")
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
            val cleanSchoolId = schoolId.trim().ifEmpty { "SCH-VCOL-6072" }

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
                prefs.edit().putString("synced_schedule", scheduleJson).apply()
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
            val resp = api.insertDailyAssignment(apiKey, authHeader, assignment.school_id, assignment)
            resp.isSuccessful
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
}

data class StudentDashboardData(
    val student: SupabaseStudentDto,
    val grades: List<SupabaseGradeDto>,
    val attendance: List<SupabaseAttendanceDto>
)
