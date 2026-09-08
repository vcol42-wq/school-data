package com.school.system.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.system.data.SyncManager
import com.school.system.data.dao.StudentDao
import com.school.system.data.dao.ColumnSettingDao
import com.school.system.data.dao.DailyColumnDao
import com.school.system.data.dao.AbsenceDao
import com.school.system.data.dao.ConfigDao
import com.school.system.data.model.Student
import com.school.system.data.model.StudentMarks
import com.school.system.data.model.DailyColumnSetting
import com.school.system.data.model.AbsenceRecord
import com.school.system.data.model.latestRecordedScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import com.school.system.data.local.SecureKeyStorage
import com.school.system.data.repository.GradesRepository
import com.school.system.data.repository.SecureUploadResult
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round

@HiltViewModel
class GradeViewModel @Inject constructor(
    private val studentDao: StudentDao,
    private val columnSettingDao: ColumnSettingDao,
    private val dailyColumnDao: DailyColumnDao,
    private val absenceDao: AbsenceDao,
    val configDao: ConfigDao,
    private val syncManager: SyncManager,
    private val secureKeyStorage: SecureKeyStorage,
    private val gradesRepository: GradesRepository
) : ViewModel() {

    val config = configDao.getConfig().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _absences = MutableStateFlow<List<AbsenceRecord>>(emptyList())
    val absences: StateFlow<List<AbsenceRecord>> = _absences

    fun loadStudents(grade: String, section: String, subject: String) {
        viewModelScope.launch {
            studentDao.getStudentsForClass(grade, section, subject).collectLatest { list ->
                val collator = java.text.Collator.getInstance(java.util.Locale("ar")).apply {
                    strength = java.text.Collator.PRIMARY
                }
                val cleanedList = list.map { s ->
                    val recalculated = calculateTotals(s.marks, s.subject)
                    if (recalculated != s.marks) s.copy(marks = recalculated) else s
                }
                _students.value = cleanedList.sortedWith { s1, s2 ->
                    val n1 = s1.fullName.trim().replace("^\\d+[\\.\\-\\s]+".toRegex(), "")
                    val n2 = s2.fullName.trim().replace("^\\d+[\\.\\-\\s]+".toRegex(), "")
                    collator.compare(n1, n2)
                }
            }
        }
        viewModelScope.launch {
            absenceDao.getAllAbsences().collectLatest {
                _absences.value = it
            }
        }
    }

    fun getDailyColumnSettings(id: String) = dailyColumnDao.getSettings(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateDailyColumnSettings(setting: DailyColumnSetting) {
        viewModelScope.launch {
            dailyColumnDao.insertSetting(setting)
        }
    }

    fun updateStudentMarks(student: Student, marks: StudentMarks) {
        if (config.value?.role == "supervisor") {
            // وضع القراءة فقط للمشرف: ممنوع تعديل الدرجات
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val updatedMarks = calculateTotals(marks, student.subject)
            val updatedStudent = student.copy(marks = updatedMarks)
            // Instant, rock-solid local database persistence
            studentDao.updateStudent(updatedStudent)
        }
    }

    fun calculateTotals(marks: StudentMarks, subject: String): StudentMarks {
        val u = marks.copy()
        
        val isSpecial = isSpecialSubject(subject)

        val hasM1 = u.m1Written > 0f || u.m1Daily.any { it > 0f }
        val hasM2 = u.m2Written > 0f || u.m2Daily.any { it > 0f }
        val hasMidterm = u.midtermScore > 0f || u.midtermOral.any { it > 0f }
        val hasM3 = u.m3Written > 0f || u.m3Daily.any { it > 0f }
        val hasM4 = u.m4Written > 0f || u.m4Daily.any { it > 0f }
        val hasFinalExam = (u.finalWrittenD1 > 0f) || (u.finalWrittenD2 != null && u.finalWrittenD2!! > 0f)

        if (isSpecial) {
            // Month 1-4 Total = Sum(daily) + written (No division)
            u.m1MonthAvg = if (hasM1) (u.m1Daily.sum() + u.m1Written) else 0f
            u.m2MonthAvg = if (hasM2) (u.m2Daily.sum() + u.m2Written) else 0f
            u.m3MonthAvg = if (hasM3) (u.m3Daily.sum() + u.m3Written) else 0f
            u.m4MonthAvg = if (hasM4) (u.m4Daily.sum() + u.m4Written) else 0f
            
            // Midterm for Special: Sum(Oral) + Score
            u.midtermTotal = if (hasMidterm) (u.midtermOral.sum() + u.midtermScore) else 0f
            u.midtermFinalGrade = u.midtermTotal

            // Final Exam for Special: Sum(FinalOral) + FinalWrittenD1
            u.finalExamTotal = if (hasFinalExam) (u.finalOral.sum() + u.finalWrittenD1) else 0f
        } else {
            // Other subjects: (Sum(daily) + written) / 2
            u.m1MonthAvg = if (hasM1) round((u.m1Daily.sum() + u.m1Written) / 2f) else 0f
            u.m2MonthAvg = if (hasM2) round((u.m2Daily.sum() + u.m2Written) / 2f) else 0f
            u.m3MonthAvg = if (hasM3) round((u.m3Daily.sum() + u.m3Written) / 2f) else 0f
            u.m4MonthAvg = if (hasM4) round((u.m4Daily.sum() + u.m4Written) / 2f) else 0f
            
            // Other subjects Midterm: Just Written Score
            u.midtermFinalGrade = if (hasMidterm) u.midtermScore else 0f
            u.midtermTotal = u.midtermFinalGrade

            // Other subjects Final: Just Written D1
            u.finalExamTotal = if (hasFinalExam) u.finalWrittenD1 else 0f
        }
        
        // فص1 = (M1 + M2) / 2 (يحسب فقط عند إدخال الشهر الثاني)
        u.term1Avg = if (hasM2 && hasM1) {
            round((u.m1MonthAvg + u.m2MonthAvg) / 2f)
        } else if (hasM2) {
            u.m2MonthAvg
        } else {
            0f
        }

        // فص2 = (M3 + M4) / 2 (يحسب فقط عند إدخال الشهر الرابع)
        u.term2Avg = if (hasM4 && hasM3) {
            round((u.m3MonthAvg + u.m4MonthAvg) / 2f)
        } else if (hasM4) {
            u.m4MonthAvg
        } else {
            0f
        }

        // Annual Effort (Sae'i) = (Term1 + MidtermFinal + Term2) / 3 (يحسب فقط عند اكتمال الفصلين ونصف السنة)
        u.annualAverage = if (u.term1Avg > 0f && u.midtermFinalGrade > 0f && u.term2Avg > 0f) {
            round((u.term1Avg + u.midtermFinalGrade + u.term2Avg) / 3f)
        } else {
            0f
        }

        // Final Grade Calculation (تحسب فقط عند توفر السعي السنوي والامتحان النهائي)
        if (hasFinalExam && u.annualAverage > 0f) {
            val d2 = u.finalWrittenD2
            if (d2 != null && d2 > 0f) {
                val d2ExamTotal = if (isSpecial) (u.finalOral.sum() + d2) else d2
                u.finalGrade = round((d2ExamTotal + u.annualAverage) / 2f)
            } else {
                u.finalGrade = round((u.finalExamTotal + u.annualAverage) / 2f)
            }
        } else {
            u.finalGrade = 0f
        }
        
        val activeScore = u.latestRecordedScore()
        u.result = if (activeScore >= 50f) "ناجح" else "مكمل/راسب"
        
        return u
    }

    fun isSpecialSubject(subject: String): Boolean {
        val s = subject.trim().lowercase()
        return s.contains("عرب") ||
               s.contains("انكل") || s.contains("إنكل") ||
               s.contains("انجلي") || s.contains("إنجلي") ||
               s.contains("انكلش") || s.contains("انجلش") ||
               s.contains("english") || s.contains("engl") || s == "e" || s == "eng" || s == "en" || s == "el" ||
               s.contains("اسلام") || s.contains("إسلام") ||
               s.contains("قرآن") || s.contains("قران") ||
               s.contains("دين") ||
               s.contains("فرنس") || s.contains("french") || s == "f" ||
               s.contains("كرد") || s.contains("kurd") ||
               s.contains("تركم") || s.contains("turk") ||
               s.contains("سريان") || s.contains("syriac") ||
               s.contains("المان") || s.contains("ألمان") || s.contains("german") ||
               s.contains("اسبان") || s.contains("إسبان") || s.contains("spanish") ||
               s.contains("لغة") || s.contains("لغات")
    }

    fun addMockStudent(grade: String, section: String, subject: String, name: String) {
        viewModelScope.launch {
            studentDao.insertStudent(
                Student(
                    recordNumber = (1000..9999).random().toString(),
                    fullName = name,
                    grade = grade,
                    section = section,
                    subject = subject
                )
            )
            syncManager.propagateStudents()
        }
    }

    fun importMultipleStudents(
        grade: String,
        section: String,
        subject: String,
        rawNamesText: String,
        onDone: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val lines = rawNamesText.split(Regex("[\r\n;,|]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.contains("الاسم") && !it.contains("تسلسل") && !it.contains("اسم الطالب") }

            var count = 0
            lines.forEach { name ->
                val cleanName = name.replace(Regex("^[0-9]+[\\.\\-\\s]+"), "").trim()
                if (cleanName.isNotEmpty()) {
                    studentDao.insertStudent(
                        Student(
                            recordNumber = (1000..9999).random().toString(),
                            fullName = cleanName,
                            grade = grade,
                            section = section,
                            subject = subject
                        )
                    )
                    count++
                }
            }
            if (count > 0) {
                syncManager.propagateStudents()
                syncManager.syncGrades(grade, section, subject)
            }
            onDone(count)
        }
    }

    fun updateStudentName(student: Student, newName: String) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            studentDao.updateStudent(student.copy(fullName = trimmedName))
            if (student.recordNumber.isNotEmpty()) {
                studentDao.updateStudentNameForAllSubjects(student.grade, student.section, student.recordNumber, trimmedName)
            }
        }
    }

    suspend fun syncWithPrincipal(grade: String, section: String, subject: String): Boolean {
        return syncManager.syncGrades(grade, section, subject)
    }

    fun summonRosterForThisClass(grade: String, section: String, subject: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.summonClassRosterDetailed(grade, section, subject)
            onResult(result.success, result.message)
        }
    }

    fun syncGradesOnly(grade: String, section: String, subject: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = syncManager.syncGrades(grade, section, subject)
            if (success) {
                onResult(true, "تم رفع ومزامنة درجات الشعبة للسحابة بنجاح ☁️✓")
            } else {
                onResult(false, "فشل رفع الدرجات. يرجى التأكد من الاتصال بالسحابة.")
            }
        }
    }

    fun getStoredPin(grade: String, section: String, subject: String): String? {
        val key = secureKeyStorage.buildSubjectKey(grade, section, subject)
        return secureKeyStorage.getSubjectPin(key)
    }

    fun savePin(grade: String, section: String, subject: String, pin: String) {
        val key = secureKeyStorage.buildSubjectKey(grade, section, subject)
        secureKeyStorage.saveSubjectPin(key, pin)
    }

    fun clearPin(grade: String, section: String, subject: String) {
        val key = secureKeyStorage.buildSubjectKey(grade, section, subject)
        secureKeyStorage.clearSubjectPin(key)
    }

    fun getSavedSupervisorCode(): String? = secureKeyStorage.getSupervisorCode()

    fun uploadGradesSecurely(
        grade: String,
        section: String,
        subject: String,
        secretPin: String,
        onResult: (SecureUploadResult) -> Unit
    ) {
        if (config.value?.role == "supervisor") {
            onResult(SecureUploadResult.Failure("غير مصرح: حساب المشرف مخصص للاطلاع والتوجيه فقط ولا يمكنه تعديل أو رفع الدرجات."))
            return
        }
        viewModelScope.launch {
            val result = gradesRepository.uploadGradesSecurely(grade, section, subject, secretPin)
            onResult(result)
        }
    }

    fun saveAllMarksLocally(onResult: (String) -> Unit) {
        if (config.value?.role == "supervisor") {
            onResult("تنبيه: حساب المشرف في وضع القراءة فقط ولا يمكنه حفظ أو تعديل درجات.")
            return
        }
        viewModelScope.launch {
            val currentList = _students.value
            currentList.forEach { student ->
                val calculated = calculateTotals(student.marks, student.subject)
                studentDao.updateStudent(student.copy(marks = calculated))
            }
            onResult("تم حفظ وتثبيت كافة درجات الطلاب محلياً بنجاح 💾✓")
        }
    }

    fun toggleAbsence(student: Student, dateString: String, periodNumber: Int = 1, isAbsent: Boolean) {
        viewModelScope.launch {
            if (isAbsent) {
                absenceDao.insertAbsence(AbsenceRecord(studentId = student.id, dateString = dateString, periodNumber = periodNumber))
            } else {
                absenceDao.deleteAbsence(student.id, dateString, periodNumber)
            }
        }
    }

    fun toggleDailyAbsence(student: Student, dateString: String, isAbsent: Boolean) {
        viewModelScope.launch {
            if (isAbsent) {
                absenceDao.insertAbsence(AbsenceRecord(studentId = student.id, dateString = dateString, periodNumber = 0))
            } else {
                absenceDao.deleteAbsence(student.id, dateString)
            }
        }
    }

    fun syncAndArchiveAbsences(grade: String, section: String, subject: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val success = syncManager.syncGrades(grade, section, subject)
            if (success) {
                val cutoffDate = java.time.LocalDate.now().minusDays(30).toString()
                val expiredCounts = absenceDao.getExpiredAbsencesCount(cutoffDate)
                for (item in expiredCounts) {
                    val student = studentDao.getStudentById(item.studentId)
                    if (student != null) {
                        studentDao.updateStudent(student.copy(
                            historicalAbsences = student.historicalAbsences + item.count
                        ))
                    }
                }
                absenceDao.deleteAbsencesOlderThan(cutoffDate)
                // Propagate students in case new packages were added
                syncManager.propagateStudents()
                onComplete("تمت المزامنة بنجاح وأرشفة غيابات ما قبل 30 يوم!")
            } else {
                onComplete("فشلت المزامنة!")
            }
        }
    }
}
