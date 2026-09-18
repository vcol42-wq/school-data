package com.example.theboss.utils.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 1. جدولة منبه في وقت محدد بدقة (Exact Alarm)
    fun scheduleExactAlarm(
        alarmId: Int,
        triggerAtMillis: Long,
        title: String,
        message: String
    ) {
        // التحقق من إمكانية جدولة المنبه الدقيق على أندرويد 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // في حال عدم توفر الإذن يتم الجدولة العادية كبديل آمن
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    createPendingIntent(alarmId, title, message)
                )
                return
            }
        }

        val intent = createPendingIntent(alarmId, title, message)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intent
        )
    }

    // 2. ضبط مؤقت تنازلي (جلسات التركيز Pomodoro)
    fun startFocusTimer(alarmId: Int, durationMinutes: Int, subjectName: String) {
        val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        scheduleExactAlarm(
            alarmId = alarmId,
            triggerAtMillis = triggerTime,
            title = "انتهت جلسة التركيز!",
            message = "أحسنت! أكملت $durationMinutes دقيقة في مادة $subjectName. خذ استراحة قصيرة."
        )
    }

    // 3. ضبط منبه متكرر (يومياً في نفس الساعة)
    fun scheduleDailyRepeatingAlarm(
        alarmId: Int,
        triggerAtMillis: Long,
        title: String,
        message: String
    ) {
        val intent = createPendingIntent(alarmId, title, message)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            intent
        )
    }

    // 4. إلغاء منبه محدد
    fun cancelAlarm(alarmId: Int) {
        val intent = Intent(context, StudyAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(alarmId: Int, title: String, message: String): PendingIntent {
        val intent = Intent(context, StudyAlarmReceiver::class.java).apply {
            putExtra("EXTRA_ALARM_ID", alarmId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", message)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
