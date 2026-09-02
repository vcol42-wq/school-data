package com.principal.system.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.principal.system.MainActivity
import com.principal.system.R

object PrincipalNotificationHelper {

    const val CHANNEL_ALERTS = "channel_principal_alerts"
    const val CHANNEL_RED_ZONE = "channel_critical_absences"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "تنبيهات الإدارة ونبض الحضور 🏛️",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات بنبض الحضور ومعدلات رصد المعلمين"
                enableVibration(true)
            }

            val redZoneChannel = NotificationChannel(
                CHANNEL_RED_ZONE,
                "المنطقة الحمراء للغيابات الحرجة ⚠️",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه فوري عند تجاوز طالب لحد الغياب الحرج"
                enableVibration(true)
            }

            manager.createNotificationChannel(alertChannel)
            manager.createNotificationChannel(redZoneChannel)
        }
    }

    fun showCriticalAbsenceAlert(
        context: Context,
        studentName: String,
        className: String,
        daysCount: Int
    ) {
        createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (100..999).random(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_RED_ZONE)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("إنذار المنطقة الحمراء للغياب ⚠️")
            .setContentText("تجاوز الطالب $studentName ($className) حد $daysCount أيام غياب!")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "إنذار إداري عاجل للمدير:\nتجاوز الطالب $studentName من الصف ($className) حد الغياب الحرج بإجمالي $daysCount أيام غياب. يرجى مراجعة الرادار واتخاذ الإجراء."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((1000..9999).random(), notification)
    }

    fun showLowAttendanceRateAlert(
        context: Context,
        rate: Float,
        absentCount: Int
    ) {
        createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (100..999).random(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("تنبيه انخفاض نسبة الحضور اليومي 📉")
            .setContentText("نسبة الحضور اليوم بلغت ${String.format("%.1f", rate)}% مع تسجيل $absentCount غيابات.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((2000..9999).random(), notification)
    }
}
