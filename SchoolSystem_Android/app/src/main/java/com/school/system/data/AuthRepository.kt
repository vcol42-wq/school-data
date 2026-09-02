package com.school.system.data

import android.content.Context
import android.provider.Settings
import com.school.system.data.dao.ConfigDao
import com.school.system.data.model.SchoolConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configDao: ConfigDao
) {
    
    private fun getApiService(providedUrl: String? = null): JoinApiService {
        val currentUrl = providedUrl?.ifEmpty { null } ?: SyncRepository.DEFAULT_SUPABASE_URL
        var formattedUrl = currentUrl.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        val baseUrl = if (formattedUrl.endsWith("/")) formattedUrl else "$formattedUrl/"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JoinApiService::class.java)
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEV"
    }

    suspend fun isApprovedLocally(): Boolean {
        val config = configDao.getConfig().first()
        return config?.isVerified == true && config.isActivated
    }

    suspend fun sendJoinRequest(
        schoolId: String,
        role: String,
        fullName: String,
        className: String? = null,
        sectionName: String? = null,
        subject: String? = null,
        providedUrl: String? = null,
        providedKey: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = providedUrl?.ifEmpty { null } ?: SyncRepository.DEFAULT_SUPABASE_URL
            val apiKey = providedKey?.ifEmpty { null } ?: SyncRepository.DEFAULT_ANON_KEY
            val authHeader = "Bearer $apiKey"

            val dto = JoinRequestDto(
                schoolId = schoolId,
                role = role,
                fullName = fullName,
                deviceId = getDeviceId(),
                className = className,
                sectionName = sectionName,
                subjectSpecialty = subject
            )

            val service = getApiService(url)
            val response = service.submitJoinRequest(
                apiKey = apiKey,
                auth = authHeader,
                schoolIdHeader = schoolId,
                onConflict = "school_id,device_id",
                request = dto
            )

            if (response.isSuccessful) {
                val current = configDao.getConfig().first() ?: SchoolConfig()
                configDao.saveConfig(
                    current.copy(
                        schoolId = schoolId,
                        schoolName = "مدرسة سحابية",
                        managerName = fullName,
                        role = role,
                        isVerified = false,
                        isActivated = false,
                        cloudUrl = url,
                        cloudKey = apiKey,
                        pairingCode = ""
                    )
                )
                Result.success(true)
            } else {
                Result.failure(Exception("فشل إرسال الطلب: ${response.code()} ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyApprovalStatus(
        providedUrl: String? = null,
        providedKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = configDao.getConfig().first() ?: SchoolConfig()
            val schoolId = currentConfig.schoolId
            if (schoolId.isEmpty()) {
                return@withContext Result.success("not_found")
            }

            val url = providedUrl?.ifEmpty { null } ?: currentConfig.cloudUrl.ifEmpty { null } ?: SyncRepository.DEFAULT_SUPABASE_URL
            val apiKey = providedKey?.ifEmpty { null } ?: currentConfig.cloudKey.ifEmpty { null } ?: SyncRepository.DEFAULT_ANON_KEY
            val authHeader = "Bearer $apiKey"

            val service = getApiService(url)
            val response = service.checkRequestStatus(
                apiKey = apiKey,
                auth = authHeader,
                schoolIdHeader = schoolId,
                deviceId = "eq.${getDeviceId()}"
            )

            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val latest = response.body()!!.first()
                if (latest.status == "approved") {
                    val current = configDao.getConfig().first() ?: SchoolConfig()
                    configDao.saveConfig(
                        current.copy(
                            schoolId = latest.schoolId,
                            role = latest.role,
                            isVerified = true,
                            isActivated = true,
                            syncSealToken = latest.id,
                            directorateName = latest.className ?: "",
                            sectionName = latest.sectionName ?: ""
                        )
                    )
                } else if (latest.status == "rejected") {
                    val current = configDao.getConfig().first() ?: SchoolConfig()
                    configDao.saveConfig(
                        current.copy(
                            isVerified = false,
                            isActivated = false
                        )
                    )
                }
                Result.success(latest.status)
            } else {
                Result.success("pending")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
