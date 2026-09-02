package com.school.system.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_school_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // حفظ بيانات المدرسة المختارة
    fun saveSchoolSession(schoolId: String, schoolCode: String, schoolName: String) {
        sharedPreferences.edit()
            .putString("KEY_SCHOOL_ID", schoolId)
            .putString("KEY_SCHOOL_CODE", schoolCode)
            .putString("KEY_SCHOOL_NAME", schoolName)
            .apply()
    }

    fun getSchoolId(): String? = sharedPreferences.getString("KEY_SCHOOL_ID", null)
    fun getSchoolCode(): String? = sharedPreferences.getString("KEY_SCHOOL_CODE", null)
    fun getSchoolName(): String? = sharedPreferences.getString("KEY_SCHOOL_NAME", null)

    fun isSchoolConfigured(): Boolean = getSchoolId() != null

    // مسح الجلسة عند الرغبة في تغيير المدرسة أو التبديل
    fun clearSchoolSession() {
        sharedPreferences.edit().clear().apply()
    }
}
