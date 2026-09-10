package com.example.theboss.di

import android.content.Context
import androidx.room.Room
import com.example.theboss.data.local.AppDao
import com.example.theboss.data.local.AppDatabase
import com.example.theboss.data.remote.SupabaseApi
import com.example.theboss.data.remote.SupabaseConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "the_boss_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAppDao(db: AppDatabase): AppDao = db.dao()

    @Provides
    @Singleton
    fun provideSupabaseApi(@ApplicationContext context: Context): SupabaseApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(DynamicUrlInterceptor(context))
            .build()

        return Retrofit.Builder()
            .baseUrl("${SupabaseConstants.PLACEHOLDER_BASE_URL}rest/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}

class DynamicUrlInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val prefs = context.getSharedPreferences("the_boss_prefs", Context.MODE_PRIVATE)
        val defaultUrl = "https://pexehlvkpdhmpukjydwd.supabase.co"
        val defaultKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleGVobHZrcGRobXB1a2p5ZHdkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4Njk4NDUsImV4cCI6MjEwMjQ0NTg0NX0.YFDRTLJnB56uD-rGtknex_NhycexP57WHhhTRVas5EY"
        val url = prefs.getString("supabase_url", null)?.takeIf { it.isNotBlank() } ?: defaultUrl
        val apiKey = prefs.getString("supabase_key", null)?.takeIf { it.isNotBlank() } ?: defaultKey
        val schoolId = prefs.getString("school_id", null)
        val builder = request.newBuilder()

        if (apiKey.isNotBlank()) {
            builder.header("apikey", apiKey)
            builder.header("Authorization", "Bearer $apiKey")
        }
        if (!schoolId.isNullOrBlank()) {
            builder.header("x-school-id", schoolId)
        }

        if (!url.isNullOrBlank()) {
            val cleanedUrl = url.trim()
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
        return chain.proceed(builder.build())
    }
}
