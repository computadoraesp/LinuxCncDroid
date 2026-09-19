# ProGuard & R8 Optimization Rules for LinuxCncDroid

# 1. Preserve Line Numbers for Crash Analytics
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Room Database Architecture Rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**

# 3. Domain Models and Data Classes
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }

# 4. JSON Serialization (org.json standard library / Moshi if enabled)
-dontwarn com.squareup.moshi.**

# 5. CameraX Lifecycle & Core
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }

# 6. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# 7. OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes *Annotation*

