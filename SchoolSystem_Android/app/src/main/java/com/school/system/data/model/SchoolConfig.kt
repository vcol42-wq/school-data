package com.school.system.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "config")
data class SchoolConfig(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "",
    val managerName: String = "",
    val directorateName: String = "",
    val sectionName: String = "",
    
    // الهوية السحابية الملكية
    val userEmail: String = "",       // الإيميل الشخصي للمستخدم
    val schoolId: String = "",        // رمز المدرسة العالمي (الربط السحري)
    val isActivated: Boolean = false, // حالة التفعيل الكلي
    val isCloudLocked: Boolean = false, // قفل السجل (الختم الإداري)
    
    val cloudUrl: String = "",
    val cloudKey: String = "",
    val geminiApiKey: String = "",
    val cloudGeminiKey: String = "", // Key fetched from Supabase app_config
    val isVerified: Boolean = false,
    val pairingCode: String = "",
    val isCloudSyncEnabled: Boolean = true,
    
    val theme: String = "diyala",     // الثيم الفخم الافتراضي
    val font: String = "tajawal",
    val isAiActivated: Boolean = true,
    val syncSealToken: String? = null,
    val role: String = "teacher",
    
    // إعدادات الجرس الذكي
    val bellStartHour: Int = 8,
    val bellStartMinute: Int = 0,
    val bellLessonDuration: Int = 40,
    val bellBreakDuration: Int = 10,
    val bellRingtoneUri: String? = null,
    val bellRingTeacherOnly: Boolean = false
)
