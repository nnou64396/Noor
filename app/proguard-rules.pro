# Add project specific ProGuard rules here.
# For more details, see
#   https://developer.android.com/studio/build/shrink-code

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking,allowoptimization interface retrofit2.Call

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Coil ---
-dontwarn coil.**
-dontwarn android.graphics.ImageDecoder

# --- Coroutines ---
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}