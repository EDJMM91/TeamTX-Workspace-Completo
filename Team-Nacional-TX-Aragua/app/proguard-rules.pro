# Team Nacional TX Venezuela - ProGuard Rules
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities and DAOs
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Accompanist permissions
-keep class com.google.accompanist.permissions.** { *; }

# Keep Coil image loading
-keep class coil.** { *; }

# Keep Moshi JSON parsing
-keep class com.squareup.moshi.** { *; }

# Keep Retrofit/OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Material3 components
-keep class androidx.compose.material3.** { *; }

# Keep BuildConfig
-keep class com.example.BuildConfig { *; }

# Keep Serialization for Room
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
}

# Prevent obfuscation of Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}