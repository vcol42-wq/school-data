package com.school.system.data

import com.school.system.data.dao.ConfigDao
import com.school.system.data.dao.StudentDao
import com.school.system.data.dao.ClassPackageDao
import com.school.system.data.dao.AbsenceDao
import com.school.system.data.model.SchoolConfig
import com.school.system.data.model.Student
import com.school.system.data.model.ClassPackage
import com.school.system.data.repository.SchoolRepository
import com.school.system.data.network.GeminiAssistantService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class SyncManager @Inject constructor(
    private val configDao: ConfigDao,
    private val studentDao: StudentDao,
    private val packageDao: ClassPackageDao,
    private val absenceDao: AbsenceDao,
    private val syncRepository: SyncRepository,
    private val schoolRepository: SchoolRepository,
    private val geminiAssistantService: GeminiAssistantService
) {
    companion object {
        const val DEFAULT_AI_GATEWAY = "https://theprinciple-ai.up.railway.app/" // Example global AI gateway
    }

    private fun getApi(baseUrl: String): DiyalaSchoolApi {
        var formattedUrl = baseUrl.trim()
        if (!formattedUrl.startsWith("http")) {
            formattedUrl = "https://$formattedUrl"
        }
        val url = if (formattedUrl.endsWith("/")) formattedUrl else "$formattedUrl/"
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiyalaSchoolApi::class.java)
    }

    suspend fun connectToCloud(url: String): Boolean {
        return try {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http")) {
                formattedUrl = "https://$formattedUrl"
            }
            
            val isSupabase = formattedUrl.contains("supabase.co")
            
            if (isSupabase) {
                val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                
                // Fetch Gemini Key from Supabase app_config
                val cloudGeminiKey = schoolRepository.fetchCloudGeminiKey()
                
                configDao.saveConfig(currentConfig.copy(
                    cloudUrl = formattedUrl,
                    cloudKey = currentConfig.cloudKey.ifEmpty { SyncRepository.DEFAULT_ANON_KEY },
                    cloudGeminiKey = cloudGeminiKey ?: ""
                ))
                true
            } else {
                val api = getApi(formattedUrl)
                val response = api.checkHealth()
                if (response.isSuccessful) {
                    val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                    configDao.saveConfig(currentConfig.copy(cloudUrl = formattedUrl))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun connectAndPairQr(qrContent: String): Boolean {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val trimmed = qrContent.trim()

            var url = SyncRepository.DEFAULT_SUPABASE_URL
            var apiKey = SyncRepository.DEFAULT_ANON_KEY
            var schoolId = currentConfig.schoolId.ifEmpty { "school_01" }
            var pairingCode = currentConfig.pairingCode
            var teacherName = ""

            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val gson = com.google.gson.Gson()
                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(trimmed, mapType)

                url = data["url"]?.toString() ?: data["supabaseUrl"]?.toString() ?: data["cloudUrl"]?.toString() ?: SyncRepository.DEFAULT_SUPABASE_URL
                apiKey = data["apiKey"]?.toString() ?: data["cloudKey"]?.toString() ?: data["api_key"]?.toString() ?: data["anonKey"]?.toString() ?: SyncRepository.DEFAULT_ANON_KEY
                schoolId = data["schoolId"]?.toString() ?: data["school_id"]?.toString() ?: data["id"]?.toString() ?: schoolId
                pairingCode = data["pairingCode"]?.toString() ?: data["pairing_code"]?.toString() ?: pairingCode
                teacherName = data["teacherName"]?.toString() ?: data["teacher_name"]?.toString() ?: data["name"]?.toString() ?: ""
            } else if (trimmed.startsWith("OTP:", ignoreCase = true)) {
                val parts = trimmed.split(":")
                pairingCode = parts.getOrNull(1) ?: pairingCode
                teacherName = parts.getOrNull(2) ?: ""
            } else if (trimmed.startsWith("TEACHER:", ignoreCase = true)) {
                val parts = trimmed.split(":")
                if (parts.size >= 4) {
                    pairingCode = parts[1].trim()
                    schoolId = parts[2].trim()
                    teacherName = parts[3].trim()
                } else if (parts.size == 3) {
                    pairingCode = parts[1].trim()
                    if (parts[2].startsWith("SCH-", ignoreCase = true)) {
                        schoolId = parts[2].trim()
                    } else {
                        teacherName = parts[2].trim()
                    }
                } else if (parts.size == 2) {
                    pairingCode = parts[1].trim()
                }
            } else if (trimmed.startsWith("SUPERVISOR:", ignoreCase = true)) {
                val parts = trimmed.split(":")
                pairingCode = parts.getOrNull(1)?.trim() ?: pairingCode
                val secondPart = parts.getOrNull(2)?.trim()
                if (secondPart != null && secondPart.startsWith("SCH-", ignoreCase = true)) {
                    schoolId = secondPart
                }
                teacherName = "المشرف العام"
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                url = trimmed
            } else if (trimmed.length in 4..12 && trimmed.all { it.isDigit() || it.isLetter() || it == '-' }) {
                pairingCode = trimmed
            }

            // Save basic settings to config
            configDao.saveConfig(
                currentConfig.copy(
                    cloudUrl = url,
                    cloudKey = apiKey,
                    schoolId = schoolId,
                    pairingCode = pairingCode,
                    isVerified = false
                )
            )

            // Auto-verify with school in background
            val pairRes = requestPairing(
                teacherName = teacherName,
                grade = "",
                section = "",
                subject = "",
                pairingCode = pairingCode
            )

            pairRes.success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun requestPairing(teacherName: String, grade: String, section: String, subject: String, pairingCode: String): PairingResult {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = currentConfig.schoolId.ifEmpty { "school_01" }
        return syncRepository.verifySchoolAndTeacher(
            schoolId = schoolId,
            teacherInput = teacherName,
            pairingCode = pairingCode,
            providedUrl = currentConfig.cloudUrl,
            providedKey = currentConfig.cloudKey
        )
    }

    suspend fun downloadClassRoster(schoolId: String, token: String): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        return syncRepository.downloadRoster(
            schoolId = schoolId,
            teacherId = token,
            providedUrl = currentConfig.cloudUrl
        )
    }

    suspend fun fetchDataFromPrincipal(): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = if (currentConfig.schoolId.isNotEmpty()) currentConfig.schoolId else "SCH-VCOL-6072"
        val token = currentConfig.syncSealToken ?: ""
        return downloadClassRoster(schoolId, token)
    }

    suspend fun downloadSimpleRosterForClass(grade: String, section: String, subject: String): Boolean {
        val result = summonClassRosterDetailed(grade, section, subject)
        return result.success
    }

    suspend fun summonClassRosterDetailed(grade: String, section: String, subject: String): SummonResult {
        return try {
            val cleanGrade = grade.trim()
            val cleanSection = section.trim()
            val cleanSubject = subject.trim().ifEmpty { "المادة العامة" }

            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            val schoolId = currentConfig.schoolId.ifEmpty { "school_01" }
            val isSupabase = url.contains("supabase.co")

            // 1. Ensure ClassPackage exists without creating duplicates
            val allPackages = packageDao.getAllPackagesList()
            val existingPkg = allPackages.find { 
                syncRepository.normalizeArabic(it.grade) == syncRepository.normalizeArabic(cleanGrade) &&
                syncRepository.normalizeArabic(it.section) == syncRepository.normalizeArabic(cleanSection) &&
                syncRepository.normalizeArabic(it.subject) == syncRepository.normalizeArabic(cleanSubject)
            }

            val isNewPackage = existingPkg == null
            if (isNewPackage) {
                packageDao.insertPackage(
                    ClassPackage(
                        grade = cleanGrade,
                        section = cleanSection,
                        subject = cleanSubject,
                        iconName = "yrd"
                    )
                )
            }

            // 2. Fetch students from Cloud if URL is configured
            val studentsFromCloud = if (url.isNotEmpty()) {
                if (isSupabase) {
                    syncRepository.downloadSimpleRoster(schoolId, cleanGrade, cleanSection).map {
                        StudentDto(
                            recordNumber = it.record_number,
                            fullName = it.full_name,
                            grade = it.current_grade,
                            section = it.section,
                            historicalAbsences = it.absences_count
                        )
                    }
                } else {
                    val api = getApi(url)
                    val response = api.downloadSimpleRoster(cleanGrade, cleanSection)
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()!!.students
                    } else {
                        emptyList()
                    }
                }
            } else {
                emptyList()
            }

            var mergedCount = 0
            if (studentsFromCloud.isNotEmpty()) {
                val currentLocalStudents = studentDao.getStudentsForGradeAndSection(cleanGrade, cleanSection)
                    .filter { syncRepository.normalizeArabic(it.subject) == syncRepository.normalizeArabic(cleanSubject) }

                studentsFromCloud.forEach { studentDto ->
                    val cleanDtoName = studentDto.fullName.trim()
                    val existingStudent = currentLocalStudents.find { old ->
                        (old.recordNumber.isNotBlank() && old.recordNumber == studentDto.recordNumber) ||
                        (syncRepository.normalizeArabic(old.fullName) == syncRepository.normalizeArabic(cleanDtoName))
                    }

                    if (existingStudent != null) {
                        // Update basic info without touching marks
                        studentDao.updateStudent(
                            existingStudent.copy(
                                fullName = cleanDtoName,
                                recordNumber = if (studentDto.recordNumber.isNotBlank()) studentDto.recordNumber else existingStudent.recordNumber,
                                historicalAbsences = studentDto.historicalAbsences
                            )
                        )
                    } else {
                        studentDao.insertStudent(
                            Student(
                                recordNumber = if (studentDto.recordNumber.isNotBlank()) studentDto.recordNumber else (1000..9999).random().toString(),
                                fullName = cleanDtoName,
                                grade = cleanGrade,
                                section = cleanSection,
                                subject = cleanSubject,
                                historicalAbsences = studentDto.historicalAbsences
                            )
                        )
                    }
                    mergedCount++
                }
                SummonResult(
                    success = true,
                    studentCount = mergedCount,
                    isNewPackage = isNewPackage,
                    message = "تم بنجاح استدعاء وتحديث ($mergedCount) طالباً لشعبة $cleanGrade ($cleanSection) - $cleanSubject ✓"
                )
            } else {
                if (isNewPackage) {
                    SummonResult(
                        success = true,
                        studentCount = 0,
                        isNewPackage = true,
                        message = "تم إنشاء سجل لشعبة $cleanGrade ($cleanSection) - $cleanSubject بنجاح."
                    )
                } else {
                    SummonResult(
                        success = true,
                        studentCount = 0,
                        isNewPackage = false,
                        message = "السجل موجود مسبقاً وتم التأكد من مزامنته بنجاح."
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SummonResult(
                success = false,
                studentCount = 0,
                isNewPackage = false,
                message = "حدث خطأ أثناء الاستدعاء: ${e.localizedMessage ?: "تأكد من الاتصال بالسحابة"}"
            )
        }
    }

    suspend fun syncSimpleAll(): Boolean {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            if (url.isEmpty()) return false

            val allPackages = packageDao.getAllPackagesList()
            if (allPackages.isEmpty()) return true

            var anySuccess = false
            for (pkg in allPackages) {
                val success = downloadSimpleRosterForClass(pkg.grade, pkg.section, pkg.subject)
                if (success) anySuccess = true
            }
            anySuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun propagateStudents() {
        val allPackages = packageDao.getAllPackagesList()
        val uniqueClassSections = allPackages.map { it.grade to it.section }.distinct()
        
        for ((grade, section) in uniqueClassSections) {
            val students = studentDao.getStudentsForGradeAndSection(grade, section)
            val uniqueStudents = students.distinctBy { it.recordNumber }
            val subjects = allPackages.filter { it.grade == grade && it.section == section }.map { it.subject }.distinct()
            
            for (student in uniqueStudents) {
                for (subject in subjects) {
                    val exists = studentDao.getStudentByDetails(grade, section, student.recordNumber, subject) != null
                    if (!exists) {
                        studentDao.insertStudent(
                            Student(
                                recordNumber = student.recordNumber,
                                fullName = student.fullName,
                                grade = grade,
                                section = section,
                                subject = subject,
                                historicalAbsences = student.historicalAbsences
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun syncGrades(grade: String, section: String, subject: String): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = if (currentConfig.schoolId.isNotEmpty()) currentConfig.schoolId.trim() else "SCH-VCOL-6072"
        val token = if (!currentConfig.syncSealToken.isNullOrEmpty()) currentConfig.syncSealToken!!.trim() else null
        return syncRepository.syncGradesAndAttendance(
            schoolId = schoolId,
            teacherId = token,
            targetGrade = grade,
            targetSection = section,
            targetSubject = subject
        )
    }

    suspend fun queryAiAssistant(queryText: String): AiAssistantResponse {
        val reply = geminiAssistantService.askGemini(queryText)
        
        if (reply == "AI_DISABLED") {
            return AiAssistantResponse(
                success = false,
                responseText = null,
                action = null,
                targetView = null,
                targetTheme = null,
                searchQuery = null,
                error = "الذكاء الاصطناعي غير مفعل. يرجى التأكد من ربط المدرسة أو إضافة مفتاح Gemini الخاص بك في الإعدادات."
            )
        }

        return AiAssistantResponse(
            success = true,
            responseText = reply,
            action = if (reply.contains("بحث") || reply.contains("أين")) "SEARCH_STUDENT" else null,
            targetView = null,
            targetTheme = null,
            searchQuery = if (reply.contains("بحث")) reply.split(" ").lastOrNull() else null,
            error = null
        )
    }

    suspend fun pairSchoolByCode(pairingCode: String, teacherName: String): PairingResult {
        val result = syncRepository.pairSchoolByCode(pairingCode, teacherName)
        if (result.success) {
            // After successful pairing, fetch Gemini Key
            val cloudGeminiKey = schoolRepository.fetchCloudGeminiKey()
            val currentConfig = configDao.getConfig().first()
            if (currentConfig != null && !cloudGeminiKey.isNullOrEmpty()) {
                configDao.saveConfig(currentConfig.copy(cloudGeminiKey = cloudGeminiKey))
            }
        }
        return result
    }

    suspend fun downloadSchedule(context: android.content.Context): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = currentConfig.schoolId.ifEmpty { "school_01" }
        return syncRepository.downloadSchedule(context, schoolId)
    }

    suspend fun getSchoolAvailableClasses(): List<SchoolClassSubjectItem> {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = currentConfig.schoolId.ifEmpty { "SCH-KAB2-6884" }
        return syncRepository.getSchoolAvailableClasses(schoolId, currentConfig.cloudUrl, currentConfig.cloudKey)
    }

    suspend fun downloadSelectedClasses(selectedItems: List<SchoolClassSubjectItem>): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = currentConfig.schoolId.ifEmpty { "SCH-KAB2-6884" }
        val ok = syncRepository.downloadSelectedClassesRoster(schoolId, selectedItems, currentConfig.cloudUrl, currentConfig.cloudKey)
        if (ok) {
            propagateStudents()
        }
        return ok
    }
}

data class PairingResult(
    val success: Boolean,
    val warning: Boolean,
    val message: String
)

data class SummonResult(
    val success: Boolean,
    val studentCount: Int,
    val isNewPackage: Boolean,
    val message: String
)
