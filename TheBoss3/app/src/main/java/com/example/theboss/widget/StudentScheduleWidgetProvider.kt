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
                Calendar.MONDAY -> "الإثنين" to false
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

            fun normDay(day: String): String = day
                .replace("[أإآ]".toRegex(), "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")
                .trim()

            fun standardizeGradeName(gradeStr: String?): String {
                if (gradeStr.isNullOrBlank()) return "الأول المتوسط"
                val clean = gradeStr.trim().replace("^(الصف|صف)\\s+".toRegex(), "").trim()
                val norm = clean.replace("[أإآ]".toRegex(), "ا").replace("ة", "ه").replace("ى", "ي").lowercase()

                var base = "الأول"
                if (norm.contains("سادس") || norm.contains("6") || norm.contains("٦")) base = "السادس"
                else if (norm.contains("خامس") || norm.contains("5") || norm.contains("٥")) base = "الخامس"
                else if (norm.contains("رابع") || norm.contains("4") || norm.contains("٤")) base = "الرابع"
                else if (norm.contains("ثالث") || norm.contains("3") || norm.contains("٣")) base = "الثالث"
                else if (norm.contains("ثاني") || norm.contains("2") || norm.contains("٢")) base = "الثاني"
                else if (norm.contains("اول") || norm.contains("1") || norm.contains("١")) base = "الأول"

                var branch = ""
                if (norm.contains("احيائ") || norm.contains("تطبيق") || norm.contains("علم")) branch = "العلمي"
                else if (norm.contains("ادب")) branch = "الأدبي"
                else if (norm.contains("صناع")) branch = "الصناعي"
                else if (norm.contains("تجار")) branch = "التجاري"
                else if (norm.contains("متوسط")) branch = "المتوسط"
                else if (norm.contains("اعداد") || norm.contains("ثانوي")) branch = "الإعدادي"
                else if (norm.contains("ابتدائ")) branch = "الابتدائي"

                if (branch.isEmpty()) {
                    if (base == "الأول" || base == "الثاني" || base == "الثالث") {
                        branch = "المتوسط"
                    } else if (base == "الرابع" || base == "الخامس" || base == "السادس") {
                        branch = "الإعدادي"
                    }
                }

                return if (branch.isNotEmpty()) "$base $branch" else base
            }

            fun isGradeMatch(g1: String?, g2: String?): Boolean {
                if (g1.isNullOrBlank() || g2.isNullOrBlank()) return false
                val std1 = standardizeGradeName(g1)
                val std2 = standardizeGradeName(g2)
                if (std1 == std2) return true

                fun normStr(s: String) = s.replace("[أإآ]".toRegex(), "ا").replace("ة", "ه").replace("ى", "ي").lowercase()
                val n1 = normStr(g1)
                val n2 = normStr(g2)

                fun extractBase(norm: String): String {
                    return when {
                        norm.contains("سادس") || norm.contains("6") || norm.contains("٦") -> "سادس"
                        norm.contains("خامس") || norm.contains("5") || norm.contains("٥") -> "خامس"
                        norm.contains("رابع") || norm.contains("4") || norm.contains("٤") -> "رابع"
                        norm.contains("ثالث") || norm.contains("3") || norm.contains("٣") -> "ثالث"
                        norm.contains("ثاني") || norm.contains("2") || norm.contains("٢") -> "ثاني"
                        norm.contains("اول") || norm.contains("1") || norm.contains("١") -> "اول"
                        else -> norm
                    }
                }

                val b1 = extractBase(n1)
                val b2 = extractBase(n2)
                if (b1 != b2) return false

                val hasBranch1 = n1.contains("متوسط") || n1.contains("اعداد") || n1.contains("ابتدائ") || n1.contains("ثانوي") || n1.contains("علم") || n1.contains("ادب")
                val hasBranch2 = n2.contains("متوسط") || n2.contains("اعداد") || n2.contains("ابتدائ") || n2.contains("ثانوي") || n2.contains("علم") || n2.contains("ادب")

                if (!hasBranch1 || !hasBranch2) return true
                return std1 == std2
            }

            fun standardizeSectionName(secStr: String?): String {
                if (secStr.isNullOrBlank()) return "أ"
                val clean = secStr.trim().replace("^(شعبة|الشعبة|ش)\\s*".toRegex(), "").trim()

                val letterOnly = clean
                    .replace("(الصف|صف|الأول|الاول|الثاني|الثالث|الرابع|الخامس|السادس|المتوسط|الإعدادي|الاعدادي|الابتدائي|العلمي|الأدبي|الادبي)".toRegex(), "")
                    .trim()
                val target = if (letterOnly.isNotBlank()) letterOnly else clean
                val targetLower = target.lowercase()

                if (target == "ا" || target == "أ" || target == "إ" || target == "آ" || targetLower == "a" || targetLower == "1" || targetLower == "١") return "أ"
                if (target == "ب" || targetLower == "b" || targetLower == "2" || targetLower == "٢") return "ب"
                if (target == "ج" || targetLower == "c" || targetLower == "3" || targetLower == "٣") return "ج"
                if (target == "د" || targetLower == "d" || targetLower == "4" || targetLower == "٤") return "د"
                if (target == "ه" || target == "هـ" || targetLower == "e" || targetLower == "5" || targetLower == "٥") return "هـ"
                if (target == "و" || targetLower == "f" || targetLower == "6" || targetLower == "٦") return "و"
                if (target == "ز" || targetLower == "z" || targetLower == "7" || targetLower == "٧") return "ز"
                if (target == "ح" || targetLower == "h" || targetLower == "8" || targetLower == "٨") return "ح"
                if (target == "ط" || targetLower == "9" || targetLower == "٩") return "ط"
                if (target == "خ") return "خ"
                return if (target.length == 1 && target[0].isLetter()) target else "أ"
            }

            var summaryText = ""

            try {
                val gson = Gson()
                val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
                if (rootObj != null) {
                    val stdS = standardizeSectionName(studentSection)

                    val dayData = rootObj.entries.find { normDay(it.key) == normDay(todayArabic) }?.value as? List<*>

                    if (dayData != null) {
                        for (row in dayData) {
                            if (row is Map<*, *>) {
                                val g = row["grade"]?.toString() ?: ""
                                val s = row["section"]?.toString() ?: ""
                                val rowS = standardizeSectionName(s)

                                if (isGradeMatch(g, studentGrade) && (rowS == stdS || stdS == "الكل")) {
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
