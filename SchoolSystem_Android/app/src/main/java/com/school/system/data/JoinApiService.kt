package com.school.system.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class JoinRequestDto(
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("role") val role: String, // "teacher" or "student"
    @SerializedName("full_name") val fullName: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("class_name") val className: String? = null,
    @SerializedName("section_name") val sectionName: String? = null,
    @SerializedName("subject_specialty") val subjectSpecialty: String? = null
)

data class RequestStatusResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String, // "pending", "approved", "rejected"
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("role") val role: String,
    @SerializedName("class_name") val className: String? = null,
    @SerializedName("section_name") val sectionName: String? = null
)

interface JoinApiService {
    @POST("rest/v1/join_requests")
    suspend fun submitJoinRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolIdHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "school_id,device_id",
        @Body request: JoinRequestDto
    ): Response<Void>

    @GET("rest/v1/join_requests")
    suspend fun checkRequestStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("x-school-id") schoolIdHeader: String,
        @Query("device_id") deviceId: String,
        @Query("select") select: String = "id,status,school_id,role,class_name,section_name"
    ): Response<List<RequestStatusResponse>>
}
