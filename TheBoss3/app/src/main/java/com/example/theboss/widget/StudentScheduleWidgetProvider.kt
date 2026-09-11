package com.example.theboss.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.theboss.MainActivity
import com.example.theboss.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class StudentScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, StudentScheduleWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun sendRefreshBroadcast(context: Context) {
            val intent = Intent(context, StudentScheduleWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, StudentScheduleWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun shortenSubject(subj: String): String {
            if (subj.isBlank() || subj == "شاغر") return subj
            var s = subj.trim()
                .replace("اللغة العربية", "عربي")
                .replace("اللغة الانكليزية", "إنكليزي")
                .replace("اللغة الإنجليزية", "إنكليزي")
                .replace("التربية الإسلامية", "إسلامية")
                .replace("التربية الاسلامية", "إسلامية")
                .replace("العلوم العامة", "علوم")
                .replace("التربية الفنية", "فنية")
                .replace("التربية الرياضية", "رياضة")
                .replace("الاجتماعيات", "اجتماعيات")
                .replace("الرياضيات", "رياضيات")
                .replace("الفيزياء", "فيزياء")
                .replace("الكيمياء", "كيمياء")
                .replace("الأحياء", "أحياء")
                .replace("الحاسوب", "حاسوب")
                .replace("القرآن الكريم", "قرآن")

            if (s.startsWith("ال") && s.length > 3) {
                s = s.substring(2)
            }
            return s
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
            val studentGrade = prefs.getString("student_grade", "الصف الثالث المتوسط") ?: "الصف الثالث المتوسط"
            val studentSection = prefs.getString("student_section", "أ") ?: "أ"
            val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"

            val views = RemoteViews(context.packageName, R.layout.widget_student_schedule)

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val (todayArabic, isWeekend) = when (dayOfWeek) {
                Calendar.SUNDAY -> "الأحد" to false
                Calendar.MONDAY -> "الأثنين" to false
                Calendar.TUESDAY -> "الثلاثاء" to false
                Calendar.WEDNESDAY -> "الأربعاء" to false
                Calendar.THURSDAY -> "الخميس" to false
                Calendar.FRIDAY -> "الأحد" to true
                Calendar.SATURDAY -> "الأحد" to true
                else -> "الأحد" to false
            }

            views.setTextViewText(
                R.id.widget_day_title,
                if (isWeekend) "جدول الأحد القادم 📅" else "جدول $todayArabic 📅"
            )
            views.setTextViewText(R.id.widget_student_info, "$studentGrade ($studentSection)")

            fun norm(str: String): String = str
                .replace("[أإآ]".toRegex(), "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .replace("^(الصف|صف)\\s*".toRegex(), "")
                .trim()

            var summaryText = ""

            try {
                val gson = Gson()
                val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
                if (rootObj != null) {
                    val stdG = norm(studentGrade)
                    val stdS = norm(studentSection)

                    val dayData = rootObj[todayArabic] as? List<*>
                        ?: rootObj["الأثنين"] as? List<*>
                        ?: rootObj["الاثنين"] as? List<*>
                        ?: rootObj.values.firstOrNull() as? List<*>

                    if (dayData != null) {
                        for (row in dayData) {
                            if (row is Map<*, *>) {
                                val lessons = row["lessons"] as? Map<*, *>
                                if (lessons != null) {
                                    val items = mutableListOf<String>()
                                    for (i in 1..6) {
                                        val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                        val subj = lessonObj?.get("subject")?.toString() ?: ""
                                        val isOff = lessonObj?.get("isOff") as? Boolean ?: false

                                        if (!isOff && subj.isNotBlank()) {
                                            items.add("$i: ${shortenSubject(subj)}")
                                        }
                                    }
                                    if (items.isNotEmpty()) {
                                        summaryText = items.joinToString("  •  ")
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (summaryText.isBlank()) {
                summaryText = "1: إسلامية  •  2: رياضيات  •  3: عربي  •  4: علوم  •  5: إنكليزي  •  6: اجتماعيات"
            }

            views.setTextViewText(R.id.widget_schedule_summary, summaryText)

            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
