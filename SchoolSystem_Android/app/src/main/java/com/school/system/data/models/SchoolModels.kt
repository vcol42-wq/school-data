package com.school.system.data.models

import com.google.gson.annotations.SerializedName

// نموذج بيانات المدرسة المتوافق مع جداول Supabase السحابية
data class School(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("school_name") val legacySchoolName: String? = null,
    @SerializedName("pairing_code") val pairingCode: String? = null,
    @SerializedName("school_code") val legacySchoolCode: String? = null
) {
    val schoolName: String get() = name ?: legacySchoolName ?: "مدرسة سحابية"
    val schoolCode: String get() = pairingCode ?: legacySchoolCode ?: ""
}

// نموذج إرسال طلب الانضمام
data class JoinRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("role") val role: String, // "teacher" أو "student"
    @SerializedName("full_name") val fullName: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("class_name") val className: String? = null,
    @SerializedName("section_name") val sectionName: String? = null,
    @SerializedName("subject_specialty") val subjectSpecialty: String? = null,
    @SerializedName("status") val status: String = "pending"
)
