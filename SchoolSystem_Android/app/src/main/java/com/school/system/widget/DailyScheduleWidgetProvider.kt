package com.school.system.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.school.system.MainActivity
import com.school.system.R
import java.util.Calendar

class DailyScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DailyScheduleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.school.system.widget.ACTION_REFRESH"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_teacher_horizontal_strip)

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الإثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة"
                Calendar.SATURDAY -> "السبت"
                else -> "الأحد"
            }

            val upcoming = WidgetScheduleHelper.calculateUpcomingTeacherLesson(context)
            val activeLessonNum = if (upcoming.isOngoing) upcoming.lessonNumber else 0

            views.setTextViewText(R.id.strip_header_day, dayName)

            val lessons = WidgetScheduleHelper.getTodayTeacherLessons(context)
            val lessonsByNum = lessons.associateBy { it.lessonNumber }

            val colIds = listOf(
                R.id.col_lesson_1, R.id.col_lesson_2, R.id.col_lesson_3,
                R.id.col_lesson_4, R.id.col_lesson_5, R.id.col_lesson_6
            )
            val subjIds = listOf(
                R.id.text_subject_1, R.id.text_subject_2, R.id.text_subject_3,
                R.id.text_subject_4, R.id.text_subject_5, R.id.text_subject_6
            )
            val classIds = listOf(
                R.id.text_class_1, R.id.text_class_2, R.id.text_class_3,
                R.id.text_class_4, R.id.text_class_5, R.id.text_class_6
            )

            for (i in 1..6) {
                val lessonInfo = lessonsByNum[i]
                val colViewId = colIds[i - 1]
                val subjViewId = subjIds[i - 1]
                val classViewId = classIds[i - 1]

                if (lessonInfo != null) {
                    val cleanSubj = WidgetScheduleHelper.cleanSubjectName(lessonInfo.subject)
                    val cleanCls = WidgetScheduleHelper.cleanClassName(lessonInfo.className)

                    views.setTextViewText(subjViewId, cleanSubj)
                    views.setTextViewText(classViewId, cleanCls)

                    if (i == activeLessonNum) {
                        views.setInt(colViewId, "setBackgroundResource", R.drawable.widget_teacher_highlight_bg)
                    } else {
                        views.setInt(colViewId, "setBackgroundResource", R.drawable.widget_teacher_badge_bg)
                    }
                } else {
                    views.setTextViewText(subjViewId, "شاغر")
                    views.setTextViewText(classViewId, "-")
                    views.setInt(colViewId, "setBackgroundResource", R.drawable.widget_item_bg)
                }
            }

            // Refresh Intent
            val refreshIntent = Intent(context, DailyScheduleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.strip_btn_refresh, refreshPendingIntent)

            // Open App
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                201,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.teacher_strip_root, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
