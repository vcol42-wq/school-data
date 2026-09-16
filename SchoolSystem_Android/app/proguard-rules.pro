# Proguard & R8 Rules for Release Build

-keep class com.school.system.data.** { *; }
-keep class com.school.system.data.model.** { *; }
-keep class com.school.system.data.dao.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Retrofit
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
