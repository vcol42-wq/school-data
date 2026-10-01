package com.example.theboss.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {

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

        try {
            context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).edit()
                .putString("school_id", schoolId)
                .putString("school_code", schoolCode)
                .putString("school_name", schoolName)
                .apply()

            context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE).edit()
                .putString("diyala_school_id", schoolId)
                .putString("school_id", schoolId)
                .putString("school_name", schoolName)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSchoolId(): String? = sharedPreferences.getString("KEY_SCHOOL_ID", null)
    fun getSchoolCode(): String? = sharedPreferences.getString("KEY_SCHOOL_CODE", null)
    fun getSchoolName(): String? = sharedPreferences.getString("KEY_SCHOOL_NAME", null)

    fun isSchoolConfigured(): Boolean {
        val id = getSchoolId()
        return !id.isNullOrBlank() && id != "school_01"
    }

    fun isIndependentStudent(): Boolean = getSchoolId() == "independent_student"

    fun getDeviceId(): String {
        var id = sharedPreferences.getString("KEY_DEVICE_ID", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            sharedPreferences.edit().putString("KEY_DEVICE_ID", id).apply()
        }
        return id
    }

    fun clearSchoolSession() {
        val deviceId = sharedPreferences.getString("KEY_DEVICE_ID", null)
        sharedPreferences.edit().clear().apply()
        if (deviceId != null) {
            sharedPreferences.edit().putString("KEY_DEVICE_ID", deviceId).apply()
        }

        try {
            context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE).edit()
                .remove("school_id")
                .remove("school_code")
                .remove("school_name")
                .remove("last_saved_school_code")
                .remove("synced_schedule")
                .remove("independent_mode")
                .apply()

            context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE).edit()
                .remove("diyala_school_id")
                .remove("school_id")
                .remove("school_name")
                .remove("synced_schedule")
                .apply()

            context.getSharedPreferences("school_system_prefs", Context.MODE_PRIVATE).edit()
                .remove("school_id")
                .remove("school_name")
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
