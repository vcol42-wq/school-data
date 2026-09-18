package com.example.theboss.utils.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class StudyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("EXTRA_ALARM_ID", 0)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "تذكير دراسي"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "حان وقت جلسة المذاكرة المقررة!"

        showAlarmNotification(context, alarmId, title, message)
    }

    private fun showAlarmNotification(
        context: Context,
        alarmId: Int,
        title: String,
        message: String
    ) {
        val channelId = "study_alarms_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // إنشاء قناة الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات المذاكرة والامتحانات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة إرسال التنبيهات المجدولة والمؤقتات الدراسية"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Add a click action to open MainActivity
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = openIntent?.let {
            PendingIntent.getActivity(
                context,
                alarmId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        notificationManager.notify(alarmId, notificationBuilder.build())
    }
}
