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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val schoolId = sessionManager.getSchoolId() ?: ""
                val request = chain.request().newBuilder()
                    .addHeader("apikey", SupabaseConstants.API_KEY)
                    .addHeader("Authorization", "Bearer ${SupabaseConstants.API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    // Multi-tenant isolation header
                    .addHeader("x-school-id", schoolId)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
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
