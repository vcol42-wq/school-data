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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

sealed class SecurityLockState {
    object Unlocked : SecurityLockState()
    data class Locked(
        val reason: String,
        val isSchoolCodeChanged: Boolean = false,
        val isTeacherRevoked: Boolean = false
    ) : SecurityLockState()
}

@Singleton
class SyncManager @Inject constructor(
    private val configDao: ConfigDao,
    private val studentDao: StudentDao,
    val packageDao: ClassPackageDao,
    private val absenceDao: AbsenceDao,
    private val syncRepository: SyncRepository,
    private val schoolRepository: SchoolRepository,
    private val geminiAssistantService: GeminiAssistantService,
    private val secureKeyStorage: com.school.system.data.local.SecureKeyStorage
) {
    companion object {
        const val DEFAULT_AI_GATEWAY = "https://theprinciple-ai.up.railway.app/" // Example global AI gateway
    }

    val securityLockState = kotlinx.coroutines.flow.MutableStateFlow<SecurityLockState>(SecurityLockState.Unlocked)

    suspend fun validateSessionIntegrity(): Boolean {
        val currentConfig = configDao.getConfig().first() ?: return true
        if (!currentConfig.isVerified || currentConfig.schoolId.isBlank() || currentConfig.schoolId == "school_01") {
            return true
        }
        val resolvedTeacher = syncRepository.getResolvedTeacherName().takeIf { it.isNotBlank() }
            ?: (if (currentConfig.role == "supervisor") currentConfig.managerName else "")
        val result = syncRepository.checkSessionIntegrity(
            schoolId = currentConfig.schoolId,
            currentPairingCode = currentConfig.pairingCode,
            teacherId = currentConfig.syncSealToken,
            teacherName = resolvedTeacher,
            role = currentConfig.role
        )
        if (!result.isValid) {
            securityLockState.value = SecurityLockState.Locked(
                reason = result.message,
                isSchoolCodeChanged = result.isSchoolCodeChanged,
                isTeacherRevoked = result.isTeacherRevoked
            )
            return false
        } else {
            if (securityLockState.value is SecurityLockState.Locked) {
                securityLockState.value = SecurityLockState.Unlocked
            }
            return true
        }
    }

    suspend fun purgeEnrolledSchoolData(context: android.content.Context) {
        try {
            // 1. Delete all packages / classes
            packageDao.clearAll()

            // 2. Delete all students
            studentDao.clearAll()

            // 3. Delete all absences
            try {
                absenceDao.clearAll()
            } catch (_: Exception) {}

            // 4. Reset config to completely offline/unpaired state
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            configDao.saveConfig(
                currentConfig.copy(
                    schoolId = "",
                    schoolName = "",
                    pairingCode = "",
                    syncSealToken = null,
                    isVerified = false,
                    isActivated = false
                )
            )

            // 5. Clear all shared preferences and session caches
            com.example.theboss.data.local.SessionManager(context).clearSchoolSession()

            try {
                context.getSharedPreferences("diyala_school_prefs", android.content.Context.MODE_PRIVATE).edit()
                    .remove("diyala_school_id")
                    .remove("school_id")
                    .remove("school_name")
                    .remove("synced_schedule")
                    .remove("cached_teacher_enrolled_classes")
                    .remove("cached_enrolled_classes_json")
                    .remove("widget_selected_day_idx")
                    .apply()

                context.getSharedPreferences("the_boss_prefs", android.content.Context.MODE_PRIVATE).edit()
                    .remove("school_id")
                    .remove("school_code")
                    .remove("school_name")
                    .remove("synced_schedule")
                    .remove("last_saved_school_code")
                    .remove("independent_mode")
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 6. Update all widgets so they reflect the empty offline schedule
            com.school.system.widget.WidgetScheduleHelper.updateAllWidgets(context)

            // 7. Reset security lock state
            securityLockState.value = SecurityLockState.Unlocked

            android.util.Log.d("SyncManager", "purgeEnrolledSchoolData: Successfully purged all enrolled classes and school data.")
        } catch (e: Exception) {
            android.util.Log.e("SyncManager", "purgeEnrolledSchoolData error: ${e.message}")
        }
    }

    private fun getApi(baseUrl: String): DiyalaSchoolApi {
        var formattedUrl = baseUrl.trim()
        if (!formattedUrl.startsWith("http")) {
            formattedUrl = "https://$formattedUrl"
        }
        val url = if (formattedUrl.endsWith("/")) formattedUrl else "$formattedUrl/"
        
        val client = OkHttpClient.Builder()
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

    suspend fun connectAndPairQr(qrContent: String, forceRole: String? = null): Boolean {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val trimmed = qrContent.trim()

            var url = SyncRepository.DEFAULT_SUPABASE_URL
            var apiKey = SyncRepository.DEFAULT_ANON_KEY
            var schoolId = currentConfig.schoolId.trim()
            var pairingCode = currentConfig.pairingCode
            var teacherName = ""
            var isPrincipal = forceRole == "supervisor" || forceRole == "principal" || currentConfig.role == "supervisor"

            var schoolName = ""
            var directorateName = ""

            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val jsonObj = org.json.JSONObject(trimmed)

                fun jsonOpt(vararg keys: String): String? {
                    for (k in keys) {
                        val v = jsonObj.optString(k, "").trim()
                        if (v.isNotBlank() && v != "null") return v
                    }
                    return null
                }

                url = jsonOpt("url", "supabaseUrl", "cloudUrl") ?: SyncRepository.DEFAULT_SUPABASE_URL
                apiKey = jsonOpt("apiKey", "cloudKey", "api_key", "anonKey") ?: SyncRepository.DEFAULT_ANON_KEY
                schoolId = jsonOpt("schoolId", "school_id", "id") ?: schoolId
                val princCode = jsonOpt("principalPairingCode", "principal_pairing_code", "adminPairingCode")
                val studCode = jsonOpt("studentPairingCode", "student_pairing_code")
                val pCode = jsonOpt("pairingCode", "pairing_code")
                
                teacherName = jsonOpt("teacherName", "teacher_name", "name") ?: ""
                schoolName = jsonOpt("schoolName", "school_name") ?: ""
                directorateName = jsonOpt("directorateName", "directorate_name") ?: ""

                val roleStr = jsonOpt("role") ?: ""
                if (roleStr.equals("principal", ignoreCase = true) || roleStr.equals("supervisor", ignoreCase = true) || (isPrincipal && princCode != null)) {
                    isPrincipal = true
                }
                
                pairingCode = when {
                    isPrincipal && !princCode.isNullOrBlank() -> princCode
                    !pCode.isNullOrBlank() -> pCode
                    !studCode.isNullOrBlank() -> studCode
                    else -> pairingCode
                }
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
            } else if (trimmed.startsWith("SUPERVISOR:", ignoreCase = true) || trimmed.startsWith("PRINCIPAL:", ignoreCase = true)) {
                val parts = trimmed.split(":")
                pairingCode = parts.getOrNull(1)?.trim() ?: pairingCode
                val secondPart = parts.getOrNull(2)?.trim()
                if (secondPart != null && secondPart.startsWith("SCH-", ignoreCase = true)) {
                    schoolId = secondPart
                }
                teacherName = ""
                isPrincipal = true
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                url = trimmed
            } else if (trimmed.startsWith("SCH-", ignoreCase = true)) {
                schoolId = trimmed
                pairingCode = trimmed
            } else if (trimmed.length in 4..14 && trimmed.all { it.isDigit() || it.isLetter() || it == '-' || it == '_' }) {
                pairingCode = trimmed
            }

            if (isPrincipal) {
                if (pairingCode.isNotBlank()) {
                    secureKeyStorage.saveSupervisorCode(pairingCode)
                }
                configDao.saveConfig(
                    currentConfig.copy(
                        cloudUrl = url,
                        cloudKey = apiKey,
                        schoolId = schoolId,
                        schoolName = if (schoolName.isNotBlank()) schoolName else currentConfig.schoolName,
                        directorateName = if (directorateName.isNotBlank()) directorateName else currentConfig.directorateName,
                        pairingCode = pairingCode,
                        role = "supervisor",
                        managerName = if (teacherName.isNotBlank() && !teacherName.contains("مدير") && !teacherName.contains("مشرف") && !teacherName.contains("إشراف") && !teacherName.contains("اشراف")) teacherName else "",
                        syncSealToken = if (currentConfig.syncSealToken.isNullOrEmpty()) "__supervisor__" else currentConfig.syncSealToken,
                        isVerified = true,
                        isActivated = true
                    )
                )
                if (schoolName.isNotBlank()) {
                    schoolRepository.updateSchoolLocally(schoolId, pairingCode, schoolName)
                }
                if (schoolId.isNotBlank() && schoolId != "school_01") {
                    try {
                        syncRepository.downloadRoster(
                            schoolId = schoolId,
                            teacherId = "__supervisor__",
                            providedUrl = url,
                            providedKey = apiKey
                        )
                    } catch (_: Exception) {}
                }
                return true
            }

            // Save basic settings to config
            configDao.saveConfig(
                currentConfig.copy(
                    cloudUrl = url,
                    cloudKey = apiKey,
                    schoolId = schoolId,
                    schoolName = if (schoolName.isNotBlank()) schoolName else currentConfig.schoolName,
                    directorateName = if (directorateName.isNotBlank()) directorateName else currentConfig.directorateName,
                    pairingCode = pairingCode,
                    isVerified = false
                )
            )
            if (schoolName.isNotBlank()) {
                schoolRepository.updateSchoolLocally(schoolId, pairingCode, schoolName)
            }

            // Auto-verify with school: if only pairingCode was provided, search school by code
            val pairRes = if (schoolId == "school_01" || schoolId.isEmpty() || pairingCode.isNotBlank()) {
                val codeResult = pairSchoolByCode(
                    pairingCode = pairingCode.ifBlank { schoolId },
                    teacherName = teacherName
                )
                if (codeResult.success) codeResult else {
                    requestPairing(
                        teacherName = teacherName,
                        grade = "",
                        section = "",
                        subject = "",
                        pairingCode = pairingCode
                    )
                }
            } else {
                requestPairing(
                    teacherName = teacherName,
                    grade = "",
                    section = "",
                    subject = "",
                    pairingCode = pairingCode
                )
            }

            pairRes.success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun requestPairing(teacherName: String, grade: String, section: String, subject: String, pairingCode: String): PairingResult {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = currentConfig.schoolId.trim()
        return syncRepository.verifySchoolAndTeacher(
            schoolId = schoolId,
            teacherInput = teacherName,
            pairingCode = pairingCode,
            providedUrl = currentConfig.cloudUrl,
            providedKey = currentConfig.cloudKey
        )
    }

    suspend fun downloadClassRoster(schoolId: String, token: String): Boolean {
        val cleanSchoolId = schoolId.trim()
        if (cleanSchoolId.isBlank() || cleanSchoolId == "school_01") return false
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        return syncRepository.downloadRoster(
            schoolId = cleanSchoolId,
            teacherId = token,
            providedUrl = currentConfig.cloudUrl,
            providedKey = currentConfig.cloudKey
        )
    }

    private fun resolveActiveSchoolId(currentConfig: SchoolConfig): String {
        return currentConfig.schoolId.trim()
    }

    suspend fun fetchDataFromPrincipal(): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = resolveActiveSchoolId(currentConfig)
        if (schoolId.isBlank() || schoolId == "school_01" || !currentConfig.isVerified) {
            android.util.Log.w("SyncManager", "fetchDataFromPrincipal aborted: unverified schoolId ($schoolId)")
            return false
        }
        val token = currentConfig.syncSealToken ?: ""

        val localPackages = packageDao.getAllPackagesList()
        if (localPackages.isNotEmpty()) {
            val resolvedTeacher = syncRepository.getResolvedTeacherName().takeIf { it.isNotBlank() }
                ?: (if (currentConfig.role == "supervisor") currentConfig.managerName else "")
            val items = localPackages.map { pkg ->
                SchoolClassSubjectItem(
                    grade = pkg.grade,
                    section = pkg.section,
                    subject = pkg.subject,
                    teacherName = resolvedTeacher
                )
            }
            return downloadSelectedClasses(items)
        }

        return downloadClassRoster(schoolId, token)
    }

    suspend fun downloadSimpleRosterForClass(grade: String, section: String, subject: String): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        if (!currentConfig.isCloudSyncEnabled) {
            return false
        }
        val result = summonClassRosterDetailed(grade, section, subject)
        return result.success
    }

    suspend fun summonClassRosterDetailed(grade: String, section: String, subject: String): SummonResult {
        return try {
            val cleanGrade = grade.trim()
            val cleanSection = section.trim()
            val cleanSubject = subject.trim().ifEmpty { "المادة العامة" }

            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl.ifEmpty { SyncRepository.DEFAULT_SUPABASE_URL }
            val schoolId = resolveActiveSchoolId(currentConfig)
            val isSupabase = url.contains("supabase")

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

            // 2. Fetch students from Cloud
            val studentsFromCloud = if (url.isNotEmpty()) {
                if (isSupabase) {
                    syncRepository.downloadSimpleRoster(schoolId, cleanGrade, cleanSection).map {
                        StudentDto(
                            recordNumber = it.record_number ?: "",
                            fullName = it.full_name ?: "",
                            grade = it.current_grade ?: "",
                            section = it.section ?: "",
                            historicalAbsences = it.absences_count ?: 0
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
                        if (existingStudent.recordNumber.isNotBlank()) {
                            studentDao.updateStudentNameForAllSubjects(cleanGrade, cleanSection, existingStudent.recordNumber, cleanDtoName)
                        }
                    } else {
                        val finalRecordNumber = if (studentDto.recordNumber.isNotBlank() && studentDto.recordNumber != "0") studentDto.recordNumber else "std_${Math.abs(cleanDtoName.hashCode())}"
                        studentDao.insertStudent(
                            Student(
                                recordNumber = finalRecordNumber,
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
            val uniqueStudents = students.distinctBy { 
                if (it.recordNumber.isNotBlank() && it.recordNumber != "0") it.recordNumber 
                else "${it.fullName.trim()}_${it.grade}_${it.section}"
            }
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

    suspend fun syncGrades(
        grade: String,
        section: String,
        subject: String,
        secretCode: String = ""
    ): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = resolveActiveSchoolId(currentConfig)
        val token = if (!currentConfig.syncSealToken.isNullOrEmpty()) currentConfig.syncSealToken!!.trim() else null
        return syncRepository.syncGradesAndAttendance(
            schoolId = schoolId,
            teacherId = token,
            targetGrade = grade,
            targetSection = section,
            targetSubject = subject,
            secretCode = secretCode
        ) is GradeUploadResult.Success
    }

    suspend fun restoreGrades(
        grade: String,
        section: String,
        subject: String,
        secretCode: String
    ): GradeRestoreResult {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = resolveActiveSchoolId(currentConfig)
        return syncRepository.restoreTeacherGradesFromCloud(
            schoolId = schoolId,
            targetGrade = grade,
            targetSection = section,
            targetSubject = subject,
            secretCode = secretCode
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

    suspend fun pairPrincipalByCode(pairingCode: String): PairingResult {
        val clean = pairingCode.trim()
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        
        val isTrivial = clean == "112233" || clean == "223344" || clean == "334455" || clean == "123456" || clean == "000000"
        if (clean.length < 4 || isTrivial) {
            return PairingResult(
                success = false,
                warning = true,
                message = "تحذير: رمز المدير غير صالح. يجب إدخال الرمز المعتمد الخاص بإدارة المدرسة."
            )
        }
        
        // Strictly verify school by principal code via syncRepository
        val school = syncRepository.verifyPrincipalSchoolByCode(clean)
        if (school == null) {
            return PairingResult(
                success = false,
                warning = true,
                message = "تحذير أمني مسجل: رمز المدير غير صحيح أو غير مسجل في السحابة."
            )
        }
        val targetSchoolId = school.id
        
        secureKeyStorage.saveSupervisorCode(clean)
        configDao.saveConfig(
            currentConfig.copy(
                schoolId = targetSchoolId,
                schoolName = school.name,
                pairingCode = clean,
                role = "supervisor",
                managerName = "",
                syncSealToken = if (currentConfig.syncSealToken.isNullOrEmpty()) "__supervisor__" else currentConfig.syncSealToken,
                isVerified = true,
                isActivated = true
            )
        )
        
        if (targetSchoolId.isNotBlank() && targetSchoolId != "school_01") {
            try {
                syncRepository.downloadRoster(
                    schoolId = targetSchoolId,
                    teacherId = "__supervisor__",
                    providedUrl = currentConfig.cloudUrl,
                    providedKey = currentConfig.cloudKey
                )
            } catch (_: Exception) {}
        }
        
        return PairingResult(
            success = true,
            warning = false,
            message = "تم تفعيل بوابة المدير والإشراف العام بنجاح 👑"
        )
    }

    suspend fun downloadSchedule(context: android.content.Context): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        if (!currentConfig.isCloudSyncEnabled) {
            android.util.Log.d("SyncManager", "downloadSchedule aborted: isCloudSyncEnabled is false")
            return false
        }
        val schoolId = resolveActiveSchoolId(currentConfig)
        if (schoolId.isBlank() || schoolId == "school_01" || !currentConfig.isVerified) {
            android.util.Log.w("SyncManager", "downloadSchedule aborted: unverified schoolId ($schoolId)")
            return false
        }
        return syncRepository.downloadSchedule(context, schoolId)
    }

    suspend fun getSchoolAvailableClasses(): List<SchoolClassSubjectItem> {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = resolveActiveSchoolId(currentConfig)
        if (schoolId.isBlank() || schoolId == "school_01" || !currentConfig.isVerified) {
            return emptyList()
        }
        return syncRepository.getSchoolAvailableClasses(schoolId, currentConfig.cloudUrl, currentConfig.cloudKey)
    }

    suspend fun downloadSelectedClasses(selectedItems: List<SchoolClassSubjectItem>): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = resolveActiveSchoolId(currentConfig)
        if (schoolId.isBlank() || schoolId == "school_01" || !currentConfig.isVerified) {
            return false
        }
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
