package com.school.system.di

import android.content.Context
import com.school.system.data.local.SessionManager
import com.school.system.data.network.SupabaseConstants
import com.school.system.data.network.SupabaseService
import com.school.system.data.repository.SchoolRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val bossPrefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
                val diyalaPrefs = context.getSharedPreferences("diyala_school_prefs", Context.MODE_PRIVATE)
                val teacherPrefs = context.getSharedPreferences("school_system_prefs", Context.MODE_PRIVATE)

                val schoolId = sessionManager.getSchoolId()?.takeIf { it.isNotBlank() }
                    ?: bossPrefs.getString("school_id", null)?.takeIf { it.isNotBlank() }
                    ?: diyalaPrefs.getString("diyala_school_id", null)?.takeIf { it.isNotBlank() }
                    ?: diyalaPrefs.getString("school_id", null)?.takeIf { it.isNotBlank() }
                    ?: teacherPrefs.getString("school_id", null)?.takeIf { it.isNotBlank() }
                    ?: ""

                val dynamicUrl = bossPrefs.getString("supabase_url", null)?.takeIf { it.isNotBlank() }
                    ?: teacherPrefs.getString("supabase_url", null)?.takeIf { it.isNotBlank() }
                    ?: diyalaPrefs.getString("diyala_supabase_url", null)?.takeIf { it.isNotBlank() }
                val dynamicKey = bossPrefs.getString("supabase_key", null)?.takeIf { it.isNotBlank() }
                    ?: teacherPrefs.getString("supabase_key", null)?.takeIf { it.isNotBlank() }
                    ?: diyalaPrefs.getString("diyala_supabase_key", null)?.takeIf { it.isNotBlank() }
                    ?: SupabaseConstants.API_KEY

                val builder = request.newBuilder()
                    .header("apikey", dynamicKey)
                    .header("Authorization", "Bearer $dynamicKey")
                    .header("Content-Type", "application/json")
                    // Multi-tenant isolation header
                    .header("x-school-id", schoolId)

                if (!dynamicUrl.isNullOrBlank()) {
                    val cleanedUrl = dynamicUrl.trim()
                    val formattedUrl = if (cleanedUrl.endsWith("/")) cleanedUrl else "$cleanedUrl/"
                    val restUrl = if (formattedUrl.endsWith("rest/v1/")) formattedUrl else "${formattedUrl}rest/v1/"
                    val newBaseUrl = restUrl.toHttpUrlOrNull()
                    if (newBaseUrl != null) {
                        val originalUrl = request.url
                        val newUrl = originalUrl.newBuilder()
                            .scheme(newBaseUrl.scheme)
                            .host(newBaseUrl.host)
                            .port(newBaseUrl.port)
                            .encodedPath(
                                newBaseUrl.encodedPath +
                                    originalUrl.encodedPath.removePrefix("/rest/v1/").removePrefix("/")
                            )
                            .build()
                        builder.url(newUrl)
                    }
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideSupabaseService(okHttpClient: OkHttpClient): SupabaseService {
        return Retrofit.Builder()
            .baseUrl(SupabaseConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseService::class.java)
    }

    @Provides
    @Singleton
    fun provideSchoolRepository(
        supabaseService: SupabaseService,
        sessionManager: SessionManager
    ): SchoolRepository {
        return SchoolRepository(supabaseService, sessionManager)
    }
}
