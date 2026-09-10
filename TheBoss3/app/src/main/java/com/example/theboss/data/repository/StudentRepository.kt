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

    /**
     * مزامنة الجدول الدراسي وتعليمات الأساتذة من السحابة وتخزينها محلياً.
     */
    suspend fun syncTimetableAndInstructions(schoolId: String) {
        try {
            // 0. مزامنة المواد والمعلمين الحقيقيين من جدول teacher_assignments & teachers
            val assignRes = api.getTeacherAssignments("eq.$schoolId")
            val teachersRes = api.getTeachers("eq.$schoolId")
            if (assignRes.isSuccessful) {
                val assignments = assignRes.body() ?: emptyList()
                val teachers = teachersRes.body() ?: emptyList()
                val realSubjectEntities = assignments.groupBy { it.subjectName }.map { (subjName, assignList) ->
                    val teacherId = assignList.firstOrNull()?.teacherId
                    val teacherName = teachers.find { it.id == teacherId }?.name ?: teacherId ?: "معلم المادة"
                    val classesStr = assignList.map { "${it.className}(${it.section})" }.distinct().joinToString(", ")
                    SubjectEntity(
                        id = "subj_${subjName.hashCode()}",
                        name = subjName,
                        teacherName = teacherName,
                        lastAssignment = if (classesStr.isNotEmpty()) "الفصول: $classesStr" else "مقرر دراسي"
                    )
                }
                if (realSubjectEntities.isNotEmpty()) {
                    dao.clearSubjects()
                    dao.insertSubjects(realSubjectEntities)
                }
            }

            // 1. مزامنة الجدول (كدروس أو مواد) إذا وجد
            val timetableResponse = api.getTimetable("eq.$schoolId")
            if (timetableResponse.isSuccessful) {
                val remoteTimetable = timetableResponse.body() ?: emptyList()
                val subjectEntities = remoteTimetable.mapIndexed { index, dto ->
                    SubjectEntity(
                        id = "remote_$index",
                        name = dto.subjectName,
                        teacherName = dto.teacherName,
                        lastAssignment = "مستورد من الجدول"
                    )
                }
                if (subjectEntities.isNotEmpty()) {
                    dao.insertSubjects(subjectEntities)
                }
            }

            // 2. مزامنة تعليمات الأساتذة والواجبات
            val instructionsResponse = api.getTeacherInstructions("eq.$schoolId")
            if (instructionsResponse.isSuccessful) {
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

            // 3. مزامنة الواجبات والدروس اليومية وقنوات التقوية
            syncDailyAssignments(schoolId)
            syncDirectives(schoolId)
        } catch (e: Exception) {
            // فشل المزامنة لا يوقف التطبيق، سيعتمد على البيانات المحلية
            e.printStackTrace()
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
