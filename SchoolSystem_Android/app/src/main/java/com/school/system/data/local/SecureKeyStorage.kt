package com.school.system.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * طبقة التخزين الآمن للأسرار ورموز المواد (Hardware-Backed Encrypted Storage)
 * تعتمد على MasterKey بتشفير AES-256 و EncryptedSharedPreferences لحماية الرموز في بيئة Zero-Trust.
 */
@Singleton
class SecureKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "SecureKeyStorage"

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize EncryptedSharedPreferences, resetting master key: ${e.message}")
            // في حال تلف مفاتيح العتاد أو إعادة تثبيت التطبيق، يتم تنظيف الملف وإعادة إنشائه بأمان
            context.deleteSharedPreferences(SECURE_PREFS_FILE)
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * حفظ الرمز السري الخاص بمادة وشعبة معينة مشفراً في عتاد الجهاز.
     */
    fun saveSubjectPin(subjectKey: String, pin: String) {
        val cleanKey = sanitizeKey(subjectKey)
        val cleanPin = pin.trim()
        sharedPreferences.edit()
            .putString(cleanKey, cleanPin)
            .apply()
        Log.d(tag, "Subject PIN securely saved for key: $cleanKey")
    }

    /**
     * استرجاع الرمز السري المشفر للمادة، أو null إذا لم يكن مخزناً مسبقاً.
     */
    fun getSubjectPin(subjectKey: String): String? {
        val cleanKey = sanitizeKey(subjectKey)
        val pin = sharedPreferences.getString(cleanKey, null)
        return if (pin.isNullOrBlank()) null else pin.trim()
    }

    /**
     * مسح الرمز السري للمادة (عند قيام الإدارة بتغيير الرمز أو عند استلام خطأ مصادقة أمنية).
     */
    fun clearSubjectPin(subjectKey: String) {
        val cleanKey = sanitizeKey(subjectKey)
        sharedPreferences.edit()
            .remove(cleanKey)
            .apply()
        Log.w(tag, "Subject PIN cleared for key: $cleanKey")
    }

    /**
     * بناء مفتاح عزل ذري وموحد لكل مادة وشعبة (Atomic Scope Key).
     */
    fun buildSubjectKey(grade: String, section: String, subject: String): String {
        val g = grade.trim().lowercase()
        val s = section.trim().lowercase()
        val sub = subject.trim().lowercase()
        return "pin_${g}_${s}_${sub}"
    }

    private fun sanitizeKey(key: String): String {
        return key.trim().replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
    }

    companion object {
        private const val SECURE_PREFS_FILE = "secure_subject_keys_vault"
    }
}
