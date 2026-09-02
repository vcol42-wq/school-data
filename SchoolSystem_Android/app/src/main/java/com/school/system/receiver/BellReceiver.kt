package com.school.system.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.school.system.MainActivity
import com.school.system.data.dao.ConfigDao
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lessonIndex = intent.getIntAsExtra("lesson_index", 0)
        val lessonName = intent.getStringExtra("lesson_name") ?: "درس جديد"
        
        val config = runBlocking {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BellReceiverEntryPoint::class.java
            )
            entryPoint.configDao().getConfig().first()
        }

        showNotification(context, lessonIndex, lessonName, config?.bellRingtoneUri)
    }

    private fun showNotification(context: Context, index: Int, name: String, ringtoneUriStr: String?) {
        val channelId = "school_bell_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "جرس المدرسة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات بداية الحصص الدراسية"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val soundUri = ringtoneUriStr?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان وقت الحصة $index")
            .setContentText("بداية درس: $name")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(index, notification)
    }

    // Define Hilt EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    @dagger.hilt.EntryPoint
    interface BellReceiverEntryPoint {
        fun configDao(): ConfigDao
    }

    // Helper to avoid API mismatch in different versions
    private fun Intent.getIntAsExtra(name: String, defaultValue: Int): Int {
        return getIntExtra(name, defaultValue)
    }
}
