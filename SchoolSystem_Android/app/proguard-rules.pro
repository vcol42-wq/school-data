# Proguard & R8 Production Rules for Release Build

# 1. Project Models, DTOs & Data Layer
-keep class com.school.system.data.** { *; }
-keepclassmembers class com.school.system.data.** { *; }
-keep class com.school.system.data.model.** { *; }
-keepclassmembers class com.school.system.data.model.** { *; }
-keep class com.school.system.data.models.** { *; }
-keepclassmembers class com.school.system.data.models.** { *; }
-keep class com.school.system.data.network.** { *; }
-keepclassmembers class com.school.system.data.network.** { *; }
-keep class com.school.system.data.dao.** { *; }
-keepclassmembers class com.school.system.data.dao.** { *; }
-keep class com.school.system.data.repository.** { *; }
-keepclassmembers class com.school.system.data.repository.** { *; }

# 2. Gson Serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# 3. Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# 4. Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# 5. Dagger Hilt
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager
-keep,allowobfuscation,allowshrinking interface * extends dagger.hilt.internal.GeneratedComponent
-keep,allowobfuscation,allowshrinking interface * extends dagger.hilt.internal.ComponentManager
-dontwarn dagger.hilt.**

# 6. ML Kit & CameraX
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# 7. Jetpack WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# 8. Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 9. Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
