package com.school.system.data

import com.school.system.data.dao.ConfigDao
import com.school.system.data.dao.StudentDao
import com.school.system.data.dao.ClassPackageDao
import com.school.system.data.dao.AbsenceDao
import com.school.system.data.model.SchoolConfig
import com.school.system.data.model.Student
import com.school.system.data.model.ClassPackage
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
    private val absenceDao: AbsenceDao
) {

    private fun getApi(baseUrl: String): DiyalaSchoolApi {
        var formattedUrl = baseUrl.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
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
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "http://$formattedUrl"
            }
            val api = getApi(formattedUrl)
            val response = api.checkHealth()
            if (response.isSuccessful) {
                val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                configDao.saveConfig(currentConfig.copy(cloudUrl = formattedUrl))
                true
            } else {
                if (formattedUrl.contains(".")) {
                    val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                    configDao.saveConfig(currentConfig.copy(cloudUrl = formattedUrl))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "http://$formattedUrl"
            }
            if (formattedUrl.contains(".")) {
                val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                configDao.saveConfig(currentConfig.copy(cloudUrl = formattedUrl))
                true
            } else {
                false
            }
        }
    }

    suspend fun connectAndPairQr(qrJson: String): Boolean {
        return try {
            val gson = com.google.gson.Gson()
            val mapType = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
            val data: Map<String, String> = gson.fromJson(qrJson, mapType)

            var url = data["url"] ?: return false
            url = url.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            val schoolId = data["schoolId"] ?: "school_01"
            val schoolName = data["schoolName"] ?: "المدرسة"
            val pairingCode = data["pairingCode"] ?: ""

            // Fallback: save anyway so that even if the PC server is not reachable right now,
            // the teacher's config is successfully updated to the correct server IP.
            configDao.saveConfig(
                SchoolConfig(
                    id = 1,
                    cloudUrl = url,
                    schoolName = schoolName,
                    schoolId = schoolId,
                    directorateName = schoolId,
                    isVerified = false,
                    syncSealToken = "",
                    pairingCode = pairingCode
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // If QR json is not standard, check if it's a raw URL string
            try {
                var url = qrJson.trim()
                if (url.contains(".") || url.startsWith("http")) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "http://$url"
                    }
                    val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
                    configDao.saveConfig(currentConfig.copy(cloudUrl = url, isVerified = false, syncSealToken = ""))
                    true
                } else {
                    false
                }
            } catch(ex: Exception) {
                false
            }
        }
    }

    suspend fun requestPairing(teacherName: String, grade: String, section: String, subject: String, pairingCode: String): PairingResult {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            val schoolId = currentConfig.schoolId // Updated to use schoolId
            val email = currentConfig.userEmail   // Added userEmail

            if (url.isEmpty()) return PairingResult(success = false, warning = false, message = "الرجاء مسح باركود المدرسة أولاً للاتصال بالخادم.")

            val api = getApi(url)
            val response = api.requestPairing(PairingRequest(schoolId, teacherName, grade, section, subject, pairingCode, email))

            if (response.isSuccessful) {
                val body = response.body()!!
                if (body.success) {
                    if (body.status == "approved") {
                        configDao.saveConfig(
                            currentConfig.copy(
                                managerName = teacherName,
                                isVerified = true,
                                syncSealToken = body.token
                            )
                        )
                        val downloaded = downloadClassRoster(schoolId, body.token!!)
                        PairingResult(success = true, warning = false, message = if (downloaded) "تم الربط وتنزيل الأسماء بنجاح!" else "تمت الموافقة ولكن فشل تنزيل الأسماء.")
                    } else {
                        configDao.saveConfig(
                            currentConfig.copy(
                                managerName = teacherName,
                                isVerified = false,
                                syncSealToken = body.token
                            )
                        )
                        PairingResult(success = true, warning = false, message = body.message)
                    }
                } else if (body.warning) {
                    PairingResult(success = false, warning = true, message = body.message)
                } else {
                    PairingResult(success = false, warning = false, message = body.message)
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: ""
                val msg = if (errorMsg.contains("رمز") || errorMsg.contains("الرمز")) {
                    "رمز الاقتران الموحد غير صحيح! يرجى التأكد من الرمز وإعادة المحاولة."
                } else {
                    "فشل الاتصال بخادم المدرسة. كود الخطأ: ${response.code()}"
                }
                PairingResult(success = false, warning = false, message = msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PairingResult(success = false, warning = false, message = "خطأ في الشبكة أو خادم المدرسة: ${e.message}")
        }
    }

    suspend fun downloadClassRoster(schoolId: String, token: String): Boolean {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            if (url.isEmpty()) return false

            val api = getApi(url)
            val response = api.downloadRoster(schoolId, token)

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                
                // Safe offline-first merge: Get all existing students with their entered marks
                val existingStudents = studentDao.getAllStudentsList()

                packageDao.clearAll()
                studentDao.clearAll()

                body.classes.forEach { classDto ->
                    packageDao.insertPackage(
                        ClassPackage(
                            grade = classDto.grade,
                            section = classDto.section,
                            subject = classDto.subject,
                            iconName = "yrd"
                        )
                    )
                }

                body.students.forEach { studentDto ->
                    studentDao.insertStudent(
                        Student(
                            recordNumber = studentDto.recordNumber,
                            fullName = studentDto.fullName,
                            grade = studentDto.grade,
                            section = studentDto.section,
                            subject = "",
                            historicalAbsences = studentDto.historicalAbsences
                        )
                    )
                }

                propagateStudents()

                // Restore marks for the propagated student records
                val propagatedStudents = studentDao.getAllStudentsList()
                propagatedStudents.forEach { newStudent ->
                    val matchedOld = existingStudents.find { oldStudent ->
                        oldStudent.recordNumber == newStudent.recordNumber &&
                        oldStudent.grade == newStudent.grade &&
                        oldStudent.section == newStudent.section &&
                        oldStudent.subject == newStudent.subject
                    }
                    if (matchedOld != null) {
                        studentDao.updateStudent(
                            newStudent.copy(marks = matchedOld.marks)
                        )
                    }
                }

                true
            } else {
                // If the link is revoked/stopped by manager, check and reset verification
                val errorBody = response.errorBody()?.string() ?: ""
                if (response.code() == 400 && errorBody.contains("REVOKED")) {
                    configDao.saveConfig(
                        currentConfig.copy(
                            isVerified = false,
                            syncSealToken = ""
                        )
                    )
                }
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchDataFromPrincipal(): Boolean {
        val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
        val schoolId = currentConfig.schoolId.ifEmpty { currentConfig.directorateName }
        val token = currentConfig.syncSealToken
        if (token.isNullOrEmpty()) {
            // Fallback to simple sync if no pairing token is present
            return syncSimpleAll()
        }
        if (schoolId.isEmpty()) return false
        return downloadClassRoster(schoolId, token)
    }

    suspend fun downloadSimpleRosterForClass(grade: String, section: String, subject: String): Boolean {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            if (url.isEmpty()) return false

            val api = getApi(url)
            val response = api.downloadSimpleRoster(grade, section)

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!

                // Insert the ClassPackage if it doesn't exist
                val existingPkg = packageDao.getAllPackagesList().find { 
                    it.grade == grade && it.section == section && it.subject == subject 
                }
                if (existingPkg == null) {
                    packageDao.insertPackage(
                        ClassPackage(
                            grade = grade,
                            section = section,
                            subject = subject,
                            iconName = "yrd"
                        )
                    )
                }

                // Insert or update students safely (avoiding duplicate records and preserving existing marks)
                body.students.forEach { studentDto ->
                    val existingStudent = studentDao.getStudentByDetails(grade, section, studentDto.recordNumber, subject)
                    if (existingStudent != null) {
                        studentDao.updateStudent(
                            existingStudent.copy(
                                fullName = studentDto.fullName,
                                historicalAbsences = studentDto.historicalAbsences
                            )
                        )
                    } else {
                        studentDao.insertStudent(
                            Student(
                                recordNumber = studentDto.recordNumber,
                                fullName = studentDto.fullName,
                                grade = grade,
                                section = section,
                                subject = subject,
                                historicalAbsences = studentDto.historicalAbsences
                            )
                        )
                    }
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
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            val schoolId = currentConfig.schoolId.ifEmpty { currentConfig.directorateName }
            val token = currentConfig.syncSealToken

            if (url.isEmpty() || token.isNullOrEmpty()) return false

            val api = getApi(url)
            val studentsInClass = studentDao.getStudentsForClass(grade, section, subject).first()
            val allAbsences = absenceDao.getAllAbsences().first()

            val gradesList = studentsInClass.map { std ->
                val activeCount = allAbsences.count { it.studentId == std.id }
                val totalAbsences = std.historicalAbsences + activeCount

                StudentGradeSyncDto(
                    recordNumber = std.recordNumber,
                    marks = StudentMarksDto(
                        m1Daily = std.marks.m1Daily,
                        m1Written = std.marks.m1Written,
                        m1MonthAvg = std.marks.m1MonthAvg,
                        m2Daily = std.marks.m2Daily,
                        m2Written = std.marks.m2Written,
                        m2MonthAvg = std.marks.m2MonthAvg,
                        term1Avg = std.marks.term1Avg,
                        midtermOral = std.marks.midtermOral,
                        midtermScore = std.marks.midtermScore,
                        midtermTotal = std.marks.midtermTotal,
                        midtermFinalGrade = std.marks.midtermFinalGrade,
                        m3Daily = std.marks.m3Daily,
                        m3Written = std.marks.m3Written,
                        m3MonthAvg = std.marks.m3MonthAvg,
                        m4Daily = std.marks.m4Daily,
                        m4Written = std.marks.m4Written,
                        m4MonthAvg = std.marks.m4MonthAvg,
                        term2Avg = std.marks.term2Avg,
                        annualAverage = std.marks.annualAverage,
                        finalOral = std.marks.finalOral,
                        finalWrittenD1 = std.marks.finalWrittenD1,
                        finalWrittenD2 = std.marks.finalWrittenD2,
                        finalExamTotal = std.marks.finalExamTotal,
                        finalGrade = std.marks.finalGrade,
                        result = std.marks.result,
                        status = std.marks.status
                    ),
                    absencesCount = totalAbsences
                )
            }

            val response = api.uploadGrades(
                UploadGradesRequest(
                    schoolId = schoolId,
                    token = token,
                    grade = grade,
                    section = section,
                    subject = subject,
                    gradesList = gradesList
                )
            )

            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun queryAiAssistant(queryText: String): AiAssistantResponse {
        return try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val url = currentConfig.cloudUrl
            if (url.isEmpty()) {
                return AiAssistantResponse(false, "يرجى ربط التطبيق بالسحابة أولاً لتمكين المساعد الذكي.", null, null, null, null, null)
            }

            // Fetch statistics
            val uniqueStudents = studentDao.getUniqueStudentsCount()
            val api = getApi(url)
            val request = AiAssistantRequest(
                query = queryText,
                studentsCount = uniqueStudents,
                staffCount = 1, // Simulated teacher count (1) or list size
                schoolName = currentConfig.schoolName,
                userApiKey = if (currentConfig.geminiApiKey.isNotEmpty()) currentConfig.geminiApiKey else null
            )

            val response = api.askAiAssistant(request)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                AiAssistantResponse(false, "عذرًا، فشل الحصول على استجابة من الذكاء الاصطناعي: $errorMsg", null, null, null, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AiAssistantResponse(false, "فشل الاتصال بخادم الذكاء الاصطناعي: ${e.localizedMessage}", null, null, null, null, null)
        }
    }
}

data class PairingResult(
    val success: Boolean,
    val warning: Boolean,
    val message: String
)
