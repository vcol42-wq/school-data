package com.principal.system.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.principal.system.data.local.*
import com.principal.system.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrincipalRepository @Inject constructor(
    private val api: PrincipalSupabaseApi,
    private val dao: PrincipalDao,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) {
    val teachers = dao.getAllTeachersOverview()
    val criticalAbsences = dao.getCriticalAbsences()
    val broadcasts = dao.getAllBroadcasts()

    fun getTodayAttendanceSummary(dateString: String) = dao.getAttendanceSummaryForDate(dateString)
    fun getRecentAttendanceSummaries() = dao.getRecentAttendanceSummaries()

    fun isConfigured(): Boolean = sessionManager.isConfigured()
    fun getSchoolName(): String = sessionManager.getSchoolName() ?: "إدارة المدرسة"
    fun getSchoolCode(): String = sessionManager.getSchoolCode() ?: ""

    /**
     * الاقتران التلقائي عبر مسح رمز QR المنشأ من تطبيق سطح المكتب
     */
    suspend fun pairWithQr(qrContent: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(qrContent, mapType)

            val schoolId = data["schoolId"]?.toString() ?: data["school_id"]?.toString() ?: ""
            val schoolName = data["schoolName"]?.toString() ?: data["school_name"]?.toString() ?: "مدرستي"
            val schoolCode = data["schoolCode"]?.toString() ?: data["school_code"]?.toString() ?: ""
            val url = data["url"]?.toString() ?: data["supabaseUrl"]?.toString() ?: ""
            val apiKey = data["apiKey"]?.toString() ?: data["key"]?.toString() ?: ""

            if (schoolId.isNotEmpty() && url.isNotEmpty() && apiKey.isNotEmpty()) {
                sessionManager.saveSchoolCredentials(schoolId, schoolName, schoolCode, url, apiKey)
                Result.success(true)
            } else {
                Result.failure(Exception("رمز QR لا يحتوي على بيانات الربط الصحيحة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * الاقتران اليدوي
     */
    fun pairManual(
        schoolId: String,
        schoolName: String,
        schoolCode: String,
        url: String,
        apiKey: String
    ) {
        sessionManager.saveSchoolCredentials(schoolId, schoolName, schoolCode, url, apiKey)
    }

    /**
     * المزامنة الشاملة وحساب المؤشرات التنفيذية ونشاط المعلمين ونبض الحضور
     */
    suspend fun syncAllExecutiveMetrics(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getSchoolId() ?: return@withContext Result.failure(Exception("لم يتم ربط المدرسة"))
            val schoolFilter = "eq.$schoolId"
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            // 1. جلب البيانات الخام من السحابة
            val rawTeachers = try { api.getTeachers(schoolFilter).body() ?: emptyList() } catch (e: Exception) { emptyList() }
            val rawStudents = try { api.getStudents(schoolFilter).body() ?: emptyList() } catch (e: Exception) { emptyList() }
            val rawAssignments = try { api.getDailyAssignments(schoolFilter).body() ?: emptyList() } catch (e: Exception) { emptyList() }
            val rawAttendance = try { api.getAttendance(schoolFilter).body() ?: emptyList() } catch (e: Exception) { emptyList() }
            val rawGrades = try { api.getGrades(schoolFilter).body() ?: emptyList() } catch (e: Exception) { emptyList() }
            val rawMessages = try { api.getDirectMessages(schoolFilter).body() ?: emptyList() } catch (e: Exception) { emptyList() }

            // 2. تحليل نشاط المعلمين (Teacher Engagement Metrics)
            val teacherOverviews = rawTeachers.map { teacher ->
                val teacherAssignments = rawAssignments.filter { it.teacherId == teacher.teacherId || it.teacherId == teacher.fullName }
                val latestAssignment = teacherAssignments.maxByOrNull { it.createdAt ?: "" }

                val teacherGrades = rawGrades.filter { it.teacherId == teacher.teacherId }
                val expectedStudentsCount = rawStudents.size
                val enteredCount = teacherGrades.size
                val completionRate = if (expectedStudentsCount > 0) {
                    (enteredCount.toFloat() / expectedStudentsCount.toFloat()).coerceIn(0f, 1f)
                } else 0f

                val pendingMsgCount = rawMessages.count {
                    it.receiverId == teacher.teacherId && it.senderRole == "student" && !it.isRead
                }

                TeacherOverviewEntity(
                    id = teacher.teacherId,
                    fullName = teacher.fullName,
                    subject = teacherAssignments.firstOrNull()?.subjectName ?: "معلم مادة",
                    assignedClasses = teacherAssignments.map { "${it.className}(${it.section})" }.distinct().take(3).joinToString(", "),
                    lastAssignmentTitle = latestAssignment?.title,
                    lastAssignmentDate = latestAssignment?.dueDate ?: latestAssignment?.createdAt?.take(10),
                    gradesEnteredCount = enteredCount,
                    totalStudentsExpected = expectedStudentsCount,
                    gradeCompletionRate = completionRate,
                    pendingMessagesCount = pendingMsgCount
                )
            }

            if (teacherOverviews.isNotEmpty()) {
                dao.clearTeachers()
                dao.insertTeachers(teacherOverviews)
            }

            // 3. تحليل نبض الحضور اليومي والحصص (Attendance Pulse)
            val todayAttendance = rawAttendance.filter { it.dateString == todayDateStr && it.status == "absent" }
            val distinctAbsentStudentsToday = todayAttendance.map { it.studentRecordNumber }.distinct().size
            val totalStudentsCount = rawStudents.size
            val attendanceRate = if (totalStudentsCount > 0) {
                (((totalStudentsCount - distinctAbsentStudentsToday).toFloat() / totalStudentsCount.toFloat()) * 100f).coerceIn(0f, 100f)
            } else 0f

            val summaryEntity = AttendanceSummaryEntity(
                dateString = todayDateStr,
                totalStudents = totalStudentsCount,
                absentStudentsCount = distinctAbsentStudentsToday,
                attendanceRatePercent = attendanceRate,
                period1Absences = todayAttendance.count { it.periodNumber == 1 },
                period2Absences = todayAttendance.count { it.periodNumber == 2 },
                period3Absences = todayAttendance.count { it.periodNumber == 3 },
                period4Absences = todayAttendance.count { it.periodNumber == 4 },
                period5Absences = todayAttendance.count { it.periodNumber == 5 },
                period6Absences = todayAttendance.count { it.periodNumber == 6 },
                period7Absences = todayAttendance.count { it.periodNumber == 7 }
            )
            dao.insertAttendanceSummary(summaryEntity)

            // 4. كشف المنطقة الحمراء للغيابات الحرجة (Critical Absences >= 3 أيام)
            val studentAbsenceGroups = rawAttendance
                .filter { it.status == "absent" }
                .groupBy { it.studentRecordNumber }

            val criticalList = studentAbsenceGroups.mapNotNull { (recordNum, records) ->
                val daysCount = records.map { it.dateString }.distinct().size
                if (daysCount >= 3) {
                    val stdInfo = rawStudents.find { it.recordNumber == recordNum }
                    CriticalStudentAbsenceEntity(
                        studentRecordNumber = recordNum,
                        fullName = stdInfo?.fullName ?: "طالب قيد #$recordNum",
                        className = stdInfo?.className ?: "غير محدد",
                        section = stdInfo?.section ?: "عام",
                        totalAbsenceDays = daysCount,
                        guardianPhone = stdInfo?.parentPhone ?: "غير مسجل",
                        lastAbsentDate = records.maxByOrNull { it.dateString }?.dateString ?: todayDateStr
                    )
                } else null
            }.sortedByDescending { it.totalAbsenceDays }

            val currentCriticalCount = dao.getCriticalAbsences().firstOrNull()?.size ?: 0
            if (criticalList.size > currentCriticalCount && criticalList.isNotEmpty()) {
                val mostCritical = criticalList.first()
                com.principal.system.utils.PrincipalNotificationHelper.showCriticalAbsenceAlert(
                    context = context,
                    studentName = mostCritical.fullName,
                    className = mostCritical.className,
                    daysCount = mostCritical.totalAbsenceDays
                )
            }

            dao.clearCriticalAbsences()
            if (criticalList.isNotEmpty()) {
                dao.insertCriticalAbsences(criticalList)
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * إرسال تعميم إداري عاجل
     */
    suspend fun dispatchBroadcast(
        title: String,
        message: String,
        targetAudience: String,
        priority: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getSchoolId() ?: return@withContext Result.failure(Exception("لم يتم ربط المدرسة"))
            val dto = SupabaseDirectMessageDto(
                schoolId = schoolId,
                senderId = "principal_admin",
                senderRole = "principal",
                receiverId = targetAudience,
                subjectName = "تعميم إداري: $title",
                messageText = "[$priority] $message",
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            )
            val response = api.sendBroadcastMessage(dto)
            if (response.isSuccessful) {
                val entity = BroadcastEntity(
                    id = "bcast_${System.currentTimeMillis()}",
                    title = title,
                    message = message,
                    targetAudience = targetAudience,
                    priority = priority,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                )
                dao.insertBroadcast(entity)
                Result.success(true)
            } else {
                Result.failure(Exception("فشل إرسال التعميم عبر السحابة: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
