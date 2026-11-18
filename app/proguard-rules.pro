# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room persistence & SQLite
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ML Kit Barcode Scanning & CameraX
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName <fields>;
    @com.google.android.gms.common.annotation.KeepName <methods>;
}
-dontwarn com.google.mlkit.**
-dontwarn org.slf4j.**

# ZXing QR generation & decoding
-keep class com.google.zxing.** { *; }

