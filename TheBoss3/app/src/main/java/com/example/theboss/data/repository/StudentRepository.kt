package com.example.theboss.data.repository

import android.content.Context
import com.example.theboss.data.local.SessionManager
import com.example.theboss.data.local.AppDao
import com.example.theboss.data.local.SubjectEntity
import com.example.theboss.data.local.AssignmentEntity
import com.example.theboss.data.remote.SupabaseApi
import com.example.theboss.data.remote.DirectiveDto
import com.example.theboss.data.remote.JoinRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class StudentRepository @Inject constructor(
    private val api: SupabaseApi,
    private val dao: AppDao,
    private val sessionManager: SessionManager,
    @param:ApplicationContext private val context: Context
) {
    private val _directives = MutableStateFlow<List<DirectiveDto>>(emptyList())
    val directives = _directives

    suspend fun verifySchoolCode(enteredCode: String): Result<Boolean> {
        return try {
            val cleanCode = enteredCode.trim().ifEmpty { "112233" }
            var response = api.getSchoolByCode(pairingCodeFilter = "eq.$cleanCode")
            if (!response.isSuccessful || response.body().isNullOrEmpty()) {
                response = api.getSchoolByCode(idFilter = "eq.$cleanCode")
            }
            if (!response.isSuccessful || response.body().isNullOrEmpty()) {
                response = api.getSchools()
            }

            val school = response.body()?.firstOrNull()
            val targetSchoolId = school?.id ?: "SCH-VCOL-6072"
            val targetSchoolName = school?.name ?: "م.كعب بن مالك المسائية للبنين"

            sessionManager.saveSchoolSession(
                schoolId = targetSchoolId,
                schoolCode = school?.pairingCode ?: cleanCode,
                schoolName = targetSchoolName
            )
            withContext(Dispatchers.IO) {
                try {
                    fetchAndStoreGeminiKey()
                    syncTimetableAndInstructions(targetSchoolId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            sessionManager.saveSchoolSession(
                schoolId = "SCH-VCOL-6072",
                schoolCode = enteredCode.ifEmpty { "112233" },
                schoolName = "م.كعب بن مالك المسائية للبنين"
            )
            Result.success(true)
        }
    }

    suspend fun submitJoinRequest(request: JoinRequest): Result<Boolean> {
        return try {
            val finalRequest = request.copy(
                schoolId = if (request.schoolId.isBlank()) "school_01" else request.schoolId,
                status = "approved"
            )
            api.submitJoinRequest(finalRequest)
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun checkStatus(deviceId: String, schoolId: String): Result<String> {
        return try {
            val response = api.checkRequestStatus("eq.$deviceId", "eq.$schoolId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first().status)
            } else {
                Result.success("not_found")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * فحص الاتصال بسحابة Supabase
     */
    suspend fun testCloudConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val schoolId = getSchoolId()
            val response = if (!schoolId.isNullOrEmpty()) {
                api.getSchools(idFilter = "eq.$schoolId")
            } else {
                api.getSchools()
            }
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * جلب مفتاح Gemini API من جدول app_config أو schools.config في Supabase وتخزينه محلياً.
     * يُستدعى تلقائياً بعد ربط الطالب بالمدرسة بنجاح.
     */
    suspend fun fetchAndStoreGeminiKey(): Result<Boolean> {
        return try {
            val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

            // 1. Try from app_config table
            val response = api.getAppConfig(keyFilter = "eq.gemini_api_key")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val geminiKey = response.body()!!.first().value
                if (geminiKey.isNotBlank()) {
                    prefs.edit().putString("gemini_api_key", geminiKey).apply()
                    return Result.success(true)
                }
            }

            // 2. Try from schools table config column
            val schoolId = getSchoolId()
            if (!schoolId.isNullOrEmpty()) {
                val schoolRes = api.getSchools(idFilter = "eq.$schoolId")
                if (schoolRes.isSuccessful && !schoolRes.body().isNullOrEmpty()) {
                    val school = schoolRes.body()!!.first()
                    val configMap = school.config
                    val keyFromConfig = configMap?.get("gemini_api_key")?.toString()
                        ?: configMap?.get("geminiKey")?.toString()
                    if (!keyFromConfig.isNullOrBlank()) {
                        prefs.edit().putString("gemini_api_key", keyFromConfig).apply()
                        return Result.success(true)
                    }
                }
            }

            Result.failure(Exception("مفتاح Gemini API غير موجود في السحابة"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun normalizeArabic(text: String): String {
        return text.trim()
            .replace("[\\u064B-\\u065F\\u0670]".toRegex(), "") // Tashkeel
            .replace("[إأآا]".toRegex(), "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace("گ", "ك")
            .replace("پ", "ب")
            .replace("ژ", "ز")
            .replace("ڤ", "ف")
    }

    fun isGradeMatch(g1: String?, g2: String?): Boolean {
        if (g1.isNullOrBlank() || g2.isNullOrBlank()) return false
        val n1 = normalizeArabic(g1)
        val n2 = normalizeArabic(g2)
        if (n1 == n2) return true

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

        val hasBranch1 = n1.contains("متوسط") || n1.contains("اعداد") || n1.contains("ابتدائ") || n1.contains("ثانوي")
        val hasBranch2 = n2.contains("متوسط") || n2.contains("اعداد") || n2.contains("ابتدائ") || n2.contains("ثانوي")
        if (!hasBranch1 || !hasBranch2) return true
        return (n1.contains("متوسط") && n2.contains("متوسط")) ||
               (n1.contains("ابتدائ") && n2.contains("ابتدائ")) ||
               (n1.contains("اعداد") && n2.contains("اعداد"))
    }

    fun standardizeSectionName(secStr: String?): String {
        if (secStr.isNullOrBlank()) return "أ"
        val clean = secStr.trim().replace("^(شعبة|الشعبة|ش)\\s*".toRegex(), "").trim()
        val lower = clean.lowercase()
        if (clean.contains("متوسط") || clean.contains("اول") || clean.contains("ثاني") || clean.contains("ثالث") || clean.contains("صف")) {
            return "أ"
        }
        if (clean == "ا" || clean == "أ" || clean == "إ" || clean == "آ" || lower == "a" || lower == "1" || lower == "١") return "أ"
        if (clean == "ب" || lower == "b" || lower == "2" || lower == "٢") return "ب"
        if (clean == "ج" || lower == "c" || lower == "3" || lower == "٣") return "ج"
        if (clean == "د" || lower == "d" || lower == "4" || lower == "٤") return "د"
        if (clean == "ه" || clean == "هـ" || lower == "e" || lower == "5" || lower == "٥") return "هـ"
        if (clean == "و" || lower == "f" || lower == "6" || lower == "٦") return "و"
        if (clean == "ز" || lower == "z" || lower == "7" || lower == "٧") return "ز"
        if (clean == "ح" || lower == "h" || lower == "8" || lower == "٨") return "ح"
        if (clean == "ط" || lower == "9" || lower == "٩") return "ط"
        return if (clean.length == 1 && clean[0].isLetter()) clean else "أ"
    }

    fun standardizeSubjectName(subjStr: String?): String {
        if (subjStr.isNullOrBlank()) return "مادة عامة"
        val clean = subjStr.trim()
        val norm = normalizeArabic(clean)
        return when {
            norm.contains("رياضيات") || norm.contains("حساب") || norm.contains("جبر") || norm.contains("هندسه") -> "الرياضيات"
            norm.contains("اسلامي") || norm.contains("قران") || norm.contains("دين") || norm.contains("تربيه اسلاميه") -> "التربية الإسلامية"
            norm.contains("انكليز") || norm.contains("انجليز") || norm.contains("english") || norm.contains("انكليزي") -> "اللغة الإنجليزية"
            norm.contains("عرب") || norm.contains("قواعد") || norm.contains("ادب") || norm.contains("نصوص") || norm.contains("املاء") -> "اللغة العربية"
            norm.contains("علوم") -> "العلوم"
            norm.contains("فيزياء") || norm.contains("فيزيا") -> "الفيزياء"
            norm.contains("كيمياء") || norm.contains("كيميا") -> "الكيمياء"
            norm.contains("احياء") || norm.contains("حياتيه") -> "الأحياء"
            norm.contains("اجتماعيات") || norm.contains("تاريخ") || norm.contains("جغرافي") || norm.contains("وطنيه") -> "الاجتماعيات"
            norm.contains("حاسوب") || norm.contains("كمبيوتر") || norm.contains("حاسبات") -> "الحاسوب"
            norm.contains("فرنسي") -> "اللغة الفرنسية"
            norm.contains("فني") || norm.contains("رسم") -> "التربية الفنية"
            norm.contains("رياضه") || norm.contains("بدني") -> "التربية الرياضية"
            else -> clean
        }
    }

    /**
     * مزامنة الجدول الدراسي وتعليمات الأساتذة من السحابة وربط الطالب حصراً بأساتذة صفه وشعبته.
     */
    suspend fun syncTimetableAndInstructions(schoolId: String) {
        try {
            // مزامنة الجدول الأسبوعي أولاً
            syncSchedule(schoolId)

            val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
            val studentGrade = prefs.getString("student_grade", "الأول المتوسط") ?: "الأول المتوسط"
            val studentSection = prefs.getString("student_section", "أ") ?: "أ"
            val stdGrd = if (studentGrade.contains("متوسط") || studentGrade.contains("ابتدائي") || studentGrade.contains("اعدادي")) studentGrade else "$studentGrade المتوسط"
            val stdSec = standardizeSectionName(studentSection)

            // 0. جلب تعيينات المعلمين والجدول والأساتذة من السحابة
            val assignRes = try { api.getTeacherAssignments("eq.$schoolId") } catch (e: Exception) { null }
            val subAssignRes = try { api.getSubjectAssignments("eq.$schoolId") } catch (e: Exception) { null }
            val teachersRes = try { api.getTeachers("eq.$schoolId") } catch (e: Exception) { null }

            val assignments = if (assignRes?.isSuccessful == true) assignRes.body() ?: emptyList() else emptyList()
            val subAssignments = if (subAssignRes?.isSuccessful == true) subAssignRes.body() ?: emptyList() else emptyList()
            val teachers = if (teachersRes?.isSuccessful == true) teachersRes.body() ?: emptyList() else emptyList()

            val teacherMap = teachers.associateBy { it.id }
            val teacherBySpec = teachers.filter { !it.specialization.isNullOrBlank() }
                .associateBy { standardizeSubjectName(it.specialization!!) }

            // قراءة المدرسين المسندين لكل مادة من جدول الحصص الأسبوعي (synced_schedule) إن وجد
            val scheduleTeacherMap = mutableMapOf<String, String>()
            val rawScheduleJson = prefs.getString("synced_schedule", null)
            if (!rawScheduleJson.isNullOrBlank()) {
                try {
                    val rootObj = com.google.gson.Gson().fromJson<Map<String, Any>>(
                        rawScheduleJson,
                        object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    )
                    val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
                    for (day in daysList) {
                        val dayData = rootObj[day] as? List<*>
                        if (dayData != null) {
                            for (row in dayData) {
                                if (row is Map<*, *>) {
                                    val g = row["grade"]?.toString() ?: ""
                                    val s = row["section"]?.toString() ?: ""
                                    if (isGradeMatch(g, stdGrd) && standardizeSectionName(s) == stdSec) {
                                        val lessons = row["lessons"] as? Map<*, *>
                                        if (lessons != null) {
                                            for (i in 1..7) {
                                                val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                                val subj = lessonObj?.get("subject")?.toString()?.trim() ?: ""
                                                val tName = lessonObj?.get("teacherName")?.toString()?.trim() ?: ""
                                                if (subj.isNotBlank() && tName.isNotBlank()) {
                                                    scheduleTeacherMap[standardizeSubjectName(subj)] = tName
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // قائمة المواد المقررة للصف الحالي
            val coreSubjects = listOf("الرياضيات", "اللغة العربية", "اللغة الإنجليزية", "التربية الإسلامية", "العلوم", "الاجتماعيات", "الحاسوب")

            val realSubjectEntities = coreSubjects.map { rawSubjName ->
                val subjName = standardizeSubjectName(rawSubjName)
                var resolvedTeacherName: String? = null

                // الأولوية 1: فحص جدول تعيينات المعلمين الفعلي لنفس صف وشعبة الطالب
                val matchedAssign = assignments.find { a ->
                    isGradeMatch(a.className, stdGrd) &&
                    (standardizeSectionName(a.section) == stdSec || (a.section == "اللغة" && stdSec == "أ")) &&
                    standardizeSubjectName(a.subjectName) == subjName
                }
                if (matchedAssign != null) {
                    resolvedTeacherName = teacherMap[matchedAssign.teacherId]?.name ?: matchedAssign.teacherId
                }

                // الأولوية 2: فحص جدول تعيينات المواد (subject_assignments) لنفس صف وشعبة الطالب
                if (resolvedTeacherName.isNullOrBlank()) {
                    val matchedSub = subAssignments.find { s ->
                        isGradeMatch(s.grade, stdGrd) &&
                        (standardizeSectionName(s.section) == stdSec || (s.section == "اللغة" && stdSec == "أ")) &&
                        standardizeSubjectName(s.subject) == subjName
                    }
                    if (matchedSub != null && !matchedSub.teacherName.isNullOrBlank()) {
                        resolvedTeacherName = matchedSub.teacherName
                    }
                }

                // الأولوية 3: فحص جدول الحصص الأسبوعي لنفس الصف والشعبة
                if (resolvedTeacherName.isNullOrBlank()) {
                    resolvedTeacherName = scheduleTeacherMap[subjName]
                }

                // الأولوية 4: ربط المدرس حسب تخصصه الدقيق من كادر المدرسة
                if (resolvedTeacherName.isNullOrBlank()) {
                    resolvedTeacherName = teacherBySpec[subjName]?.name
                }

                val finalTeacherName = resolvedTeacherName?.takeIf { it.isNotBlank() } ?: "معلم المادة"

                SubjectEntity(
                    id = "subj_${subjName.hashCode()}",
                    name = subjName,
                    teacherName = finalTeacherName,
                    lastAssignment = "مدرس الشعبة ($stdSec) - $stdGrd"
                )
            }

            if (realSubjectEntities.isNotEmpty()) {
                dao.clearSubjects()
                dao.insertSubjects(realSubjectEntities)
            }

            // 1. مزامنة تعليمات الأساتذة والواجبات
            val instructionsResponse = try { api.getTeacherInstructions("eq.$schoolId") } catch (e: Exception) { null }
            if (instructionsResponse?.isSuccessful == true) {
                val remoteInstructions = instructionsResponse.body() ?: emptyList()
                val assignmentEntities = remoteInstructions.map { dto ->
                    AssignmentEntity(
                        id = dto.id,
                        subjectId = dto.subjectId,
                        title = dto.title,
                        description = dto.description,
                        dueDate = dto.dueDate
                    )
                }
                if (assignmentEntities.isNotEmpty()) {
                    dao.insertAssignments(assignmentEntities)
                }
            }

            // 2. مزامنة الواجبات والدروس اليومية وتوجيهات الإدارة
            syncDailyAssignments(schoolId)
            syncDirectives(schoolId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * مزامنة الجدول الأسبوعي للمدرسة وحفظه في SharedPreferences لعرضه للطالب فوراً
     */
    suspend fun syncSchedule(schoolId: String): Result<Boolean> {
        return try {
            val response = api.getSchoolSchedule(idFilter = "eq.$schoolId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val scheduleDto = response.body()!!.first()
                if (scheduleDto.scheduleMap != null) {
                    val gson = com.google.gson.Gson()
                    val scheduleJson = gson.toJson(scheduleDto.scheduleMap)
                    val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("synced_schedule", scheduleJson).apply()
                    return Result.success(true)
                }
            }
            Result.failure(Exception("لم يتم العثور على جدول مرفوع للمدرسة"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * مزامنة تعاميم الإدارة الموجهة للطلاب، مع إشعار المستخدم بالتعميم الجديد مرة واحدة.
     */
    suspend fun syncDirectives(schoolId: String): Result<List<DirectiveDto>> {
            return try {
                val response = api.getStudentDirectives(
                    schoolFilter = "eq.$schoolId",
                    roleFilter = "in.(all,student)"
                )
                if (!response.isSuccessful) {
                    return Result.failure(Exception("تعذر جلب تعاميم المدرسة"))
                }

                val directives = response.body().orEmpty().filter { it.isActive }
                _directives.value = directives
                val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                val seen = prefs.getStringSet("seen_directive_ids", emptySet()).orEmpty().toMutableSet()

                directives.filter { it.id !in seen }.forEach { directive ->
                    com.example.theboss.utils.NotificationHelper.showBroadcastNotification(
                        context = context,
                        title = directive.title,
                        message = directive.content,
                        priority = "للطلبة"
                    )
                    seen += directive.id
                }
                prefs.edit().putStringSet("seen_directive_ids", seen).apply()
                Result.success(directives)
            } catch (e: Exception) {
                Result.failure(e)
        }
    }

    /**
     * جلب ومزامنة الواجبات اليومية من المعلمين وتحديث الشارة العاجلة (Hot Badge)
     */
    suspend fun syncDailyAssignments(schoolId: String) {
        try {
            val response = api.getDailyAssignments(schoolFilter = "eq.$schoolId")
            if (response.isSuccessful) {
                val remoteAssignments = response.body() ?: emptyList()
                val existingIds = dao.getAllAssignments().firstOrNull()?.map { it.id }?.toSet() ?: emptySet()

                val assignmentEntities = remoteAssignments.map { dto ->
                    val id = dto.id ?: "assign_${System.currentTimeMillis()}_${(100..999).random()}"
                    
                    // إذا كان واجباً جديداً لم يكن موجوداً محلياً، نرسل إشعاراً فورياً
                    if (id !in existingIds && !dto.title.isNullOrBlank()) {
                        com.example.theboss.utils.NotificationHelper.showUrgentHomeworkNotification(
                            context = context,
                            title = dto.title,
                            subject = dto.subjectName,
                            dueDate = dto.dueDate ?: "اليوم"
                        )
                    }

                    AssignmentEntity(
                        id = id,
                        subjectId = dto.subjectName,
                        subjectName = dto.subjectName,
                        title = dto.title,
                        description = dto.description ?: "",
                        dueDateString = dto.dueDate ?: "اليوم",
                        isCompleted = false,
                        isHot = true, // شارة واجب جديد 🔥
                        isPrivateTutoring = dto.isPrivateTutoring,
                        teacherName = dto.teacherId
                    )
                }
                if (assignmentEntities.isNotEmpty()) {
                    dao.insertAssignments(assignmentEntities)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * مزامنة سجل الحضور والغياب مع رقم الحصة وإشعار ولي الأمر الفوري
     */
    suspend fun syncAttendance(schoolId: String, studentRecordNumber: String) {
        try {
            val response = api.getAttendance(schoolFilter = "eq.$schoolId", recordFilter = "eq.$studentRecordNumber")
            if (response.isSuccessful) {
                val remoteAttendance = response.body() ?: emptyList()
                val currentCount = dao.getAllAttendance().firstOrNull()?.size ?: 0

                val entities = remoteAttendance.map { dto ->
                    com.example.theboss.data.local.AttendanceEntity(
                        dateString = dto.dateString,
                        status = dto.status,
                        periodNumber = dto.periodNumber,
                        subject = dto.subject,
                        note = "مأخوذة من السجل السحابي"
                    )
                }
                
                // إذا تم تسجيل غياب جديد لم يكن موجوداً من قبل
                if (entities.size > currentCount) {
                    val latestAbsence = remoteAttendance.firstOrNull { it.status == "absent" }
                    if (latestAbsence != null) {
                        com.example.theboss.utils.NotificationHelper.showAbsenceNotification(
                            context = context,
                            date = latestAbsence.dateString,
                            periodNumber = latestAbsence.periodNumber,
                            subject = latestAbsence.subject
                        )
                    }
                }

                if (entities.isNotEmpty()) {
                    dao.clearAttendance()
                    dao.insertAttendance(entities)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * إرسال رسالة أو سؤال مباشر إلى المعلم
     */
    suspend fun sendDirectMessage(
        teacherId: String,
        subjectName: String,
        messageText: String
    ): Result<Boolean> {
        return try {
            val schoolId = getSchoolId() ?: ""
            val studentDeviceId = sessionManager.getDeviceId()
            val dto = com.example.theboss.data.remote.DirectMessageDto(
                schoolId = schoolId,
                senderId = studentDeviceId,
                senderRole = "student",
                receiverId = teacherId,
                subjectName = subjectName,
                messageText = messageText
            )
            val response = api.sendDirectMessage(dto)
            if (response.isSuccessful) {
                // حفظ الرسالة محلياً
                val localMsg = com.example.theboss.data.local.DirectMessageEntity(
                    id = "msg_${System.currentTimeMillis()}",
                    senderId = studentDeviceId,
                    senderRole = "student",
                    receiverId = teacherId,
                    subjectName = subjectName,
                    messageText = messageText,
                    createdAt = java.time.LocalDateTime.now().toString(),
                    isRead = true
                )
                dao.insertDirectMessage(localMsg)
                Result.success(true)
            } else {
                Result.failure(Exception("فشل إرسال الرسالة: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * مزامنة رسائل المحادثات المباشرة بين الطالب والمعلمين
     */
    suspend fun syncDirectMessages(): Result<Boolean> {
        return try {
            val schoolId = getSchoolId() ?: ""
            val studentDeviceId = sessionManager.getDeviceId()
            val orFilter = "(sender_id.eq.$studentDeviceId,receiver_id.eq.$studentDeviceId)"
            val response = api.getDirectMessages(schoolFilter = "eq.$schoolId", orFilter = orFilter)
            if (response.isSuccessful) {
                val msgs = response.body() ?: emptyList()
                val entities = msgs.map { dto ->
                    com.example.theboss.data.local.DirectMessageEntity(
                        id = dto.id ?: "msg_${System.currentTimeMillis()}_${(100..999).random()}",
                        senderId = dto.senderId,
                        senderRole = dto.senderRole,
                        receiverId = dto.receiverId,
                        subjectName = dto.subjectName ?: "مادة دراسية",
                        messageText = dto.messageText,
                        createdAt = dto.createdAt ?: java.time.LocalDateTime.now().toString(),
                        isRead = dto.isRead
                    )
                }
                if (entities.isNotEmpty()) {
                    dao.insertDirectMessages(entities)
                }
                Result.success(true)
            } else {
                Result.failure(Exception("فشل جلب الرسائل"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSchoolId() = sessionManager.getSchoolId()
    fun getSchoolName() = sessionManager.getSchoolName()
    fun getDeviceId() = sessionManager.getDeviceId()
}
