# Add project specific ProGuard rules here.

# OpenCV
-dontwarn org.opencv.**
-keep class org.opencv.** { *; }

# Project classes
-keep class com.paperscanner.app.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# AndroidX Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ViewBinding
-keep class com.paperscanner.app.databinding.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
