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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
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
    private val syncManager: SyncManager
) : ViewModel() {

    val config = configDao.getConfig().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _absences = MutableStateFlow<List<AbsenceRecord>>(emptyList())
    val absences: StateFlow<List<AbsenceRecord>> = _absences

    fun loadStudents(grade: String, section: String, subject: String) {
        viewModelScope.launch {
            studentDao.getStudentsForClass(grade, section, subject).collectLatest {
                _students.value = it
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
        viewModelScope.launch {
            val updatedMarks = calculateTotals(marks, student.subject)
            val updatedStudent = student.copy(marks = updatedMarks)
            studentDao.updateStudent(updatedStudent)
            
            // Real-time Cloud Sync
            syncManager.syncGrades(updatedStudent.grade, updatedStudent.section, updatedStudent.subject)
        }
    }

    private fun calculateTotals(marks: StudentMarks, subject: String): StudentMarks {
        val u = marks.copy()
        
        val isSpecial = isSpecialSubject(subject)

        if (isSpecial) {
            // Month 1 Total = Sum(daily) + written (No division per request)
            u.m1MonthAvg = u.m1Daily.sum() + u.m1Written
            
            // Month 2 Total
            u.m2MonthAvg = u.m2Daily.sum() + u.m2Written
            
            // Month 3 Total
            u.m3MonthAvg = u.m3Daily.sum() + u.m3Written
            
            // Month 4 Total
            u.m4MonthAvg = u.m4Daily.sum() + u.m4Written
        } else {
            // Other subjects: (Sum(daily) + written) / 2
            u.m1MonthAvg = round((u.m1Daily.sum() + u.m1Written) / 2f)
            u.m2MonthAvg = round((u.m2Daily.sum() + u.m2Written) / 2f)
            u.m3MonthAvg = round((u.m3Daily.sum() + u.m3Written) / 2f)
            u.m4MonthAvg = round((u.m4Daily.sum() + u.m4Written) / 2f)
        }
        
        // فص1 = (M1 + M2) / 2
        u.term1Avg = round((u.m1MonthAvg + u.m2MonthAvg) / 2)

        // Midterm = Activity (5 cols) + Score
        u.midtermTotal = u.midtermOral.sum() + u.midtermScore
        u.midtermFinalGrade = u.midtermTotal // Final reported grade for midterm

        // فص2 = (M3 + M4) / 2
        u.term2Avg = round((u.m3MonthAvg + u.m4MonthAvg) / 2)

        // Annual Effort (Sae'i) = (Term1 + MidtermFinal + Term2) / 3
        u.annualAverage = round((u.term1Avg + u.midtermFinalGrade + u.term2Avg) / 3)

        // Final Exam Score (الدور الأول) = Sum(finalOral) + finalWrittenD1
        u.finalExamTotal = u.finalOral.sum() + u.finalWrittenD1

        // Final Grade = (Second Round + Sae'i) / 2, or (First Round + Sae'i) / 2 if no Second Round
        val d2 = u.finalWrittenD2
        if (d2 != null && d2 > 0f) {
            u.finalGrade = round((d2 + u.annualAverage) / 2)
        } else {
            u.finalGrade = round((u.finalExamTotal + u.annualAverage) / 2)
        }
        
        u.result = if (u.finalGrade >= 50) "ناجح" else "مكمل/راسب"
        
        return u
    }

    private fun isSpecialSubject(subject: String): Boolean {
        val s = subject.trim()
        return s.contains("عرب") ||
               s.contains("انكل") ||
               s.contains("إنكل") ||
               s.contains("انجلي") ||
               s.contains("إنجلي") ||
               s.contains("english", ignoreCase = true) ||
               s.contains("اسلام") ||
               s.contains("إسلام")
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

    fun updateStudentName(student: Student, newName: String) {
        viewModelScope.launch {
            studentDao.updateStudentNameForAllSubjects(student.grade, student.section, student.recordNumber, newName)
        }
    }

    suspend fun syncWithPrincipal(grade: String, section: String, subject: String): Boolean {
        return syncManager.syncGrades(grade, section, subject)
    }

    fun toggleAbsence(student: Student, dateString: String, isAbsent: Boolean) {
        viewModelScope.launch {
            if (isAbsent) {
                absenceDao.insertAbsence(AbsenceRecord(studentId = student.id, dateString = dateString))
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
