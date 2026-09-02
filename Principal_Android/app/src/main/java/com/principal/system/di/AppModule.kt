package com.principal.system.di

import android.content.Context
import androidx.room.Room
import com.principal.system.data.local.PrincipalDao
import com.principal.system.data.local.PrincipalDatabase
import com.principal.system.data.local.SessionManager
import com.principal.system.data.remote.PrincipalSupabaseApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrincipalDatabase {
        return Room.databaseBuilder(
            context,
            PrincipalDatabase::class.java,
            "principal_system.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun providePrincipalDao(database: PrincipalDatabase): PrincipalDao {
        return database.dao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val apiKey = sessionManager.getApiKey()?.ifEmpty { null }
                ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY"
            val schoolId = sessionManager.getSchoolId()?.ifEmpty { null } ?: "SCH-VCOL-6072"
            val rawUrl = sessionManager.getSupabaseUrl()?.ifEmpty { null } ?: "https://pexehlvkpdhmpukjydwd.supabase.co"

            val requestBuilder = originalRequest.newBuilder()
            requestBuilder.header("apikey", apiKey)
            requestBuilder.header("Authorization", "Bearer $apiKey")
            requestBuilder.header("x-school-id", schoolId)

            val cleanedUrl = rawUrl.trim()
            val formattedUrl = if (cleanedUrl.endsWith("/")) cleanedUrl else "$cleanedUrl/"
            val restUrl = if (formattedUrl.endsWith("rest/v1/")) formattedUrl else "${formattedUrl}rest/v1/"

            val newBaseUrl = restUrl.toHttpUrlOrNull()
            if (newBaseUrl != null) {
                val originalUrl = originalRequest.url
                val pathSegment = originalUrl.encodedPath.removePrefix("/rest/v1/").removePrefix("/")
                val newUrl = originalUrl.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)
                    .encodedPath(newBaseUrl.encodedPath + pathSegment)
                    .build()
                requestBuilder.url(newUrl)
            }

            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pexehlvkpdhmpukjydwd.supabase.co/rest/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providePrincipalApi(retrofit: Retrofit): PrincipalSupabaseApi {
        return retrofit.create(PrincipalSupabaseApi::class.java)
    }
}
