package com.school.system.widget

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Locale

data class LessonTimeInfo(
    val lessonNumber: Int,
    val className: String,
    val subject: String,
    val teacherName: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)

data class UpcomingLessonResult(
    val lessonNumber: Int,
    val className: String,
    val subject: String,
    val remainingMinutes: Int,
    val isOngoing: Boolean,
    val isUrgent: Boolean, // < 5 minutes
    val hasNoMoreLessons: Boolean,
    val isWeekend: Boolean,
    val nextDayName: String = "",
    val formattedTime: String = "",
    val isFutureDay: Boolean = false
)

object WidgetScheduleHelper {

    fun cleanSubjectName(raw: String): String {
        var text = raw.trim()
        if (text.contains("انكليز") || text.contains("إنجليز") || text.contains("انجليز") || text.lowercase().contains("english")) {
            return "E"
        }
        if (text.contains("اجتماعيات") || text.contains("إجتماعيات") || text == "اجتماع") {
            return "اجتماعيات"
        }
        text = text.replace("التربية ", "")
            .replace("تربية ", "")
            .replace("اللغة ", "")
            .replace("لغة ", "")
        
        if (text.startsWith("ال") && text.length > 3) {
            text = text.substring(2)
        }
        return text.trim().ifEmpty { raw }
    }

    fun formatClassLabelForWidget(raw: String): String {
        val text = raw.trim()

        var sectionLetter = ""
        val secMatch = "\\b([أبجدـهوهزحطيكلمنسعفضقرشتثخذضظغa-zA-Z])\\b|\\(([أبجدـهوهزحطيكلمنسعفضقرشتثخذضظغa-zA-Z])\\)".toRegex().find(text)
        if (secMatch != null) {
            sectionLetter = (secMatch.groupValues[1].ifEmpty { secMatch.groupValues[2] }).trim()
        }

        val digit = text.firstOrNull { it.isDigit() }?.toString() ?: ""
        val wordGrade = when {
            text.contains("أول") || text.contains("الاول") || text.contains("الأول") || digit == "1" -> "اول"
            text.contains("ثاني") || text.contains("الثاني") || digit == "2" -> "ثاني"
            text.contains("ثالث") || text.contains("الثالث") || digit == "3" -> "ثالث"
            text.contains("رابع") || text.contains("الرابع") || digit == "4" -> "رابع"
            text.contains("خامس") || text.contains("الخامس") || digit == "5" -> "خامس"
            text.contains("سادس") || text.contains("السادس") || digit == "6" -> "سادس"
            else -> ""
        }

        return when {
            wordGrade.isNotEmpty() && sectionLetter.isNotEmpty() -> "$wordGrade $sectionLetter"
            wordGrade.isNotEmpty() -> wordGrade
            else -> text.replace("الصف ", "").replace("صف ", "").replace("(", "").replace(")", "").trim()
        }
    }

    fun cleanClassName(raw: String): String {
        var text = raw.trim()
        text = text.replace("الصف ", "")
            .replace("صف ", "")
            .replace("(", "").replace(")", "").replace("[", "").replace("]", "")
            .replace("الأول", "1")
            .replace("الاول", "1")
            .replace("أول", "1")
            .replace("الثاني", "2")
            .replace("ثاني", "2")
            .replace("الثالث", "3")
            .replace("ثالث", "3")
            .replace("الرابع", "4")
            .replace("رابع", "4")
            .replace("الخامس", "5")
            .replace("خامس", "5")
            .replace("السادس", "6")
            .replace("سادس", "6")
        return text.trim().ifEmpty { raw }
    }

    fun getClassRank(className: String): Int {
        val str = className.trim()
        val digit = str.firstOrNull { it.isDigit() }?.digitToInt()
        if (digit != null && digit in 1..12) return digit

        return when {
            str.contains("أول") || str.contains("الاول") || str.contains("الأول") || str.contains("اول") -> 1
            str.contains("ثاني") || str.contains("الثاني") -> 2
            str.contains("ثالث") || str.contains("الثالث") -> 3
            str.contains("رابع") || str.contains("الرابع") -> 4
            str.contains("خامس") || str.contains("الخامس") -> 5
            str.contains("سادس") || str.contains("السادس") -> 6
            str.contains("سابع") || str.contains("السابع") -> 7
            str.contains("ثامن") || str.contains("الثامن") -> 8
            str.contains("تاسع") || str.contains("التاسع") -> 9
            str.contains("عاشر") || str.contains("العاشر") -> 10
            else -> 99
        }
    }

    fun compareClassNames(a: String, b: String): Int {
        val rankA = getClassRank(a)
        val rankB = getClassRank(b)

        if (rankA != rankB) {
            return rankA.compareTo(rankB)
        }
        return a.compareTo(b)
    }

    fun cleanTeacherFirstName(raw: String): String {
        var text = raw.trim()
        text = text.replace("أ.", "").replace("م.", "").replace("الاستاذ ", "").replace("الأستاذ ", "").replace("أستاذ ", "").replace("ست ", "").replace("معلم ", "").replace("مدرس ", "")
        val parts = text.split(" ").filter { it.isNotBlank() }
        return parts.firstOrNull() ?: raw
    }

    /**
     * استخراج الاسم الثنائي للأستاذ لحمايته من التطابق الخاطئ عند تشابه الأسماء الأولى (مثل محمد أو أحمد أو علي)
     */
    fun cleanTeacherTwoPartName(raw: String): String {
        var text = raw.trim()
        text = text.replace("أ.", "")
            .replace("م.", "")
            .replace("الاستاذ ", "")
            .replace("الأستاذ ", "")
            .replace("أستاذ ", "")
            .replace("ست ", "")
            .replace("معلم ", "")
            .replace("مدرس ", "")
            .replace("د.", "")
            .replace("دكتور ", "")
            .replace("الشيخ ", "")
        val parts = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0]} ${parts[1]}"
            parts.size == 1 -> parts[0]
            else -> raw.trim()
        }
    }

    fun normArabic(s: String): String {
        return s.replace("[أإآ]".toRegex(), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .trim()
            .lowercase()
    }

    data class EnrolledClassKey(val gradeNorm: String, val sectionNorm: String, val subjectNorm: String)

    /**
     * استرجاع الشعب والمواد التي قام الأستاذ باختيارها وتنزيل أسمائها فعلياً إلى جهازه
     */
    fun getTeacherEnrolledClasses(context: Context): List<EnrolledClassKey> {
        val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
        val cached = prefs.getStringSet("cached_teacher_enrolled_classes", null)
        if (cached != null && cached.isNotEmpty()) {
            return cached.mapNotNull { item ->
                val p = item.split("::")
                if (p.size >= 2) EnrolledClassKey(p[0], p[1], if (p.size > 2) p[2] else "") else null
            }
        }
        return try {
            val db = com.school.system.data.SchoolDatabase.getInstance(context)
            val pkgs = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                db.classPackageDao().getAllPackagesList()
            }
            if (pkgs.isNotEmpty()) {
                val list = pkgs.map {
                    EnrolledClassKey(
                        gradeNorm = normArabic(cleanClassName(it.grade)),
                        sectionNorm = normArabic(it.section.trim()),
                        subjectNorm = normArabic(cleanSubjectName(it.subject))
                    )
                }
                val set = list.map { "${it.gradeNorm}::${it.sectionNorm}::${it.subjectNorm}" }.toSet()
                prefs.edit().putStringSet("cached_teacher_enrolled_classes", set).apply()
                list
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * مطابقة دقيقة وذكية لدروس الأستاذ في الودجت:
     * 1. حصر التضليل بالشعب التي اختارها الأستاذ فقط (Enrolled Classes).
     * 2. اعتماد الاسم الثنائي للأستاذ لمنع تكرار التضليل عند تشابه الأسماء الأولى.
     * 3. منع مطابقة اسم الأستاذ مع اسم المادة نهائياً لتفادي خلط المعلمين.
     */
    fun isLessonMatchingTeacherStrict(
        context: Context,
        rawGrade: String,
        rawSection: String,
        rawSubject: String,
        rawTeacher: String,
        teacherConfigName: String
    ): Boolean {
        val cleanTeacherCfg = cleanTeacherTwoPartName(teacherConfigName)
        val normTeacherCfg = normArabic(cleanTeacherCfg)
        if (normTeacherCfg.isEmpty()) return false

        val enrolledClasses = getTeacherEnrolledClasses(context)
        val rowGradeNorm = normArabic(cleanClassName(rawGrade))
        val rowSecNorm = normArabic(rawSection.trim())
        val cellSubjNorm = normArabic(cleanSubjectName(rawSubject))
        val cellTeacherNorm = normArabic(cleanTeacherTwoPartName(rawTeacher))

        // 1. إذا كان الأستاذ قد نزل شعباً ومواد، نحصر التضليل حتماً في هذه الشعب دون غيرها
        if (enrolledClasses.isNotEmpty()) {
            val matchingEnrolledClass = enrolledClasses.firstOrNull { enrolled ->
                val gradeMatches = rowGradeNorm == enrolled.gradeNorm ||
                        rowGradeNorm.contains(enrolled.gradeNorm) ||
                        enrolled.gradeNorm.contains(rowGradeNorm)

                val secMatches = rowSecNorm.equals(enrolled.sectionNorm, ignoreCase = true) ||
                        (rowSecNorm.isNotEmpty() && enrolled.sectionNorm.isNotEmpty() && rowSecNorm.first() == enrolled.sectionNorm.first())

                gradeMatches && secMatches
            }

            // إذا كانت هذه الشعبة ليست من شعب الأستاذ المختارة، نرفضها فوراً
            if (matchingEnrolledClass == null) {
                return false
            }

            // داخل الشعبة الخاصة به:
            // أ) مطابقة الاسم الثنائي للأستاذ
            if (cellTeacherNorm.isNotEmpty()) {
                val matchesTwoPart = (cellTeacherNorm == normTeacherCfg) ||
                        (normTeacherCfg.length >= 4 && cellTeacherNorm.contains(normTeacherCfg)) ||
                        (cellTeacherNorm.length >= 4 && normTeacherCfg.contains(cellTeacherNorm))
                if (matchesTwoPart) return true
            }

            // ب) أو إذا كان اسم المدرس في الخلية فارغاً، نتأكد من تطابق المادة مع المادة المسندة له في هذه الشعبة
            if (cellSubjNorm.isNotEmpty() && matchingEnrolledClass.subjectNorm.isNotEmpty()) {
                if (cellSubjNorm == matchingEnrolledClass.subjectNorm ||
                    cellSubjNorm.contains(matchingEnrolledClass.subjectNorm) ||
                    matchingEnrolledClass.subjectNorm.contains(cellSubjNorm)
                ) {
                    return true
                }
            }
            return false
        }

        // 2. إذا لم تكن هناك شعب منزلة بعد، نعتمد حصراً على الاسم الثنائي المكتوب في الخلية
        if (cellTeacherNorm.isNotEmpty()) {
            return (cellTeacherNorm == normTeacherCfg) ||
                    (normTeacherCfg.length >= 4 && cellTeacherNorm.contains(normTeacherCfg)) ||
                    (cellTeacherNorm.length >= 4 && normTeacherCfg.contains(cellTeacherNorm))
        }

        return false
    }

    fun isLessonMatchingTeacher(input: String, teacher: String, subject: String): Boolean {
        val q = normArabic(cleanTeacherTwoPartName(input))
        if (q.isEmpty()) return true

        val normTeacher = normArabic(cleanTeacherTwoPartName(teacher))
        if (normTeacher.isNotEmpty()) {
            return normTeacher == q || (q.length >= 4 && normTeacher.contains(q)) || (normTeacher.length >= 4 && q.contains(normTeacher))
        }
        return false
    }

    val SCHOOL_DAYS = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

    fun getRealCurrentDayArabic(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "الأحد"
            Calendar.MONDAY -> "الإثنين"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.THURSDAY -> "الخميس"
            else -> ""
        }
    }

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
                // If past 12:45 PM on Thursday, show Sunday (0)
                if (hourOfDay > 12 || (hourOfDay == 12 && minute >= 45)) 0 else 4
            }
            Calendar.FRIDAY, Calendar.SATURDAY -> 0 // Weekend -> show Sunday
            else -> 0
        }
    }

    fun getEffectiveDayArabic(context: Context): String {
        val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
        val selectedIdx = prefs.getInt("widget_selected_day_idx", -1)
        val dayIdx = if (selectedIdx in 0..4) selectedIdx else getDefaultDayIndex()
        return SCHOOL_DAYS[dayIdx]
    }

    data class ScheduleTimingInfo(
        val startHour: String,
        val lessonDuration: Int,
        val breakDuration: Int,
        val customLessons: List<Int> = emptyList(),
        val customBreaks: List<Int> = emptyList()
    )

    fun getFullTimingFromScheduleJson(context: Context): ScheduleTimingInfo {
        return try {
            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
            val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            val rootObj = Gson().fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            val timingObj = rootObj?.get("_timing") as? Map<*, *>

            val startHour = timingObj?.get("schoolStartHour")?.toString()
                ?.ifBlank { null }
                ?: prefs.getString("school_start_hour", null)
                ?: "08:00"

            val lessonDur = (timingObj?.get("lessonDurationMinutes") as? Number)?.toInt()
                ?: prefs.getInt("lesson_duration_minutes", 0).takeIf { it > 0 }
                ?: prefs.getInt("bell_lesson_duration", 0).takeIf { it > 0 }
                ?: 45

            val breakDur = (timingObj?.get("breakDurationMinutes") as? Number)?.toInt()
                ?: prefs.getInt("break_duration_minutes", 0).takeIf { it > 0 }
                ?: prefs.getInt("bell_break_duration", 0).takeIf { it > 0 }
                ?: 10

            var customLessons = (timingObj?.get("customLessonDurations") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
            if (customLessons.isNullOrEmpty()) {
                val cached = prefs.getString("custom_lesson_durations", null)
                if (!cached.isNullOrBlank()) {
                    customLessons = Gson().fromJson<List<Int>>(cached, object : TypeToken<List<Int>>() {}.type)
                }
            }

            var customBreaks = (timingObj?.get("customBreakDurations") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
            if (customBreaks.isNullOrEmpty()) {
                val cached = prefs.getString("custom_break_durations", null)
                if (!cached.isNullOrBlank()) {
                    customBreaks = Gson().fromJson<List<Int>>(cached, object : TypeToken<List<Int>>() {}.type)
                }
            }

            ScheduleTimingInfo(startHour, lessonDur, breakDur, customLessons ?: emptyList(), customBreaks ?: emptyList())
        } catch (e: Exception) {
            ScheduleTimingInfo("08:00", 45, 10, emptyList(), emptyList())
        }
    }

    fun getTimingFromScheduleJson(context: Context): Triple<String, Int, Int> {
        val info = getFullTimingFromScheduleJson(context)
        return Triple(info.startHour, info.lessonDuration, info.breakDuration)
    }

    fun calculateLessonTiming(
        lessonNumber: Int,
        startHourStr: String = "08:00",
        lessonDuration: Int = 45,
        breakDuration: Int = 10,
        isShort: Boolean = true,
        customLessons: List<Int> = emptyList(),
        customBreaks: List<Int> = emptyList()
    ): String {
        if (lessonNumber < 1) return ""
        val parts = startHourStr.split(":").mapNotNull { it.toIntOrNull() }
        val startH = if (parts.isNotEmpty()) parts[0] else 8
        val startM = if (parts.size > 1) parts[1] else 0

        var currentTotalMinutes = startH * 60 + startM
        for (i in 1 until lessonNumber) {
            val lDur = customLessons.getOrNull(i - 1) ?: lessonDuration
            val bDur = customBreaks.getOrNull(i - 1) ?: breakDuration
            currentTotalMinutes += lDur + bDur
        }

        val lDurCurrent = customLessons.getOrNull(lessonNumber - 1) ?: lessonDuration
        val lessonStartMin = currentTotalMinutes
        val lessonEndMin = currentTotalMinutes + lDurCurrent

        fun formatMin(min: Int): String {
            var h = min / 60
            val m = min % 60
            var period = if (h in 12..23) "م" else "ص"
            h %= 12
            if (h == 0) h = 12
            val timeStr = String.format(Locale.US, "%02d:%02d", h, m)
            return if (isShort) timeStr else "$timeStr $period"
        }

        return "${formatMin(lessonStartMin)} - ${formatMin(lessonEndMin)}"
    }

    fun getCurrentActiveLessonNumber(context: Context): Int {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) return 0

        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val timing = getFullTimingFromScheduleJson(context)
        val parts = timing.startHour.split(":").mapNotNull { it.toIntOrNull() }
        val startH = if (parts.isNotEmpty()) parts[0] else 8
        val startM = if (parts.size > 1) parts[1] else 0

        var currentTotalMinutes = startH * 60 + startM
        for (i in 1..7) {
            val lDur = timing.customLessons.getOrNull(i - 1) ?: timing.lessonDuration
            val bDur = timing.customBreaks.getOrNull(i - 1) ?: timing.breakDuration

            val lessonStartMin = currentTotalMinutes
            val lessonEndMin = currentTotalMinutes + lDur

            if (nowMinutes in lessonStartMin..lessonEndMin) {
                return i
            }
            currentTotalMinutes = lessonEndMin + bDur
        }
        return 0
    }

    fun getTodayTeacherLessons(context: Context): List<LessonTimeInfo> {
        val dayArabic = getEffectiveDayArabic(context)
        return getTeacherLessonsForDay(context, dayArabic)
    }

    fun getTeacherLessonsForDay(context: Context, dayArabic: String): List<LessonTimeInfo> {
        val result = mutableListOf<LessonTimeInfo>()
        try {
            val fullTiming = getFullTimingFromScheduleJson(context)
            val startHourStr = fullTiming.startHour
            val lessonD = fullTiming.lessonDuration
            val breakD = fullTiming.breakDuration
            val customLessons = fullTiming.customLessons
            val customBreaks = fullTiming.customBreaks

            val parts = startHourStr.split(":").mapNotNull { it.toIntOrNull() }
            val startH = if (parts.isNotEmpty()) parts[0] else 8
            val startM = if (parts.size > 1) parts[1] else 0

            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
            val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            var teacherNameInput = prefs.getString("teacher_name", "")?.trim() ?: ""
            if (teacherNameInput.isEmpty()) {
                teacherNameInput = prefs.getString("user_name", "")?.trim() ?: ""
            }

            val gson = Gson()
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type) ?: return emptyList()

            val dayData = rootObj[dayArabic] ?: return emptyList()
            val lessonsByNumber = mutableMapOf<Int, Pair<String, String>>()

            if (dayData is List<*>) {
                for (row in dayData) {
                    if (row is Map<*, *>) {
                        val grade = row["grade"]?.toString() ?: ""
                        val section = row["section"]?.toString() ?: ""
                        val className = cleanClassName("$grade ($section)".trim())
                        val lessons = row["lessons"] as? Map<*, *>
                        if (lessons != null) {
                            for (i in 1..7) {
                                val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                val subjRaw = lessonObj?.get("subject")?.toString() ?: ""
                                val teacherRaw = lessonObj?.get("teacherName")?.toString() ?: ""
                                val isOff = lessonObj?.get("isOff") as? Boolean ?: false

                                if (!isOff && (subjRaw.isNotEmpty() || teacherRaw.isNotEmpty())) {
                                    if (isLessonMatchingTeacherStrict(context, grade, section, subjRaw, teacherRaw, teacherNameInput)) {
                                        lessonsByNumber[i] = className to cleanSubjectName(subjRaw)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (dayData is Map<*, *>) {
                for ((clsKey, lessonsList) in dayData) {
                    val className = cleanClassName(clsKey.toString())
                    if (lessonsList is List<*>) {
                        lessonsList.forEachIndexed { idx, item ->
                            val itemStr = item?.toString() ?: ""
                            if (itemStr.isNotEmpty() && itemStr != "-") {
                                if (isLessonMatchingTeacherStrict(context, className, "", itemStr, "", teacherNameInput)) {
                                    lessonsByNumber[idx + 1] = className to cleanSubjectName(itemStr)
                                }
                            }
                        }
                    }
                }
            }

            var currentTotalMinutes = startH * 60 + startM
            for (i in 1..7) {
                val lDur = customLessons.getOrNull(i - 1) ?: lessonD
                val bDur = customBreaks.getOrNull(i - 1) ?: breakD

                val lessonStartH = currentTotalMinutes / 60
                val lessonStartM = currentTotalMinutes % 60
                val lessonEndTotal = currentTotalMinutes + lDur
                val lessonEndH = lessonEndTotal / 60
                val lessonEndM = lessonEndTotal % 60

                val lessonData = lessonsByNumber[i]
                if (lessonData != null) {
                    result.add(
                        LessonTimeInfo(
                            lessonNumber = i,
                            className = lessonData.first,
                            subject = lessonData.second,
                            teacherName = cleanTeacherFirstName(teacherNameInput),
                            startHour = lessonStartH,
                            startMinute = lessonStartM,
                            endHour = lessonEndH,
                            endMinute = lessonEndM
                        )
                    )
                }
                currentTotalMinutes = lessonEndTotal + bDur
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun calculateUpcomingTeacherLesson(context: Context): UpcomingLessonResult {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val daysList = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

        val currentDayIndex = when (dayOfWeek) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            else -> -1 // Friday or Saturday
        }

        // 1. Check today's lessons if today is a school day
        if (currentDayIndex in 0..4) {
            val todayArabic = daysList[currentDayIndex]
            val todayLessons = getTeacherLessonsForDay(context, todayArabic)

            // Check if currently inside a lesson
            for (l in todayLessons) {
                val startTotal = l.startHour * 60 + l.startMinute
                val endTotal = l.endHour * 60 + l.endMinute

                if (nowMinutes in startTotal..endTotal) {
                    val remainingInLesson = endTotal - nowMinutes
                    val formatTime = String.format("%02d:%02d", l.startHour, l.startMinute)
                    return UpcomingLessonResult(
                        lessonNumber = l.lessonNumber,
                        className = l.className,
                        subject = l.subject,
                        remainingMinutes = remainingInLesson,
                        isOngoing = true,
                        isUrgent = remainingInLesson <= 5,
                        hasNoMoreLessons = false,
                        isWeekend = false,
                        nextDayName = todayArabic,
                        formattedTime = formatTime,
                        isFutureDay = false
                    )
                }
            }

            // Find next upcoming lesson today
            val upcomingToday = todayLessons.firstOrNull { (it.startHour * 60 + it.startMinute) > nowMinutes }
            if (upcomingToday != null) {
                val startTotal = upcomingToday.startHour * 60 + upcomingToday.startMinute
                val remainingUntilStart = startTotal - nowMinutes
                val formatTime = String.format("%02d:%02d", upcomingToday.startHour, upcomingToday.startMinute)
                return UpcomingLessonResult(
                    lessonNumber = upcomingToday.lessonNumber,
                    className = upcomingToday.className,
                    subject = upcomingToday.subject,
                    remainingMinutes = remainingUntilStart,
                    isOngoing = false,
                    isUrgent = remainingUntilStart <= 5,
                    hasNoMoreLessons = false,
                    isWeekend = false,
                    nextDayName = todayArabic,
                    formattedTime = formatTime,
                    isFutureDay = false
                )
            }
        }

        // 2. Search for the next upcoming lesson on future days in the week
        val startIndex = if (currentDayIndex in 0..4) (currentDayIndex + 1) % 5 else 0
        for (step in 0..4) {
            val nextDayIdx = (startIndex + step) % 5
            val nextDayArabic = daysList[nextDayIdx]
            val dayLessons = getTeacherLessonsForDay(context, nextDayArabic)

            if (dayLessons.isNotEmpty()) {
                val firstLesson = dayLessons.first()
                val formatTime = String.format("%02d:%02d", firstLesson.startHour, firstLesson.startMinute)
                return UpcomingLessonResult(
                    lessonNumber = firstLesson.lessonNumber,
                    className = firstLesson.className,
                    subject = firstLesson.subject,
                    remainingMinutes = 0,
                    isOngoing = false,
                    isUrgent = false,
                    hasNoMoreLessons = false,
                    isWeekend = false,
                    nextDayName = nextDayArabic,
                    formattedTime = formatTime,
                    isFutureDay = true
                )
            }
        }

        return UpcomingLessonResult(
            lessonNumber = 0,
            className = "",
            subject = "لا توجد دروس مسجلة لك",
            remainingMinutes = 0,
            isOngoing = false,
            isUrgent = false,
            hasNoMoreLessons = true,
            isWeekend = false,
            nextDayName = "",
            formattedTime = "",
            isFutureDay = false
        )
    }

    /**
     * تحديث فوري لجميع الودجات على الشاشة الرئيسية (الودجت اليومي والودجت الكامل)
     */
    fun updateAllWidgets(context: Context) {
        try {
            // مسح الكاش لإعادة قراءة شعب الأستاذ المحدثة فوراً
            context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
                .edit().remove("cached_teacher_enrolled_classes").apply()

            val intentDaily = Intent(context, DailyScheduleWidgetProvider::class.java).apply {
                action = DailyScheduleWidgetProvider.ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intentDaily)

            val intentFull = Intent(context, FullScheduleWidgetProvider::class.java).apply {
                action = FullScheduleWidgetProvider.ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intentFull)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
