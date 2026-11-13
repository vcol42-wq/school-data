package com.school.system.utils

import android.content.Context

enum class AppRole(val key: String, val titleAr: String) {
    TEACHER("teacher", "أستاذ / كادر تعليمي"),
    STUDENT("student", "طالب / ولي أمر"),
    PRINCIPAL("principal", "المدير / الإدارة المدرسية")
}

object RoleManager {
    private const val PREFS_NAME = "app_portal_prefs"
    private const val KEY_SELECTED_ROLE = "selected_role"
    private const val KEY_ROLE_LOCKED = "role_locked"

    fun getSelectedRole(context: Context): AppRole? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roleKey = prefs.getString(KEY_SELECTED_ROLE, null) ?: return null
        return try {
            AppRole.valueOf(roleKey)
        } catch (e: Exception) {
            when (roleKey.lowercase()) {
                "teacher" -> AppRole.TEACHER
                "student" -> AppRole.STUDENT
                "principal", "supervisor" -> AppRole.PRINCIPAL
                else -> null
            }
        }
    }

    fun setSelectedRole(context: Context, role: AppRole) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_ROLE, role.name).apply()
    }

    fun isRoleLocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ROLE_LOCKED, false)
    }

    fun lockRole(context: Context, role: AppRole) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SELECTED_ROLE, role.name)
            .putBoolean(KEY_ROLE_LOCKED, true)
            .apply()
    }

    fun clearSelectedRole(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ROLE_LOCKED, false)) {
            prefs.edit().remove(KEY_SELECTED_ROLE).apply()
        }
    }

    fun resetRoleBeforeLock(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ROLE_LOCKED, false)) {
            prefs.edit()
                .remove(KEY_SELECTED_ROLE)
                .remove(KEY_ROLE_LOCKED)
                .apply()
        }
    }

    /**
     * إعادة ضبط البوابة والعودة لشاشة الاختيار الثلاثية (طالب • أستاذ • مدير) حتى بعد القفل
     */
    fun resetToRoleSelection(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_SELECTED_ROLE)
            .remove(KEY_ROLE_LOCKED)
            .apply()
    }

    fun isRoleSelected(context: Context): Boolean {
        return getSelectedRole(context) != null
    }
}
