package com.example.theboss.ui.workspace

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theboss.data.local.*
import com.example.theboss.utils.alarm.StudyAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class GpaCourse(
    val name: String,
    val grade: Double,
    val credits: Int
)

@HiltViewModel
class WorkspaceToolsViewModel @Inject constructor(
    private val workspaceDao: WorkspaceDao,
    private val alarmScheduler: StudyAlarmScheduler,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>> = workspaceDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = workspaceDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = workspaceDao.getUpcomingExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarms: StateFlow<List<AlarmEntity>> = workspaceDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _gpaCourses = MutableStateFlow<List<GpaCourse>>(emptyList())
    val gpaCourses: StateFlow<List<GpaCourse>> = _gpaCourses

    private val _calculatedGpa = MutableStateFlow(0.0)
    val calculatedGpa: StateFlow<Double> = _calculatedGpa

    // ==================== عمليات الملاحظات النصية ====================
    fun addTextNote(title: String, content: String) {
        viewModelScope.launch {
            val note = NoteEntity(title = title, content = content)
            workspaceDao.insertNote(note)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            workspaceDao.deleteNote(note)
        }
    }

    // ==================== عمليات المهام ====================
    fun addTask(title: String, dueDate: Long, priority: Int, subject: String?) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                dueDate = dueDate,
                priority = priority,
                subjectTag = subject,
                isCompleted = false
            )
            workspaceDao.insertTask(task)
        }
    }

    fun setTaskCompletion(taskId: Long, completed: Boolean) {
        viewModelScope.launch {
            workspaceDao.setTaskCompletion(taskId, completed)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            workspaceDao.deleteTask(task)
        }
    }

    // ==================== عمليات الامتحانات ====================
    fun addExam(subjectName: String, timestamp: Long, targetGrade: Int, notes: String?) {
        viewModelScope.launch {
            val exam = ExamEntity(
                subjectName = subjectName,
                examTimestamp = timestamp,
                targetGrade = targetGrade,
                notes = notes
            )
            workspaceDao.insertExam(exam)
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            workspaceDao.deleteExam(exam)
        }
    }

    // ==================== عمليات المنبهات والمؤقتات ====================
    fun addExactAlarm(title: String, message: String, triggerTime: Long) {
        viewModelScope.launch {
            val alarmId = Random.nextInt(1000, 99999)
            val alarm = AlarmEntity(
                id = alarmId,
                title = title,
                message = message,
                triggerTime = triggerTime
            )
            workspaceDao.insertAlarm(alarm)
            alarmScheduler.scheduleExactAlarm(alarmId, triggerTime, title, message)
        }
    }

    fun startPomodoro(subjectName: String, minutes: Int) {
        viewModelScope.launch {
            val alarmId = Random.nextInt(1000, 99999)
            val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
            val alarm = AlarmEntity(
                id = alarmId,
                title = "انتهاء جلسة بومودورو: $subjectName",
                message = "لقد أكملت جلسة المذاكرة بنجاح!",
                triggerTime = triggerTime,
                delayMinutes = minutes
            )
            workspaceDao.insertAlarm(alarm)
            alarmScheduler.startFocusTimer(alarmId, minutes, subjectName)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm.id)
            workspaceDao.deleteAlarm(alarm)
        }
    }

    // ==================== حاسبة المعدل GPA ====================
    fun addGpaCourse(name: String, grade: Double, credits: Int) {
        _gpaCourses.value = _gpaCourses.value + GpaCourse(name, grade, credits)
        calculateGpa()
    }

    fun removeGpaCourse(course: GpaCourse) {
        _gpaCourses.value = _gpaCourses.value - course
        calculateGpa()
    }

    fun clearGpaCourses() {
        _gpaCourses.value = emptyList()
        _calculatedGpa.value = 0.0
    }

    private fun calculateGpa() {
        val courses = _gpaCourses.value
        if (courses.isEmpty()) {
            _calculatedGpa.value = 0.0
            return
        }

        var totalPoints = 0.0
        var totalCredits = 0

        courses.forEach { course ->
            val point = when {
                course.grade >= 95 -> 4.0
                course.grade >= 90 -> 3.75
                course.grade >= 85 -> 3.5
                course.grade >= 80 -> 3.0
                course.grade >= 75 -> 2.5
                course.grade >= 70 -> 2.0
                course.grade >= 65 -> 1.5
                course.grade >= 60 -> 1.0
                else -> 0.0
            }
            totalPoints += point * course.credits
            totalCredits += course.credits
        }

        _calculatedGpa.value = if (totalCredits > 0) totalPoints / totalCredits else 0.0
    }
}
