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

# --- Gson ---
# Gson binds each JSON key to a Kotlin property reflectively, at runtime, by
# field name. R8 cannot see those lookups, so without an explicit keep rule it
# strips and renames the response DTOs and the AlAdhan payload silently fails to
# parse in a minified release build. The unit tests cannot catch this because
# they run against unminified classes.
# Retrofit's consumer rules keep the service interface, not the response types.
# Any new DTO added to the remote layer must be listed here.
-keep class dz.techvision.noor.data.remote.prayer.TimingsResponse { *; }
-keep class dz.techvision.noor.data.remote.prayer.TimingsData { *; }
-keep class dz.techvision.noor.data.remote.prayer.TimingsDate { *; }
-keep class dz.techvision.noor.data.remote.prayer.TimingsMeta { *; }
-keep class dz.techvision.noor.data.remote.prayer.HijriDate { *; }
-keep class dz.techvision.noor.data.remote.prayer.GregorianDate { *; }
-keep class dz.techvision.noor.data.remote.update.** { *; }

# The bundled adhkar parser uses private Gson DTOs for the asset shape. Gson
# binds their JSON keys to property names reflectively, so R8 must not remove or
# rename them in a minified release build. Without these rules the release app
# silently falls back to the empty corpus and shows the "coming soon" state.
-keep class dz.techvision.noor.data.local.athkar.AthkarCorpusParser$SourceCategory { *; }
-keep class dz.techvision.noor.data.local.athkar.AthkarCorpusParser$SourceDhikr { *; }

# --- Coil ---
-dontwarn coil.**
-dontwarn android.graphics.ImageDecoder

# --- Coroutines ---
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}