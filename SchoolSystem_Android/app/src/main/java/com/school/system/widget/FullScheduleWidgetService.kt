package com.school.system.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.school.system.R
import java.util.Calendar

class FullScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FullScheduleWidgetFactory(this.applicationContext)
    }
}

data class GridClassRow(
    val className: String,
    val lessons: Map<Int, Pair<String, String>> // lessonNumber -> (subject, teacher)
)

class FullScheduleWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val gridRows = mutableListOf<GridClassRow>()
    private var activeLessonNumber: Int = 0
    private var teacherName: String = ""

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    override fun onDestroy() {
        gridRows.clear()
    }

    override fun getCount(): Int = gridRows.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= gridRows.size) {
            return RemoteViews(context.packageName, R.layout.widget_grid_row)
        }

        val item = gridRows[position]
        val views = RemoteViews(context.packageName, R.layout.widget_grid_row)

        views.setTextViewText(R.id.row_title_text, item.className)

        val cellIds = listOf(
            R.id.cell_lesson_1,
            R.id.cell_lesson_2,
            R.id.cell_lesson_3,
            R.id.cell_lesson_4,
            R.id.cell_lesson_5,
            R.id.cell_lesson_6
        )

        for (i in 1..6) {
            val cellViewId = cellIds[i - 1]
            val lessonPair = item.lessons[i]
            val subj = lessonPair?.first ?: "-"
            val teacher = lessonPair?.second ?: ""

            val display = subj
            views.setTextViewText(cellViewId, display)

            val isTeacherLesson = if (teacherName.isNotEmpty() && subj != "-") {
                WidgetScheduleHelper.isLessonMatchingTeacher(teacherName, teacher, subj)
            } else false

            val isCurrentActive = (i == activeLessonNumber)

            when {
                isCurrentActive && isTeacherLesson -> {
                    views.setTextColor(cellViewId, Color.parseColor("#FEF08A"))
                    views.setInt(cellViewId, "setBackgroundResource", R.drawable.widget_teacher_highlight_bg)
                }
                isCurrentActive -> {
                    views.setTextColor(cellViewId, Color.parseColor("#38BDF8"))
                    views.setInt(cellViewId, "setBackgroundResource", R.drawable.widget_compact_ongoing_bg)
                }
                isTeacherLesson -> {
                    views.setTextColor(cellViewId, Color.WHITE)
                    views.setInt(cellViewId, "setBackgroundResource", R.drawable.widget_teacher_card_bg)
                }
                else -> {
                    views.setTextColor(cellViewId, Color.parseColor("#94A3B8"))
                    views.setInt(cellViewId, "setBackgroundResource", R.drawable.widget_item_bg)
                }
            }
        }

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.row_title_text, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadData() {
        gridRows.clear()
        try {
            val prefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
            val rawScheduleJson = prefs.getString("synced_schedule", "{}") ?: "{}"
            teacherName = prefs.getString("teacher_name", "")?.trim()?.lowercase() ?: ""

            val upcoming = WidgetScheduleHelper.calculateUpcomingTeacherLesson(context)
            activeLessonNumber = if (upcoming.isOngoing) upcoming.lessonNumber else 0

            val currentDayArabic = WidgetScheduleHelper.getEffectiveDayArabic(context)

            val gson = Gson()
            val rootObj = gson.fromJson<Map<String, Any>>(rawScheduleJson, object : TypeToken<Map<String, Any>>() {}.type)

            if (rootObj != null) {
                val dayData = rootObj[currentDayArabic]
                if (dayData is List<*>) {
                    for (row in dayData) {
                        if (row is Map<*, *>) {
                            val grade = row["grade"]?.toString() ?: ""
                            val section = row["section"]?.toString() ?: ""
                            val rawClassName = "$grade ($section)".trim()
                            val className = WidgetScheduleHelper.cleanClassName(rawClassName)
                            val lessons = row["lessons"] as? Map<*, *>
                            val lessonMap = mutableMapOf<Int, Pair<String, String>>()

                            if (lessons != null) {
                                for (i in 1..6) {
                                    val lessonObj = lessons["lesson$i"] as? Map<*, *>
                                    val subj = lessonObj?.get("subject")?.toString() ?: ""
                                    val teacher = lessonObj?.get("teacherName")?.toString() ?: ""
                                    val isOff = lessonObj?.get("isOff") as? Boolean ?: false

                                    if (!isOff && (subj.isNotEmpty() || teacher.isNotEmpty())) {
                                        val cleanSubj = WidgetScheduleHelper.cleanSubjectName(subj)
                                        val cleanTeacher = WidgetScheduleHelper.cleanTeacherFirstName(teacher)
                                        lessonMap[i] = cleanSubj to cleanTeacher
                                    }
                                }
                            }
                            if (className.isNotEmpty()) {
                                gridRows.add(GridClassRow(className, lessonMap))
                            }
                        }
                    }
                } else if (dayData is Map<*, *>) {
                    for ((clsKey, lessonsList) in dayData) {
                        val className = WidgetScheduleHelper.cleanClassName(clsKey.toString())
                        val lessonMap = mutableMapOf<Int, Pair<String, String>>()
                        if (lessonsList is List<*>) {
                            lessonsList.forEachIndexed { idx, item ->
                                val itemStr = item?.toString() ?: ""
                                if (itemStr.isNotEmpty() && itemStr != "-") {
                                    val cleanSubj = WidgetScheduleHelper.cleanSubjectName(itemStr)
                                    lessonMap[idx + 1] = cleanSubj to ""
                                }
                            }
                        }
                        if (className.isNotEmpty()) {
                            gridRows.add(GridClassRow(className, lessonMap))
                        }
                    }
                }
            }

            gridRows.sortWith { r1, r2 -> WidgetScheduleHelper.compareClassNames(r1.className, r2.className) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
