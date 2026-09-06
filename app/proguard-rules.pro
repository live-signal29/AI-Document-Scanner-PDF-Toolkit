# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ProGuard rules for AI PDF Scanner

# ML Kit Text Recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Mobile Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlinx.coroutines.**

# Retrofit / Moshi
-keepattributes Signature
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}
