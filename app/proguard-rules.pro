# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Optimization rules
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Application specific rules
-keep class com.sameerasw.canvas.CanvasModel { *; }
-keep class com.sameerasw.canvas.model.** { *; }
-keep class com.sameerasw.canvas.data.** { *; }

# Compose rules
-keepclassmembers class androidx.compose.ui.platform.ComposeView {
    public void set*(...);
}

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Suppress XR missing classes warnings
-dontwarn com.android.extensions.xr.**
-dontwarn com.google.androidxr.**
-dontwarn com.google.imp.**