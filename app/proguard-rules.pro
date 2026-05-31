# =============================================
# Chehar Temple - ProGuard Rules
# =============================================

# Keep data models (Gson needs them)
-keep class com.chehartemple.app.data.model.** { *; }

# Keep Retrofit interfaces
-keep interface com.chehartemple.app.data.api.TempleApi { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Google Sign-In
-keep class com.google.android.gms.** { *; }

# Prevent leaking class names in stack traces
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
