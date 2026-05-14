# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keepattributes Signature, AnnotationDefault, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable

# Retrofit
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Kotlin Serialization
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
}

# Keep Data Models
-keep class com.gustavo.brilhante.cutestickers.network.model.** { *; }
-keep class com.gustavo.brilhante.cutestickers.model.** { *; }
