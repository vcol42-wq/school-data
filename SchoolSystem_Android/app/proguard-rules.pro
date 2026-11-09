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
