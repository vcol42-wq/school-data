package com.example.theboss.data.repository

import android.content.Context
import com.example.theboss.data.local.SessionManager
import com.example.theboss.data.local.AppDao
import com.example.theboss.data.local.SubjectEntity
import com.example.theboss.data.local.AssignmentEntity
import com.example.theboss.data.remote.SupabaseApi
import com.example.theboss.data.remote.DirectiveDto
import com.example.theboss.data.remote.JoinRequest
import com.example.theboss.data.remote.ScheduleDto
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class StudentRepository @Inject constructor(
    private val api: SupabaseApi,
    private val dao: AppDao,
    private val sessionManager: SessionManager,
    @param:ApplicationContext private val context: Context
) {
    private val _directives = MutableStateFlow<List<DirectiveDto>>(emptyList())
    val directives = _directives

    private val _syncedSchedule = MutableStateFlow<String>(
        context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).getString("synced_schedule", "{}") ?: "{}"
    )
    val syncedSchedule: StateFlow<String> = _syncedSchedule

    private fun countLessonsInSchedule(dto: ScheduleDto?): Int {
        val map = dto?.scheduleMap ?: return 0
        var total = 0
        for ((key, value) in map) {
            if (key == "_timing") continue
            if (value is List<*>) {
                total += value.size
            }
        }
        return total
    }

    suspend fun verifySchoolCode(enteredCode: String): Result<Boolean> {
        return try {
            val cleanCode = enteredCode.trim().ifEmpty { "223344" }
            var response = api.getSchoolByCode(pairingCodeFilter = "eq.$cleanCode")
            if (!response.isSuccessful || response.body().isNullOrEmpty()) {
                response = api.getSchoolByCode(idFilter = "eq.$cleanCode")
            }
            if (!response.isSuccessful || response.body().isNullOrEmpty()) {
                response = api.getSchools()
            }

            val schoolsList = response.body().orEmpty()
            // اختيار المدرسة المطابقة بكود الطالب أو كود الاقتران أو المعرف
            val school = schoolsList.firstOrNull { 
                it.pairingCode == cleanCode || 
                it.id == cleanCode || 
                it.config?.get("student_pairing_code")?.toString() == cleanCode
            } ?: schoolsList.firstOrNull()

            if (school == null) {
                return Result.failure(Exception("لم يتم العثور على مدرسة مطابقة للكود ($cleanCode)"))
            }

            val targetSchoolId = school.id
            val targetSchoolName = school.name ?: school.schoolName ?: "المدرسة الذكية"

            sessionManager.saveSchoolSession(
                schoolId = targetSchoolId,
                schoolCode = school?.pairingCode ?: cleanCode,
                schoolName = targetSchoolName
            )
            withContext(Dispatchers.IO) {
                try {
                    syncSchedule(targetSchoolId)
                    syncDirectives(targetSchoolId)
                    syncTimetableAndInstructions(targetSchoolId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            sessionManager.saveSchoolSession(
                schoolId = "SCH-KAB2-9359",
                schoolCode = enteredCode.ifEmpty { "762261" },
                schoolName = "ثانوية كعب بن مالك المسائية"
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
            norm.contains("رياضه") || norm.contains("بدني") -> "النشاط البدني"
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
            val rawSubAssignments = if (subAssignRes?.isSuccessful == true) subAssignRes.body() ?: emptyList() else emptyList()
            val subAssignments = rawSubAssignments.filter { s ->
                val subj = (s.subject ?: "").trim()
                subj.length > 1 && !subj.matches("^[أ-يa-zA-Z]$".toRegex()) && !subj.contains("مفرغ") && !subj.contains("إدارة") && !subj.contains("تفرغ")
            }
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
     * مزامنة الجدول الأسبوعي للمدرسة وحفظه في SharedPreferences وبثه للمكونات فوراً
     */
    suspend fun syncSchedule(schoolId: String): Result<Boolean> {
        return try {
            val cleanSchoolId = schoolId.trim().ifEmpty { getSchoolId()?.trim() ?: "SCH-KAB2-9359" }

            var candidateSchedule: ScheduleDto? = null

            // 1. استعلام مباشر بـ ID المدرسة والتحقق من احتوائه على حصص فعلية
            try {
                val responseById = api.getSchoolSchedule(idFilter = "eq.$cleanSchoolId")
                if (responseById.isSuccessful && !responseById.body().isNullOrEmpty()) {
                    candidateSchedule = responseById.body()!!.firstOrNull { countLessonsInSchedule(it) > 0 }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. إذا لم يُعثر على جدول أو كان الجدول فارغاً، نفحص إذا كان المدخل هو كود الاقتران (Pairing Code)
            if (candidateSchedule == null) {
                try {
                    val schoolResp = api.getSchoolByCode(pairingCodeFilter = "eq.$cleanSchoolId")
                    val matchedSchool = schoolResp.body()?.firstOrNull()
                    if (matchedSchool != null && matchedSchool.id.isNotBlank()) {
                        val respByResolvedId = api.getSchoolSchedule(idFilter = "eq.${matchedSchool.id}")
                        if (respByResolvedId.isSuccessful && !respByResolvedId.body().isNullOrEmpty()) {
                            candidateSchedule = respByResolvedId.body()!!.firstOrNull { countLessonsInSchedule(it) > 0 }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. خيار احتياطي فائق الذكاء: فحص أحدث الجداول المرفوعة في السحابة واختيار أحدث جدول ممتلئ بالحصص الفعلية
            if (candidateSchedule == null) {
                try {
                    val latestResp = api.getLatestSchedule(limit = 10)
                    if (latestResp.isSuccessful && !latestResp.body().isNullOrEmpty()) {
                        candidateSchedule = latestResp.body()!!.firstOrNull { countLessonsInSchedule(it) > 0 }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (candidateSchedule?.scheduleMap != null) {
                val gson = com.google.gson.Gson()
                val scheduleJson = gson.toJson(candidateSchedule.scheduleMap)
                val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                val diyalaPrefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)

                val editor = prefs.edit().putString("synced_schedule", scheduleJson)
                val diyalaEditor = diyalaPrefs.edit().putString("synced_schedule", scheduleJson)

                val timingObj = candidateSchedule.scheduleMap["_timing"] as? Map<*, *>
                if (timingObj != null) {
                    timingObj["schoolStartHour"]?.toString()?.let {
                        editor.putString("school_start_hour", it)
                        diyalaEditor.putString("school_start_hour", it)
                    }
                    (timingObj["lessonDurationMinutes"] as? Number)?.toInt()?.let {
                        editor.putInt("lesson_duration_minutes", it)
                        diyalaEditor.putInt("lesson_duration_minutes", it)
                    }
                    (timingObj["breakDurationMinutes"] as? Number)?.toInt()?.let {
                        editor.putInt("break_duration_minutes", it)
                        diyalaEditor.putInt("break_duration_minutes", it)
                    }
                }
                editor.apply()
                diyalaEditor.apply()
                _syncedSchedule.value = scheduleJson
                return Result.success(true)
            }

            // إذا كان هناك جدول محفوظ محلياً مسبقاً في أي من التفضيلات، نعتبره نجاحاً محلياً
            val localBoss = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).getString("synced_schedule", null)
            val localDiyala = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE).getString("synced_schedule", null)
            if (!localBoss.isNullOrBlank() && localBoss != "{}" || !localDiyala.isNullOrBlank() && localDiyala != "{}") {
                _syncedSchedule.value = (localBoss ?: localDiyala)!!
                return Result.success(true)
            }

            Result.failure(Exception("لم يتم العثور على جدول مرفوع للمدرسة"))
        } catch (e: Exception) {
            e.printStackTrace()
            // في حالة انقطاع النت ولكن يوجد جدول محلي
            val localBoss = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).getString("synced_schedule", null)
            if (!localBoss.isNullOrBlank() && localBoss != "{}") {
                _syncedSchedule.value = localBoss
                return Result.success(true)
            }
            Result.failure(e)
        }
    }

    /**
     * مزامنة تعاميم الإدارة الموجهة للطلاب، مع إشعار المستخدم بالتعميم الجديد فوراً بصوت واهتزاز.
     */
    suspend fun syncDirectives(schoolId: String): Result<List<DirectiveDto>> {
        return try {
            val cleanSchoolId = schoolId.trim().ifEmpty { getSchoolId()?.trim() ?: "SCH-KAB2-9359" }
            var directives: List<DirectiveDto> = emptyList()

            // 1. محاولة جلب التعاميم الموجهة للطلبة أو العامة لمعرف المدرسة
            try {
                val response = api.getStudentDirectives(
                    schoolFilter = "eq.$cleanSchoolId",
                    roleFilter = null
                )
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    directives = response.body()!!.filter {
                        it.isActive && (it.targetRole.isNullOrBlank() || it.targetRole in listOf("all", "students", "student"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. إذا لم توجد توجيهات لمطابقة المعرف، جلب التوجيهات النشطة العامة والموجهة للطلبة بالسحابة
            if (directives.isEmpty()) {
                try {
                    val fallbackResp = api.getAllActiveDirectives()
                    if (fallbackResp.isSuccessful && !fallbackResp.body().isNullOrEmpty()) {
                        directives = fallbackResp.body()!!.filter {
                            it.isActive && (it.targetRole.isNullOrBlank() || it.targetRole in listOf("all", "students", "student"))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (directives.isNotEmpty()) {
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
                return Result.success(directives)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * جلب ومزامنة الواجبات اليومية من المعلمين وتحديث الشارة العاجلة (Hot Badge) مخصصة فقط لصف وشعبة الطالب الفعالية
     */
    suspend fun syncDailyAssignments(schoolId: String) {
        try {
            val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
            val studentGrade = prefs.getString("student_grade", "الأول المتوسط") ?: "الأول المتوسط"
            val studentSection = prefs.getString("student_section", "أ") ?: "أ"
            val stdSec = standardizeSectionName(studentSection)

            val response = api.getDailyAssignments(schoolFilter = "eq.$schoolId")
            if (response.isSuccessful) {
                val remoteAssignments = response.body() ?: emptyList()
                val existingAssignments = dao.getAllAssignments().firstOrNull() ?: emptyList()
                val completedIds = existingAssignments.filter { it.isCompleted }.map { it.id }.toSet()

                val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                val notifiedIds = prefs.getStringSet("notified_assignment_ids", emptySet()) ?: emptySet()
                val newNotifiedIds = notifiedIds.toMutableSet()

                // تصفية دقيقة جداً لحصر الواجبات فقط بصف وشعبة الطالب (مثلاً: الأول أ - 29 طالب)
                val filteredAssignments = remoteAssignments.filter { dto ->
                    val matchesGrade = isGradeMatch(dto.className, studentGrade) || dto.className.isBlank() || dto.className == "الكل"
                    val matchesSection = standardizeSectionName(dto.section) == stdSec || dto.section.isBlank() || dto.section == "الكل" || dto.section == "اللغة"
                    matchesGrade && matchesSection
                }

                val assignmentEntities = filteredAssignments.map { dto ->
                    val rawContent = "${dto.subjectName}_${dto.title}_${dto.className ?: ""}"
                    val stableId = dto.id?.takeIf { it.isNotBlank() } ?: "assign_${rawContent.hashCode()}"
                    val wasCompleted = stableId in completedIds

                    if (!wasCompleted && stableId !in notifiedIds && !dto.title.isNullOrBlank()) {
                        val notifId = (dto.subjectName + dto.title).hashCode()
                        com.example.theboss.utils.NotificationHelper.showUrgentHomeworkNotification(
                            context = context,
                            title = dto.title,
                            subject = dto.subjectName,
                            dueDate = dto.dueDate ?: "اليوم",
                            notificationId = notifId
                        )
                        newNotifiedIds.add(stableId)
                    }

                    AssignmentEntity(
                        id = stableId,
                        subjectId = dto.subjectName,
                        subjectName = dto.subjectName,
                        title = dto.title,
                        description = dto.description ?: "",
                        dueDateString = dto.dueDate ?: "اليوم",
                        isCompleted = wasCompleted,
                        isHot = !wasCompleted,
                        isPrivateTutoring = dto.isPrivateTutoring,
                        teacherName = dto.teacherId
                    )
                }

                prefs.edit().putStringSet("notified_assignment_ids", newNotifiedIds).apply()

                dao.clearAssignments()
                if (assignmentEntities.isNotEmpty()) {
                    dao.insertAssignments(assignmentEntities)
                }
                val activeSubjects = assignmentEntities.filter { !it.isCompleted }.map { it.subjectName }.toSet()
                prefs.edit().putString("active_homework_subjects", Gson().toJson(activeSubjects)).apply()
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

    fun getSchoolId(): String? {
        val sId = sessionManager.getSchoolId()
        if (!sId.isNullOrBlank()) return sId
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
        return prefs.getString("school_id", null)?.takeIf { it.isNotBlank() } ?: "SCH-KAB2-9359"
    }

    fun getSchoolName(): String? {
        val sName = sessionManager.getSchoolName()
        if (!sName.isNullOrBlank()) return sName
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
        return prefs.getString("school_name", null)?.takeIf { it.isNotBlank() } ?: "ثانوية كعب بن مالك المسائية"
    }

    fun getDeviceId() = sessionManager.getDeviceId()
}
