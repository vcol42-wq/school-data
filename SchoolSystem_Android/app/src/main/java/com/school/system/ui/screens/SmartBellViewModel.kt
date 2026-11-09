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
import com.school.system.widget.WidgetScheduleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val configDao: ConfigDao,
    @ApplicationContext private val appContext: Context
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

    fun applyAutoSchedule(useCustomTimings: Boolean = false) {
        viewModelScope.launch {
            val c = config.value ?: SchoolConfig()
            val timing = WidgetScheduleHelper.getFullTimingFromScheduleJson(appContext)
            val startParts = if (useCustomTimings) timing.startHour.split(":") else emptyList()
            val settings = BellSettings(
                startHour = startParts.getOrNull(0)?.toIntOrNull() ?: c.bellStartHour,
                startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: c.bellStartMinute,
                lessonDuration = if (useCustomTimings) timing.lessonDuration else c.bellLessonDuration,
                breakDuration = if (useCustomTimings) timing.breakDuration else c.bellBreakDuration
            )
            bellManager.setupDefaultSchedule(
                settings,
                if (useCustomTimings) timing.customLessons else emptyList(),
                if (useCustomTimings) timing.customBreaks else emptyList()
            )
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
            val rawScheduleJson = sequenceOf(
                prefs.getString("synced_schedule", null),
                context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                    .getString("synced_schedule", null),
                context.getSharedPreferences("com.example.theboss_preferences", Context.MODE_PRIVATE)
                    .getString("synced_schedule", null)
            ).firstOrNull { !it.isNullOrBlank() && it != "{}" } ?: "{}"
            val teacherName = prefs.getString("teacher_name", "")?.trim()?.lowercase() ?: ""
            val c = config.value ?: SchoolConfig()
            val timing = WidgetScheduleHelper.getFullTimingFromScheduleJson(context)
            val startParts = timing.startHour.split(":")
            
            val settings = BellSettings(
                startHour = startParts.getOrNull(0)?.toIntOrNull() ?: c.bellStartHour,
                startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: c.bellStartMinute,
                lessonDuration = timing.lessonDuration,
                breakDuration = timing.breakDuration
            )
            bellManager.setupDefaultSchedule(settings, timing.customLessons, timing.customBreaks)

            var importedLessonCount = 0
            try {
                val gson = Gson()
                val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
                val importedLessons = mutableMapOf<Pair<Int, Int>, LinkedHashSet<String>>()
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

                                                if (!c.bellRingTeacherOnly || isTeacherLesson) {
                                                    val lessonLabel = "$subj - $className"
                                                    val slotLessons = importedLessons.getOrPut(dayVal to i) { linkedSetOf() }
                                                    if (slotLessons.add(lessonLabel)) {
                                                        importedLessonCount++
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val alarmsBySlot = currentAlarms.associateBy { it.dayOfWeek to it.lessonIndex }
                importedLessons.forEach { (slot, lessons) ->
                    val alarm = alarmsBySlot[slot] ?: return@forEach
                    bellManager.updateAlarm(
                        alarm.copy(
                            lessonName = lessons.joinToString("، "),
                            isEnabled = true
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onComplete(importedLessonCount)
        }
    }
}
