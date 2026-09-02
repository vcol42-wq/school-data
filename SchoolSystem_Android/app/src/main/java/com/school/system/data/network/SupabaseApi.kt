package com.school.system.data.network

import com.google.gson.annotations.SerializedName
import com.school.system.data.models.JoinRequest
import com.school.system.data.models.School
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface SupabaseService {

    // 1. الاستعلام عن بيانات المدرسة عبر كود المدرسة
    @GET("schools")
    suspend fun getSchoolByCode(
        @Query("pairing_code") codeFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<School>>

    // 2. إرسال طلب انضمام جديد
    @POST("join_requests")
    @Headers("Prefer: return=representation")
    suspend fun submitJoinRequest(
        @Body request: JoinRequest
    ): Response<List<JoinRequest>>

    // 3. التحقق من حالة الطلب للمستخدم بناءً على معرف الجهاز والمدرسة
    @GET("join_requests")
    suspend fun checkRequestStatus(
        @Query("device_id") deviceFilter: String,
        @Query("school_id") schoolFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<JoinRequest>>

    // 4. جلب إعدادات التطبيق (مثل مفتاح Gemini)
    @GET("app_config")
    suspend fun getAppConfig(
        @Query("config_key") keyFilter: String,
        @Query("select") select: String = "config_value"
    ): Response<List<AppConfigDto>>
}

data class AppConfigDto(
    @SerializedName("config_value") val configValue: String
)
