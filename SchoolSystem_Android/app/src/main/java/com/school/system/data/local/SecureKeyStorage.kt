package com.school.system.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * طبقة التخزين الآمن للأسرار ورموز المواد (Hardware-Backed Encrypted Storage)
 * تعتمد على MasterKey بتشفير AES-256 و EncryptedSharedPreferences مع دعم التخزين الاحتياطي المضمون.
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
            try {
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
            } catch (fallbackEx: Exception) {
                Log.e(tag, "Fallback directly to standard prefs: ${fallbackEx.message}")
                context.getSharedPreferences(BACKUP_PREFS_FILE, Context.MODE_PRIVATE)
            }
        }
    }

    private val backupPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(BACKUP_PREFS_FILE, Context.MODE_PRIVATE)
    }

    /**
     * حفظ الرمز السري الخاص بمادة وشعبة معينة مشفراً في عتاد الجهاز مع حفظ فوري ومضمون.
     */
    fun saveSubjectPin(subjectKey: String, pin: String) {
        val cleanKey = sanitizeKey(subjectKey)
        val cleanPin = pin.trim()
        if (cleanPin.isEmpty()) return

        try {
            sharedPreferences.edit()
                .putString(cleanKey, cleanPin)
                .putString(KEY_TEACHER_GLOBAL_PIN, cleanPin)
                .commit()
        } catch (e: Exception) {
            Log.w(tag, "Encrypted write error: ${e.message}")
        }

        try {
            val obfuscated = Base64.encodeToString(cleanPin.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            backupPrefs.edit()
                .putString(cleanKey, obfuscated)
                .putString(KEY_TEACHER_GLOBAL_PIN, obfuscated)
                .commit()
        } catch (e: Exception) {
            Log.w(tag, "Backup write error: ${e.message}")
        }

        Log.d(tag, "Subject PIN securely saved for key: $cleanKey")
    }

    /**
     * استرجاع الرمز السري المشفر للمادة، أو null إذا لم يكن مخزناً مسبقاً.
     */
    fun getSubjectPin(subjectKey: String): String? {
        val cleanKey = sanitizeKey(subjectKey)

        // 1. Try hardware encrypted storage first
        val pin = try {
            sharedPreferences.getString(cleanKey, null)?.trim()
        } catch (e: Exception) {
            null
        }
        if (!pin.isNullOrBlank()) return pin

        // 2. Try backup secure storage
        val backupRaw = try {
            backupPrefs.getString(cleanKey, null)
        } catch (e: Exception) {
            null
        }
        if (!backupRaw.isNullOrBlank()) {
            try {
                val decoded = String(Base64.decode(backupRaw, Base64.NO_WRAP), Charsets.UTF_8).trim()
                if (decoded.isNotBlank()) return decoded
            } catch (_: Exception) {}
        }

        // 3. Fallback: Check global teacher pin
        val globalPin = try {
            sharedPreferences.getString(KEY_TEACHER_GLOBAL_PIN, null)?.trim()
        } catch (e: Exception) {
            null
        }
        if (!globalPin.isNullOrBlank()) return globalPin

        val backupGlobal = try {
            backupPrefs.getString(KEY_TEACHER_GLOBAL_PIN, null)
        } catch (e: Exception) {
            null
        }
        if (!backupGlobal.isNullOrBlank()) {
            try {
                val decoded = String(Base64.decode(backupGlobal, Base64.NO_WRAP), Charsets.UTF_8).trim()
                if (decoded.isNotBlank()) return decoded
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * مسح الرمز السري للمادة المحددة فقط دون التأثير على الرمز العام للمدرس.
     */
    fun clearSubjectPin(subjectKey: String) {
        val cleanKey = sanitizeKey(subjectKey)
        try {
            sharedPreferences.edit().remove(cleanKey).commit()
        } catch (_: Exception) {}
        try {
            backupPrefs.edit().remove(cleanKey).commit()
        } catch (_: Exception) {}
        Log.w(tag, "Subject PIN cleared for key: $cleanKey")
    }

    /**
     * بناء مفتاح عزل ذري وموحد لكل مادة وشعبة مع تطبيع الحروف العربية والهمزات.
     */
    fun buildSubjectKey(grade: String, section: String, subject: String): String {
        val g = normalizeKeyPart(grade)
        val s = normalizeKeyPart(section)
        val sub = normalizeKeyPart(subject)
        return "pin_${g}_${s}_${sub}"
    }

    private fun normalizeKeyPart(str: String): String {
        return str.trim()
            .replace("[أإآٱ]".toRegex(), "ا")
            .replace("ة".toRegex(), "ه")
            .replace("ى".toRegex(), "ي")
            .replace("^الصف\\s+".toRegex(), "")
            .replace("(^|\\s)ال".toRegex(), "$1")
            .replace("\\s+".toRegex(), "_")
            .lowercase()
    }

    private fun sanitizeKey(key: String): String {
        return key.trim().replace(Regex("[^a-zA-Z0-9_\\u0600-\\u06FF]"), "_")
    }

    /**
     * حفظ واسترجاع كود المشرف التربوي العام مشفراً في عتاد الجهاز.
     */
    fun saveSupervisorCode(code: String) {
        val clean = code.trim()
        try {
            sharedPreferences.edit().putString(KEY_SUPERVISOR_CODE, clean).commit()
        } catch (_: Exception) {}
        try {
            backupPrefs.edit().putString(KEY_SUPERVISOR_CODE, clean).commit()
        } catch (_: Exception) {}
        Log.d(tag, "Supervisor code securely saved")
    }

    fun getSupervisorCode(): String? {
        val code = try {
            sharedPreferences.getString(KEY_SUPERVISOR_CODE, null)?.trim()
        } catch (_: Exception) {
            null
        }
        if (!code.isNullOrBlank()) return code
        return try {
            backupPrefs.getString(KEY_SUPERVISOR_CODE, null)?.trim()
        } catch (_: Exception) {
            null
        }
    }

    fun clearSupervisorCode() {
        try {
            sharedPreferences.edit().remove(KEY_SUPERVISOR_CODE).commit()
        } catch (_: Exception) {}
        try {
            backupPrefs.edit().remove(KEY_SUPERVISOR_CODE).commit()
        } catch (_: Exception) {}
    }

    companion object {
        private const val SECURE_PREFS_FILE = "secure_subject_keys_vault"
        private const val BACKUP_PREFS_FILE = "secure_backup_pins_vault"
        private const val KEY_SUPERVISOR_CODE = "secure_supervisor_code"
        private const val KEY_TEACHER_GLOBAL_PIN = "secure_teacher_global_pin"
    }
}
