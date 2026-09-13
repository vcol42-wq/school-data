package com.school.system.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
        if (intent.action == ACTION_REFRESH_WIDGET ||
            intent.action == ACTION_PREV_DAY ||
            intent.action == ACTION_NEXT_DAY ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
            if (intent.action == ACTION_PREV_DAY) {
                val currentIdx = prefs.getInt("widget_selected_day_idx", WidgetScheduleHelper.getDefaultDayIndex())
                val newIdx = if (currentIdx > 0) currentIdx - 1 else 4
                prefs.edit().putInt("widget_selected_day_idx", newIdx).apply()
            } else if (intent.action == ACTION_NEXT_DAY) {
                val currentIdx = prefs.getInt("widget_selected_day_idx", WidgetScheduleHelper.getDefaultDayIndex())
                val newIdx = if (currentIdx < 4) currentIdx + 1 else 0
                prefs.edit().putInt("widget_selected_day_idx", newIdx).apply()
            } else if (intent.action == ACTION_REFRESH_WIDGET) {
                prefs.edit().putInt("widget_selected_day_idx", WidgetScheduleHelper.getDefaultDayIndex()).apply()
            }

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
        const val ACTION_PREV_DAY = "com.school.system.widget.ACTION_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.school.system.widget.ACTION_NEXT_DAY"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_teacher_horizontal_strip)

            val dayName = WidgetScheduleHelper.getEffectiveDayArabic(context)
            views.setTextViewText(R.id.strip_header_day, dayName)

            val upcoming = WidgetScheduleHelper.calculateUpcomingTeacherLesson(context)
            val isViewingRealToday = (dayName == WidgetScheduleHelper.getRealCurrentDayArabic())
            val activeLessonNum = if (upcoming.isOngoing && isViewingRealToday) upcoming.lessonNumber else 0

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
                        views.setTextColor(subjViewId, Color.WHITE)
                        views.setTextColor(classViewId, Color.parseColor("#FEF08A"))
                        views.setInt(colViewId, "setBackgroundResource", R.drawable.widget_teacher_highlight_bg)
                    } else {
                        views.setTextColor(subjViewId, Color.WHITE)
                        views.setTextColor(classViewId, Color.parseColor("#E0F2FE"))
                        views.setInt(colViewId, "setBackgroundResource", R.drawable.widget_teacher_card_bg)
                    }
                } else {
                    views.setTextViewText(subjViewId, "شاغر")
                    views.setTextViewText(classViewId, "-")
                    views.setTextColor(subjViewId, Color.parseColor("#CBD5E1"))
                    views.setTextColor(classViewId, Color.parseColor("#94A3B8"))
                    views.setInt(colViewId, "setBackgroundResource", R.drawable.widget_item_bg)
                }
            }

            // Prev Day Intent
            val prevIntent = Intent(context, DailyScheduleWidgetProvider::class.java).apply {
                action = ACTION_PREV_DAY
                setPackage(context.packageName)
            }
            val prevPending = PendingIntent.getBroadcast(
                context,
                101,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.strip_btn_prev_day, prevPending)

            // Next Day Intent
            val nextIntent = Intent(context, DailyScheduleWidgetProvider::class.java).apply {
                action = ACTION_NEXT_DAY
                setPackage(context.packageName)
            }
            val nextPending = PendingIntent.getBroadcast(
                context,
                102,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.strip_btn_next_day, nextPending)

            // Refresh Intent
            val refreshIntent = Intent(context, DailyScheduleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                setPackage(context.packageName)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                103,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.strip_btn_refresh, refreshPendingIntent)

            // Open App from Badge (Do not attach to root to prevent swallowing button clicks)
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                201,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.strip_status_badge, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
