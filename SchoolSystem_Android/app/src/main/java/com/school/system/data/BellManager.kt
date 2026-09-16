package com.school.system.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.school.system.data.dao.LessonAlarmDao
import com.school.system.data.model.BellSettings
import com.school.system.data.model.LessonAlarm
import com.school.system.receiver.BellReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BellManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lessonAlarmDao: LessonAlarmDao
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun setupDefaultSchedule(settings: BellSettings) {
        val alarms = mutableListOf<LessonAlarm>()
        val days = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
        
        days.forEach { day ->
            var currentTotalMinutes = settings.startHour * 60 + settings.startMinute
            
            for (i in 1..6) {
                val startH = currentTotalMinutes / 60
                val startM = currentTotalMinutes % 60
                
                val endTotal = currentTotalMinutes + settings.lessonDuration
                val endH = endTotal / 60
                val endM = endTotal % 60
                
                alarms.add(
                    LessonAlarm(
                        dayOfWeek = day,
                        lessonIndex = i,
                        startTime = String.format(Locale.US, "%02d:%02d", startH, startM),
                        endTime = String.format(Locale.US, "%02d:%02d", endH, endM),
                        isEnabled = settings.isGlobalEnabled
                    )
                )
                
                currentTotalMinutes = endTotal + settings.breakDuration
            }
        }
        
        lessonAlarmDao.deleteAll()
        lessonAlarmDao.insertAlarms(alarms)
        if (settings.isGlobalEnabled) {
            scheduleAllAlarms()
        } else {
            cancelAllAlarms()
        }
    }

    suspend fun scheduleAllAlarms() {
        cancelAllAlarms()
        val enabledAlarms = lessonAlarmDao.getEnabledAlarmsList()
        enabledAlarms.forEach { alarm ->
            scheduleAlarm(alarm)
        }
    }

    private fun scheduleAlarm(alarm: LessonAlarm) {
        if (!alarm.isEnabled) return
        try {
            val parts = alarm.startTime.split(":").mapNotNull { it.toIntOrNull() }
            if (parts.size < 2) return
            val hour = parts[0]
            val minute = parts[1]
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, alarm.dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, BellReceiver::class.java).apply {
                putExtra("lesson_index", alarm.lessonIndex)
                putExtra("lesson_name", alarm.lessonName)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun cancelAllAlarms() {
        val all = lessonAlarmDao.getAllAlarms().first()
        all.forEach { alarm ->
            val intent = Intent(context, BellReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
    
    suspend fun updateAlarm(alarm: LessonAlarm) {
        lessonAlarmDao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            scheduleAlarm(alarm)
        } else {
            // Cancel specific alarm
            val intent = Intent(context, BellReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
