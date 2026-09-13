package com.example.theboss.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.example.theboss.MainActivity
import com.example.theboss.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

data class WidgetSlot(val subject: String, val teacherName: String)

class StudentScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)

        if (action == ACTION_PREV_DAY) {
            val currentIdx = prefs.getInt("widget_selected_day_idx", getDefaultDayIndex())
            val newIdx = if (currentIdx > 0) currentIdx - 1 else 4
            prefs.edit().putInt("widget_selected_day_idx", newIdx).apply()
        } else if (action == ACTION_NEXT_DAY) {
            val currentIdx = prefs.getInt("widget_selected_day_idx", getDefaultDayIndex())
            val newIdx = if (currentIdx < 4) currentIdx + 1 else 0
            prefs.edit().putInt("widget_selected_day_idx", newIdx).apply()
        } else if (action == ACTION_REFRESH_WIDGET) {
            prefs.edit().putInt("widget_selected_day_idx", getDefaultDayIndex()).apply()
        }

        if (action == ACTION_PREV_DAY || action == ACTION_NEXT_DAY || action == ACTION_REFRESH_WIDGET || action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StudentScheduleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val TAG = "StudentWidget"
        const val ACTION_PREV_DAY = "com.example.theboss.widget.ACTION_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.example.theboss.widget.ACTION_NEXT_DAY"
        const val ACTION_REFRESH_WIDGET = "com.example.theboss.widget.ACTION_REFRESH"

        val SCHOOL_DAYS = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

        fun getDefaultDayIndex(): Int {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            return when (dayOfWeek) {
                Calendar.SUNDAY -> 0
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> {
                    // Past 12:45 PM on Thursday -> show Sunday
                    if (hourOfDay > 12 || (hourOfDay == 12 && minute >= 45)) 0 else 4
                }
                Calendar.FRIDAY, Calendar.SATURDAY -> 0 // Weekend -> show Sunday
                else -> 0
            }
        }

        fun sendRefreshBroadcast(context: Context) {
            try {
                val intent = Intent(context, StudentScheduleWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, StudentScheduleWidgetProvider::class.java)
                    )
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Error sending refresh broadcast: ${e.message}")
            }
        }

        fun shortenSubject(subj: String): String {
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

        val defaultLessonsMap = mapOf(
            "الأحد" to mapOf(
                1 to WidgetSlot("إسلامية", "أ. أحمد العلي"),
                2 to WidgetSlot("رياضيات", "أ. محمد الموسوي"),
                3 to WidgetSlot("عربي", "أ. علي الحسين"),
                4 to WidgetSlot("علوم", "أ. حيدر عباس"),
                5 to WidgetSlot("إنكليزي", "أ. عمر الفاروق"),
                6 to WidgetSlot("اجتماعيات", "أ. جاسم محمد")
            ),
            "الإثنين" to mapOf(
                1 to WidgetSlot("عربي", "أ. علي الحسين"),
                2 to WidgetSlot("علوم", "أ. حيدر عباس"),
                3 to WidgetSlot("رياضيات", "أ. محمد الموسوي"),
                4 to WidgetSlot("اجتماعيات", "أ. جاسم محمد"),
                5 to WidgetSlot("إسلامية", "أ. أحمد العلي"),
                6 to WidgetSlot("إنكليزي", "أ. عمر الفاروق")
            ),
            "الثلاثاء" to mapOf(
                1 to WidgetSlot("كيمياء", "أ. سامر صاحب"),
                2 to WidgetSlot("إنكليزي", "أ. عمر الفاروق"),
                3 to WidgetSlot("رياضيات", "أ. محمد الموسوي"),
                4 to WidgetSlot("إسلامية", "أ. أحمد العلي"),
                5 to WidgetSlot("حاسوب", "أ. ماهر فاضل"),
                6 to WidgetSlot("عربي", "أ. علي الحسين")
            ),
            "الأربعاء" to mapOf(
                1 to WidgetSlot("رياضيات", "أ. محمد الموسوي"),
                2 to WidgetSlot("عربي", "أ. علي الحسين"),
                3 to WidgetSlot("اجتماعيات", "أ. جاسم محمد"),
                4 to WidgetSlot("علوم", "أ. حيدر عباس"),
                5 to WidgetSlot("إنكليزي", "أ. عمر الفاروق"),
                6 to WidgetSlot("فنية", "أ. زينب الجبوري")
            ),
            "الخميس" to mapOf(
                1 to WidgetSlot("إنكليزي", "أ. عمر الفاروق"),
                2 to WidgetSlot("رياضيات", "أ. محمد الموسوي"),
                3 to WidgetSlot("إسلامية", "أ. أحمد العلي"),
                4 to WidgetSlot("عربي", "أ. علي الحسين"),
                5 to WidgetSlot("علوم", "أ. حيدر عباس"),
                6 to WidgetSlot("نشاط مدرسي", "إدارة المدرسة")
            )
        )

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                val studentGrade = prefs.getString("student_grade", "الصف الأول المتوسط") ?: "الصف الأول المتوسط"
                val studentSection = prefs.getString("student_section", "أ") ?: "أ"
                val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
                val customOverridesJson = prefs.getString("custom_subject_overrides", "{}") ?: "{}"
                val activeHwJson = prefs.getString("active_homework_subjects", "[]") ?: "[]"

                val views = RemoteViews(context.packageName, R.layout.widget_student_schedule)

                val selectedDayIdx = prefs.getInt("widget_selected_day_idx", -1)
                val dayIdx = if (selectedDayIdx in 0..4) selectedDayIdx else getDefaultDayIndex()
                val targetDayArabic = SCHOOL_DAYS[dayIdx]

                views.setTextViewText(R.id.widget_day_title, targetDayArabic)

                // Read active homework subjects from preferences + SQLite Room DB
                val activeHwSet = try {
                    val type = object : TypeToken<Set<String>>() {}.type
                    Gson().fromJson<Set<String>>(activeHwJson, type) ?: emptySet()
                } catch (e: Exception) {
                    emptySet<String>()
                }

                val dbHwSet = try {
                    val db = androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        com.example.theboss.data.local.AppDatabase::class.java,
                        "the_boss_db"
                    ).build()
                    val cursor = db.openHelper.readableDatabase.query(
                        "SELECT DISTINCT subjectName FROM assignments WHERE isCompleted = 0"
                    )
                    val set = mutableSetOf<String>()
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) set.add(name)
                    }
                    cursor.close()
                    set
                } catch (e: Exception) {
                    emptySet<String>()
                }

                val allActiveHw = activeHwSet + dbHwSet

                fun hasHomework(subject: String): Boolean {
                    if (subject.isBlank() || subject == "شاغر") return false
                    val clean = shortenSubject(subject).lowercase()
                    val normClean = normDay(clean)
                    return allActiveHw.any {
                        val s = shortenSubject(it).lowercase()
                        val normS = normDay(s)
                        normS.contains(normClean) || normClean.contains(normS)
                    }
                }

                var displayGrade = studentGrade
                var displaySection = studentSection
                val slots = mutableMapOf<Int, WidgetSlot>()
                var foundSchedule = false

                try {
                    val gson = Gson()
                    val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
                    if (rootObj != null) {
                        val stdS = standardizeSectionName(studentSection)
                        val dayData = rootObj.entries.find { normDay(it.key) == normDay(targetDayArabic) }?.value as? List<*>

                        if (dayData != null) {
                            // Pass 1: Strict Grade + Section match
                            for (row in dayData) {
                                if (row is Map<*, *>) {
                                    val g = row["grade"]?.toString() ?: ""
                                    val s = row["section"]?.toString() ?: ""
                                    val rowS = standardizeSectionName(s)

                                    if (isGradeMatch(g, studentGrade) && (rowS == stdS || stdS == "الكل" || rowS.isEmpty())) {
                                        val lessons = row["lessons"] as? Map<*, *>
                                        if (lessons != null) {
                                            for (i in 1..6) {
                                                val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                                val rawSubj = lessonObj?.get("subject")?.toString() ?: ""
                                                val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                                val isOff = lessonObj?.get("isOff") == true || lessonObj?.get("isOff")?.toString() == "true"
                                                if (!isOff && (rawSubj.isNotBlank() || teacher.isNotBlank())) {
                                                    slots[i] = WidgetSlot(shortenSubject(rawSubj), teacher)
                                                    foundSchedule = true
                                                }
                                            }
                                            if (foundSchedule) {
                                                displayGrade = g
                                                displaySection = s
                                                break
                                            }
                                        }
                                    }
                                }
                            }

                            // Pass 2: Match Grade only
                            if (!foundSchedule) {
                                for (row in dayData) {
                                    if (row is Map<*, *>) {
                                        val g = row["grade"]?.toString() ?: ""
                                        val s = row["section"]?.toString() ?: ""
                                        if (isGradeMatch(g, studentGrade)) {
                                            val lessons = row["lessons"] as? Map<*, *>
                                            if (lessons != null) {
                                                for (i in 1..6) {
                                                    val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                                    val rawSubj = lessonObj?.get("subject")?.toString() ?: ""
                                                    val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                                    val isOff = lessonObj?.get("isOff") == true || lessonObj?.get("isOff")?.toString() == "true"
                                                    if (!isOff && (rawSubj.isNotBlank() || teacher.isNotBlank())) {
                                                        slots[i] = WidgetSlot(shortenSubject(rawSubj), teacher)
                                                        foundSchedule = true
                                                    }
                                                }
                                                if (foundSchedule) {
                                                    displayGrade = g
                                                    displaySection = s
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Pass 3: Auto-detect first populated class in school schedule if student grade not found
                            if (!foundSchedule && dayData.isNotEmpty()) {
                                for (rawRow in dayData) {
                                    if (rawRow is Map<*, *>) {
                                        val g = rawRow["grade"]?.toString() ?: ""
                                        val s = rawRow["section"]?.toString() ?: ""
                                        val lessons = rawRow["lessons"] as? Map<*, *>
                                        if (lessons != null) {
                                            var count = 0
                                            for (i in 1..6) {
                                                val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                                val rawSubj = lessonObj?.get("subject")?.toString() ?: ""
                                                val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                                val isOff = lessonObj?.get("isOff") == true || lessonObj?.get("isOff")?.toString() == "true"
                                                if (!isOff && (rawSubj.isNotBlank() || teacher.isNotBlank())) {
                                                    slots[i] = WidgetSlot(shortenSubject(rawSubj), teacher)
                                                    count++
                                                }
                                            }
                                            if (count > 0) {
                                                foundSchedule = true
                                                displayGrade = g
                                                displaySection = s
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing schedule: ${e.message}")
                }

                // Pass 4: Fallback to defaultLessonsMap
                if (!foundSchedule) {
                    val def = defaultLessonsMap[targetDayArabic] ?: emptyMap()
                    slots.putAll(def)
                }

                val cleanGrdDisplay = displayGrade.replace("^(الصف|صف)\\s+".toRegex(), "")
                views.setTextViewText(R.id.widget_student_info, "🎒 $cleanGrdDisplay ($displaySection)")

                // Apply custom subject overrides if any
                try {
                    val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
                    val customMap: Map<String, Map<String, String>> = Gson().fromJson(customOverridesJson, type) ?: emptyMap()
                    val dayOverrides = customMap[targetDayArabic]
                    if (dayOverrides != null) {
                        for (i in 1..6) {
                            val overrideSubj = dayOverrides[i.toString()]
                            if (!overrideSubj.isNullOrBlank()) {
                                val currentTeacher = slots[i]?.teacherName ?: ""
                                slots[i] = WidgetSlot(shortenSubject(overrideSubj), currentTeacher)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Notice customOverrides: ${e.message}")
                }

                val colIds = listOf(
                    R.id.col_student_lesson_1, R.id.col_student_lesson_2, R.id.col_student_lesson_3,
                    R.id.col_student_lesson_4, R.id.col_student_lesson_5, R.id.col_student_lesson_6
                )
                val numIds = listOf(
                    R.id.student_num_1, R.id.student_num_2, R.id.student_num_3,
                    R.id.student_num_4, R.id.student_num_5, R.id.student_num_6
                )
                val subjIds = listOf(
                    R.id.student_subject_1, R.id.student_subject_2, R.id.student_subject_3,
                    R.id.student_subject_4, R.id.student_subject_5, R.id.student_subject_6
                )
                val hwIds = listOf(
                    R.id.student_hw_1, R.id.student_hw_2, R.id.student_hw_3,
                    R.id.student_hw_4, R.id.student_hw_5, R.id.student_hw_6
                )

                for (i in 1..6) {
                    val slot = slots[i]
                    val colId = colIds[i - 1]
                    val numId = numIds[i - 1]
                    val subjId = subjIds[i - 1]
                    val hwId = hwIds[i - 1]

                    if (slot != null && slot.subject.isNotBlank() && slot.subject != "شاغر") {
                        views.setTextViewText(subjId, slot.subject)

                        val isHomework = hasHomework(slot.subject)
                        if (isHomework) {
                            // Highlight with Warm Amber Gold matching in-app timetable design
                            views.setInt(colId, "setBackgroundResource", R.drawable.widget_student_prep_bg)
                            views.setTextColor(numId, Color.parseColor("#D97706"))
                            views.setTextColor(subjId, Color.parseColor("#78350F"))
                            views.setTextViewText(hwId, "📝 واجب")
                            views.setTextColor(hwId, Color.parseColor("#B45309"))
                        } else {
                            // Standard lesson card
                            views.setInt(colId, "setBackgroundResource", R.drawable.widget_student_card_bg)
                            views.setTextColor(numId, Color.parseColor("#38BDF8"))
                            views.setTextColor(subjId, Color.WHITE)
                            val subText = slot.teacherName.ifBlank { "-" }
                            views.setTextViewText(hwId, subText)
                            views.setTextColor(hwId, Color.parseColor("#93C5FD"))
                        }
                    } else {
                        // Empty / vacant
                        views.setInt(colId, "setBackgroundResource", R.drawable.widget_student_empty_bg)
                        views.setTextColor(numId, Color.parseColor("#475569"))
                        views.setTextViewText(subjId, "شاغر")
                        views.setTextColor(subjId, Color.parseColor("#64748B"))
                        views.setTextViewText(hwId, "-")
                        views.setTextColor(hwId, Color.parseColor("#475569"))
                    }
                }

                // Click handlers
                val prevIntent = Intent(context, StudentScheduleWidgetProvider::class.java).apply {
                    action = ACTION_PREV_DAY
                    setPackage(context.packageName)
                }
                val prevPending = PendingIntent.getBroadcast(
                    context, 201, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev_day, prevPending)

                val nextIntent = Intent(context, StudentScheduleWidgetProvider::class.java).apply {
                    action = ACTION_NEXT_DAY
                    setPackage(context.packageName)
                }
                val nextPending = PendingIntent.getBroadcast(
                    context, 202, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next_day, nextPending)

                val refreshIntent = Intent(context, StudentScheduleWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                    setPackage(context.packageName)
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context, 203, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)

                val clickIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_student_info, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (t: Throwable) {
                Log.e(TAG, "Fatal error updating widget: ${t.message}", t)
            }
        }
    }
}
