package com.school.system.widget

import android.content.Context
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
        if (text.contains("اجتماعيات") || text.contains("إجتماعيات")) {
            return "اجتماع"
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

    fun compareClassNames(a: String, b: String): Int {
        val cleanA = cleanClassName(a)
        val cleanB = cleanClassName(b)

        val numA = cleanA.firstOrNull { it.isDigit() }?.digitToInt() ?: 99
        val numB = cleanB.firstOrNull { it.isDigit() }?.digitToInt() ?: 99

        if (numA != numB) {
            return numA.compareTo(numB)
        }
        return cleanA.compareTo(cleanB)
    }

    fun cleanTeacherFirstName(raw: String): String {
        var text = raw.trim()
        text = text.replace("أ.", "").replace("م.", "").replace("الاستاذ ", "").replace("الأستاذ ", "").replace("أستاذ ", "")
        val parts = text.split(" ").filter { it.isNotBlank() }
        return parts.firstOrNull() ?: raw
    }

    fun normArabic(s: String): String {
        return s.replace("[أإآ]".toRegex(), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .trim()
            .lowercase()
    }

    fun isLessonMatchingTeacher(input: String, teacher: String, subject: String): Boolean {
        val q = normArabic(input)
        if (q.isEmpty()) return true

        val normTeacher = normArabic(teacher)
        val normSubj = normArabic(subject)
        val cleanTeacher = normArabic(cleanTeacherFirstName(teacher))
        val cleanSubj = normArabic(cleanSubjectName(subject))

        return normTeacher.contains(q) ||
               normSubj.contains(q) ||
               cleanTeacher.contains(q) ||
               cleanSubj.contains(q) ||
               (normTeacher.length >= 3 && q.contains(normTeacher)) ||
               (cleanTeacher.length >= 3 && q.contains(cleanTeacher))
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

    fun getTimingFromScheduleJson(context: Context): Triple<String, Int, Int> {
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

            Triple(startHour, lessonDur, breakDur)
        } catch (e: Exception) {
            Triple("08:00", 45, 10)
        }
    }

    fun calculateLessonTiming(
        lessonNumber: Int,
        startHourStr: String = "08:00",
        lessonDuration: Int = 45,
        breakDuration: Int = 10,
        isShort: Boolean = true
    ): String {
        if (lessonNumber < 1) return ""
        val parts = startHourStr.split(":").mapNotNull { it.toIntOrNull() }
        val startH = if (parts.isNotEmpty()) parts[0] else 8
        val startM = if (parts.size > 1) parts[1] else 0

        var currentTotalMinutes = startH * 60 + startM
        for (i in 1 until lessonNumber) {
            currentTotalMinutes += lessonDuration + breakDuration
        }

        val lessonStartMin = currentTotalMinutes
        val lessonEndMin = currentTotalMinutes + lessonDuration

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

    fun getTodayTeacherLessons(context: Context): List<LessonTimeInfo> {
        val dayArabic = getEffectiveDayArabic(context)
        return getTeacherLessonsForDay(context, dayArabic)
    }

    fun getTeacherLessonsForDay(context: Context, dayArabic: String): List<LessonTimeInfo> {
        val result = mutableListOf<LessonTimeInfo>()
        try {
            val (startHourStr, lessonD, breakD) = getTimingFromScheduleJson(context)
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
                                    if (isLessonMatchingTeacher(teacherNameInput, teacherRaw, subjRaw)) {
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
                                if (isLessonMatchingTeacher(teacherNameInput, "", itemStr)) {
                                    lessonsByNumber[idx + 1] = className to cleanSubjectName(itemStr)
                                }
                            }
                        }
                    }
                }
            }

            var currentTotalMinutes = startH * 60 + startM
            for (i in 1..7) {
                val lessonStartH = currentTotalMinutes / 60
                val lessonStartM = currentTotalMinutes % 60
                val lessonEndTotal = currentTotalMinutes + lessonD
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

                currentTotalMinutes = lessonEndTotal + breakD
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
}
