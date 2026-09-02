package com.school.system.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.school.system.MainActivity

object TeacherNotificationHelper {

    const val CHANNEL_QUESTIONS = "channel_teacher_questions"
    const val CHANNEL_BROADCASTS = "channel_teacher_broadcasts"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val questionsChannel = NotificationChannel(
                CHANNEL_QUESTIONS,
                "استفسارات وأسئلة الطلاب 💬",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار فوري عند طرح طالب لسؤال في قناة التقوية"
                enableVibration(true)
            }

            val broadcastChannel = NotificationChannel(
                CHANNEL_BROADCASTS,
                "تعاميم إدارة المدرسة 📢",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات وتوجيهات مدير المدرسة"
                enableVibration(true)
            }

            manager.createNotificationChannel(questionsChannel)
            manager.createNotificationChannel(broadcastChannel)
        }
    }

    fun showStudentQuestionNotification(
        context: Context,
        studentName: String,
        subject: String,
        questionText: String
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

        val notification = NotificationCompat.Builder(context, CHANNEL_QUESTIONS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("سؤال جديد من طالب 💬: $subject")
            .setContentText("$studentName: $questionText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("الطالب: $studentName ($subject)\nالسؤال: $questionText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((1000..9999).random(), notification)
    }

    fun showBroadcastNotification(
        context: Context,
        title: String,
        message: String
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

        val notification = NotificationCompat.Builder(context, CHANNEL_BROADCASTS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("تعميم إداري عاجل 📢: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$message"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify((10000..99999).random(), notification)
    }
}
