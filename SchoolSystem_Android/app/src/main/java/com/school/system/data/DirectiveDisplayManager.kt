package com.school.system.data

import android.content.Context
import android.content.SharedPreferences

/**
 * مدير ظهور وأرشفة رسائل وتوجيهات الإدارة المدرسية.
 * يحدد صلاحية العرض التلقائي لكل رسالة (بحد أقصى 3 مرات) أو عند قيام المستخدم بإخفائها/قراءتها.
 * يحفظ الرسائل التاريخية في الأرشيف للرجوع إليها بأي وقت.
 */
object DirectiveDisplayManager {
    private const val PREFS_NAME = "directive_display_prefs"
    private const val KEY_VIEW_COUNT_PREFIX = "view_count_"
    private const val KEY_DISMISSED_PREFIX = "dismissed_"
    const val MAX_AUTO_DISPLAY_COUNT = 3

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getViewCount(context: Context, directiveId: String): Int {
        if (directiveId.isBlank()) return 0
        return getPrefs(context).getInt(KEY_VIEW_COUNT_PREFIX + directiveId, 0)
    }

    fun incrementViewCount(context: Context, directiveId: String): Int {
        if (directiveId.isBlank()) return 0
        val prefs = getPrefs(context)
        val newCount = prefs.getInt(KEY_VIEW_COUNT_PREFIX + directiveId, 0) + 1
        prefs.edit().putInt(KEY_VIEW_COUNT_PREFIX + directiveId, newCount).apply()
        return newCount
    }

    fun isDismissed(context: Context, directiveId: String): Boolean {
        if (directiveId.isBlank()) return false
        return getPrefs(context).getBoolean(KEY_DISMISSED_PREFIX + directiveId, false)
    }

    fun setDismissed(context: Context, directiveId: String, dismissed: Boolean = true) {
        if (directiveId.isBlank()) return
        getPrefs(context).edit().putBoolean(KEY_DISMISSED_PREFIX + directiveId, dismissed).apply()
    }

    fun dismissAll(context: Context, directiveIds: List<String>) {
        val editor = getPrefs(context).edit()
        for (id in directiveIds) {
            if (id.isNotBlank()) {
                editor.putBoolean(KEY_DISMISSED_PREFIX + id, true)
            }
        }
        editor.apply()
    }

    /**
     * هل يحق لهذه الرسالة الظهور التلقائي في واجهة التطبيق الرئيسية؟
     * تظهر فقط إذا لم تُؤرشف يدوياً وكان عدد مرات عرضها أقل من الحد الأقصى (3 مرات).
     */
    fun shouldAutoDisplay(context: Context, directiveId: String): Boolean {
        if (directiveId.isBlank()) return false
        if (isDismissed(context, directiveId)) return false
        val count = getViewCount(context, directiveId)
        return count < MAX_AUTO_DISPLAY_COUNT
    }

    /**
     * تصفية قائمة الرسائل وإبقاء الرسائل الجديدة المؤهلة للعرض التلقائي فقط.
     */
    fun filterForAutoDisplay(context: Context, directives: List<SupabaseDirectiveDto>): List<SupabaseDirectiveDto> {
        return directives.filter { shouldAutoDisplay(context, it.id) }
    }
}
