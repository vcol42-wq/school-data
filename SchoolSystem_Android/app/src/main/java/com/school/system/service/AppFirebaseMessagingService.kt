package com.school.system.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.school.system.utils.TeacherNotificationHelper

class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase FCM Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "إشعار مدرسي جديد"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: ""

        if (body.isNotBlank()) {
            TeacherNotificationHelper.showBroadcastNotification(
                context = applicationContext,
                title = title,
                message = body
            )
        }
    }

    companion object {
        private const val TAG = "AppFirebaseMsgService"
    }
}
