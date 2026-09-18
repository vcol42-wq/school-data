package com.example.theboss.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_boss_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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

    fun getDeviceId(): String {
        var id = sharedPreferences.getString("KEY_DEVICE_ID", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            sharedPreferences.edit().putString("KEY_DEVICE_ID", id).apply()
        }
        return id
    }

    fun clearSchoolSession() {
        sharedPreferences.edit().clear().apply()
    }
}
