package com.principal.system.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "principal_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSchoolCredentials(
        schoolId: String,
        schoolName: String,
        schoolCode: String,
        supabaseUrl: String,
        apiKey: String
    ) {
        prefs.edit()
            .putString("KEY_SCHOOL_ID", schoolId)
            .putString("KEY_SCHOOL_NAME", schoolName)
            .putString("KEY_SCHOOL_CODE", schoolCode)
            .putString("KEY_SUPABASE_URL", supabaseUrl)
            .putString("KEY_API_KEY", apiKey)
            .apply()
    }

    fun getSchoolId(): String? = prefs.getString("KEY_SCHOOL_ID", null)
    fun getSchoolName(): String? = prefs.getString("KEY_SCHOOL_NAME", null)
    fun getSchoolCode(): String? = prefs.getString("KEY_SCHOOL_CODE", null)
    fun getSupabaseUrl(): String = prefs.getString("KEY_SUPABASE_URL", "https://pexehlvkpdhmpukjydwd.supabase.co") ?: "https://pexehlvkpdhmpukjydwd.supabase.co"
    fun getApiKey(): String = prefs.getString("KEY_API_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY") ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY"

    fun isConfigured(): Boolean = !getSchoolId().isNullOrBlank() && !getSupabaseUrl().isNullOrBlank()

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
