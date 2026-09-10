package com.school.system.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.school.system.R
import java.util.Calendar

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleWidgetFactory(this.applicationContext)
    }
}

data class WidgetLessonItem(
    val lessonNumber: Int,
    val className: String,
    val subject: String,
    val teacherName: String
)

class ScheduleWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val lessonList = mutableListOf<WidgetLessonItem>()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    override fun onDestroy() {
        lessonList.clear()
    }

    override fun getCount(): Int = lessonList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= lessonList.size) {
            return RemoteViews(context.packageName, R.layout.widget_lesson_item)
        }

        val item = lessonList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_lesson_item)

        val subjectDisplay = if (item.subject.isNotEmpty()) item.subject else "درس"
        val classDisplay = if (item.className.isNotEmpty()) item.className else ""

        views.setTextViewText(R.id.item_subject_text, subjectDisplay)
        views.setTextViewText(R.id.item_class_text, classDisplay)
        views.setTextViewText(R.id.item_lesson_badge, "الحصة ${item.lessonNumber}")

        // Fill in intent for item click
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadData() {
        lessonList.clear()
        try {
            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
            val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            val teacherName = prefs.getString("teacher_name", "")?.trim()?.lowercase() ?: ""

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            val currentDayArabic = when (dayOfWeek) {
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الإثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                else -> "الأحد" // Weekend -> Default to Sunday's schedule
            }

            val gson = Gson()
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)
            
            if (rootObj != null) {
                val dayData = rootObj[currentDayArabic]
                if (dayData is List<*>) {
                    for (row in dayData) {
                        if (row is Map<*, *>) {
                            val grade = row["grade"]?.toString() ?: ""
                            val section = row["section"]?.toString() ?: ""
                            val className = "$grade ($section)".trim()
                            val lessons = row["lessons"] as? Map<*, *>
                            if (lessons != null) {
                                for (i in 1..7) {
                                    val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                    val subj = lessonObj?.get("subject")?.toString() ?: ""
                                    val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                    val isOff = lessonObj?.get("isOff") as? Boolean ?: false

                                    if (!isOff && (subj.isNotEmpty() || teacher.isNotEmpty())) {
                                        val isTeacherLesson = if (teacherName.isNotEmpty()) {
                                            teacher.lowercase().contains(teacherName) || subj.lowercase().contains(teacherName)
                                        } else {
                                            true
                                        }

                                        if (isTeacherLesson) {
                                            lessonList.add(
                                                WidgetLessonItem(
                                                    lessonNumber = i,
                                                    className = className,
                                                    subject = subj,
                                                    teacherName = teacher
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (dayData is Map<*, *>) {
                    for ((clsKey, lessonsList) in dayData) {
                        val className = clsKey.toString()
                        if (lessonsList is List<*>) {
                            lessonsList.forEachIndexed { idx, item ->
                                val itemStr = item?.toString() ?: ""
                                if (itemStr.isNotEmpty() && itemStr != "-") {
                                    val isTeacherLesson = if (teacherName.isNotEmpty()) {
                                        itemStr.lowercase().contains(teacherName)
                                    } else {
                                        true
                                    }
                                    if (isTeacherLesson) {
                                        lessonList.add(
                                            WidgetLessonItem(
                                                lessonNumber = idx + 1,
                                                className = className,
                                                subject = itemStr,
                                                teacherName = ""
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sort by lesson number
            lessonList.sortBy { it.lessonNumber }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
