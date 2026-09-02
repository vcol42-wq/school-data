package com.school.system.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model representing a join request stored in Supabase.
 * Fields follow the snake_case naming in the database and are mapped to camelCase
 * Kotlin properties via @SerializedName.
 */

data class JoinRequestModel(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("school_id")
    val schoolId: String,

    @SerializedName("role")
    val role: String, // "teacher" or "student"

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("class_name")
    val className: String? = null,

    @SerializedName("section_name")
    val sectionName: String? = null,

    @SerializedName("subject_specialty")
    val subjectSpecialty: String? = null,

    @SerializedName("status")
    val status: String = "pending",

    @SerializedName("created_at")
    val createdAt: String? = null
)
