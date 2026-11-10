# Preserve generic signatures and runtime annotations used by Gson, Retrofit,
# Room, and Hilt without retaining entire implementation packages.
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# DTOs and Data Models for Room, Gson, and Retrofit serialization
-keep class com.school.system.data.model.** { *; }
-keep class com.school.system.data.*Dto { *; }
-keep class com.school.system.data.*Request { *; }
-keep class com.school.system.data.*Response { *; }
-keep class com.school.system.data.Supabase* { *; }
-keep class com.school.system.data.Student* { *; }

-keep,allowoptimization interface * {
    @retrofit2.http.* <methods>;
}

# Room loads generated database implementations by convention and accesses
# entity fields through generated adapters.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# WorkManager instantiates worker subclasses reflectively.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

-keep,allowobfuscation,allowoptimization interface * extends dagger.hilt.internal.GeneratedComponent
-keep,allowobfuscation,allowoptimization interface * extends dagger.hilt.internal.ComponentManager

# ==================== CameraX ====================
-keep class androidx.camera.** { *; }
-keep class androidx.camera.core.impl.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.camera.core.ExperimentalGetImage *;
}

# ==================== ML Kit Barcode Scanning ====================
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-dontwarn org.tensorflow.**

# ==================== Google Play Services ====================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ==================== ZXing ====================
-keep class com.google.zxing.** { *; }
-keep class com.google.zxing.client.android.** { *; }

# ==================== TensorFlow (used by ML Kit) ====================
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# ==================== App Packages ====================
-keep class com.school.system.** { *; }
-keep class com.example.theboss.** { *; }

# ==================== Compose Runtime ====================
-keepclassmembers class * {
    @org.jetbrains.compose.resources.DrawableRes *;
    @org.jetbrains.compose.resources.StringRes *;
}

# ==================== Gson ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==================== Room ====================
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <methods>;
}
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.RoomInjectionProcessor
