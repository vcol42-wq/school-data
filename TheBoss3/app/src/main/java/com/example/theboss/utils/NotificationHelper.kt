package com.example.theboss.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.theboss.MainActivity
import com.example.theboss.R

object NotificationHelper {

    const val CHANNEL_HOMEWORK = "channel_urgent_homework"
    const val CHANNEL_ABSENCE = "channel_guardian_absence"
    const val CHANNEL_BROADCASTS = "channel_broadcasts"

    fun isNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("notifications_enabled", true)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val homeworkChannel = NotificationChannel(
                CHANNEL_HOMEWORK,
                "الواجبات والدروس العاجلة 🔥",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند نشر المعلم لدرس أو واجب جديد"
                enableVibration(true)
            }

            val absenceChannel = NotificationChannel(
                CHANNEL_ABSENCE,
                "متابعة غياب الحصص لولي الأمر 🔴",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه فوري لولي الأمر عند تسجيل غياب الطالب في أي حصة"
                enableVibration(true)
            }

            val broadcastChannel = NotificationChannel(
                CHANNEL_BROADCASTS,
                "التعاميم الإدارية العاجلة 📢",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الإدارة والقرارات المدرسية الهامة"
                enableVibration(true)
            }

            manager.createNotificationChannel(homeworkChannel)
            manager.createNotificationChannel(absenceChannel)
            manager.createNotificationChannel(broadcastChannel)
        }
    }

    fun showUrgentHomeworkNotification(
        context: Context,
        title: String,
        subject: String,
        dueDate: String,
        notificationId: Int = (1000..9999).random()
    ) {
        if (!isNotificationsEnabled(context)) return
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

        val notification = NotificationCompat.Builder(context, CHANNEL_HOMEWORK)
            .setSmallIcon(R.drawable.axa)
            .setContentTitle("واجب جديد عاجل 🔥: $subject")
            .setContentText("$title • موعد التسليم: $dueDate")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\nالمادة: $subject\nموعد التسليم: $dueDate"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    fun showAbsenceNotification(
        context: Context,
        date: String,
        periodNumber: Int,
        subject: String
    ) {
        if (!isNotificationsEnabled(context)) return
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ABSENCE)
            .setSmallIcon(R.drawable.axa)
            .setContentTitle("تنبيه لولي الأمر: تسجيل غياب 🔴")
            .setContentText("تم تسجيل غياب الطالب في الحصة رقم $periodNumber ($subject)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "تنبيه هام لولي الأمر:\nتم رصد غياب الطالب في الحصة الدراسية رقم $periodNumber لمادة $subject بتاريخ $date."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((10000..99999).random(), notification)
    }

    fun showBroadcastNotification(
        context: Context,
        title: String,
        message: String,
        priority: String
    ) {
        if (!isNotificationsEnabled(context)) return
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

        val notification = NotificationCompat.Builder(context, CHANNEL_BROADCASTS)
            .setSmallIcon(R.drawable.axa)
            .setContentTitle("تعميم إداري ($priority) 📢: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((100000..999999).random(), notification)
    }
}
