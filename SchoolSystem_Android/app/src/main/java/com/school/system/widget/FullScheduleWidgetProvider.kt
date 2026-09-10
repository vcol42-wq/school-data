package com.school.system.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.school.system.MainActivity
import com.school.system.R
import java.util.Calendar

class FullScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, FullScheduleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.school.system.widget.ACTION_REFRESH_FULL"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_full_schedule)

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الإثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة (عطلة)"
                Calendar.SATURDAY -> "السبت (عطلة)"
                else -> "الأحد"
            }

            views.setTextViewText(R.id.widget_day_text, "اليوم: $dayName")

            // Update live countdown banner
            val upcoming = WidgetScheduleHelper.calculateUpcomingTeacherLesson(context)
            when {
                upcoming.isOngoing -> {
                    views.setTextViewText(
                        R.id.widget_countdown_banner,
                        "⏱️ جاري الآن: الحصة ${upcoming.lessonNumber} (${upcoming.subject}) • باقي ${upcoming.remainingMinutes}m"
                    )
                }
                upcoming.isFutureDay -> {
                    views.setTextViewText(
                        R.id.widget_countdown_banner,
                        "📌 درسك القادم: يوم ${upcoming.nextDayName} - الحصة ${upcoming.lessonNumber} (${upcoming.subject} - ${upcoming.className}) الساعة ${upcoming.formattedTime}"
                    )
                }
                !upcoming.hasNoMoreLessons -> {
                    views.setTextViewText(
                        R.id.widget_countdown_banner,
                        "⏳ القادمة اليوم: الحصة ${upcoming.lessonNumber} (${upcoming.subject}) • باقي ${upcoming.remainingMinutes}m"
                    )
                }
                else -> {
                    views.setTextViewText(
                        R.id.widget_countdown_banner,
                        "✨ اكتملت جميع دروس اليوم"
                    )
                }
            }

            // Service Intent for RemoteViews ListView
            val serviceIntent = Intent(context, FullScheduleWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // PendingIntent for Refresh button
            val refreshIntent = Intent(context, FullScheduleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // PendingIntent for clicking widget header / container -> Opens MainActivity
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                102,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)
            views.setPendingIntentTemplate(R.id.widget_list_view, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
        }
    }
}
