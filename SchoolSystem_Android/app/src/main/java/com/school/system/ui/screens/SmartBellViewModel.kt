package com.school.system.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.school.system.data.BellManager
import com.school.system.data.dao.LessonAlarmDao
import com.school.system.data.dao.ConfigDao
import com.school.system.data.model.BellSettings
import com.school.system.data.model.LessonAlarm
import com.school.system.data.model.SchoolConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SmartBellViewModel @Inject constructor(
    private val lessonAlarmDao: LessonAlarmDao,
    private val bellManager: BellManager,
    private val configDao: ConfigDao
) : ViewModel() {

    val allAlarms = lessonAlarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val config = configDao.getConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateBellSettings(
        startH: Int, 
        startM: Int, 
        lessonD: Int, 
        breakD: Int, 
        ringtone: String? = null
    ) {
        viewModelScope.launch {
            val current = config.value ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                bellStartHour = startH,
                bellStartMinute = startM,
                bellLessonDuration = lessonD,
                bellBreakDuration = breakD,
                bellRingtoneUri = ringtone
            ))
        }
    }

    fun updateBellRingMode(ringTeacherOnly: Boolean) {
        viewModelScope.launch {
            val current = config.value ?: SchoolConfig()
            configDao.saveConfig(current.copy(
                bellRingTeacherOnly = ringTeacherOnly
            ))
        }
    }

    fun applyAutoSchedule() {
        viewModelScope.launch {
            val c = config.value ?: SchoolConfig()
            val settings = BellSettings(
                startHour = c.bellStartHour,
                startMinute = c.bellStartMinute,
                lessonDuration = c.bellLessonDuration,
                breakDuration = c.bellBreakDuration
            )
            bellManager.setupDefaultSchedule(settings)
        }
    }

    fun toggleAlarm(alarm: LessonAlarm, isEnabled: Boolean) {
        viewModelScope.launch {
            bellManager.updateAlarm(alarm.copy(isEnabled = isEnabled))
        }
    }

    fun enableAllForDay(dayOfWeek: Int, enable: Boolean) {
        viewModelScope.launch {
            val alarms = allAlarms.value.filter { it.dayOfWeek == dayOfWeek }
            alarms.forEach { alarm ->
                bellManager.updateAlarm(alarm.copy(isEnabled = enable))
            }
        }
    }

    fun updateAlarmDetails(alarm: LessonAlarm, name: String, start: String, end: String) {
        viewModelScope.launch {
            bellManager.updateAlarm(alarm.copy(lessonName = name, startTime = start, endTime = end))
        }
    }

    fun importFromSchedule(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
            val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            val teacherName = prefs.getString("teacher_name", "")?.trim()?.lowercase() ?: ""
            val c = config.value ?: SchoolConfig()
            
            // Ensure default baseline alarms exist
            val settings = BellSettings(
                startHour = c.bellStartHour,
                startMinute = c.bellStartMinute,
                lessonDuration = c.bellLessonDuration,
                breakDuration = c.bellBreakDuration
            )
            bellManager.setupDefaultSchedule(settings)

            var matchedCount = 0
            try {
                val gson = Gson()
                val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
                val dayMap = mapOf(
                    "الأحد" to Calendar.SUNDAY,
                    "الإثنين" to Calendar.MONDAY,
                    "الثلاثاء" to Calendar.TUESDAY,
                    "الأربعاء" to Calendar.WEDNESDAY,
                    "الخميس" to Calendar.THURSDAY
                )

                val currentAlarms = lessonAlarmDao.getAllAlarms().first().toMutableList()

                if (rootObj != null) {
                    for ((dayName, dayVal) in dayMap) {
                        val dayData = rootObj[dayName]
                        if (dayData is List<*>) {
                            for (row in dayData) {
                                if (row is Map<*, *>) {
                                    val grade = row["grade"]?.toString() ?: ""
                                    val section = row["section"]?.toString() ?: ""
                                    val className = "$grade ($section)".trim()
                                    val lessons = row["lessons"] as? Map<*, *>
                                    if (lessons != null) {
                                        for (i in 1..6) {
                                            val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                            val subj = lessonObj?.get("subject")?.toString() ?: ""
                                            val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                            val isOff = lessonObj?.get("isOff") as? Boolean ?: false

                                            if (subj.isNotEmpty() && !isOff) {
                                                val isTeacherLesson = if (teacherName.isNotEmpty()) {
                                                    teacher.lowercase().contains(teacherName) || subj.lowercase().contains(teacherName)
                                                } else {
                                                    true
                                                }

                                                val alarm = currentAlarms.find { it.dayOfWeek == dayVal && it.lessonIndex == i }
                                                if (alarm != null) {
                                                    val updatedName = "$subj - $className"
                                                    val shouldEnable = if (c.bellRingTeacherOnly) isTeacherLesson else true
                                                    val updatedAlarm = alarm.copy(lessonName = updatedName, isEnabled = shouldEnable)
                                                    bellManager.updateAlarm(updatedAlarm)
                                                    matchedCount++
                                                }
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
            onComplete(matchedCount)
        }
    }
}
