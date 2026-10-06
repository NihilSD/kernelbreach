# Kernel Breach app ProGuard/R8 rules.
# kotlinx.serialization: keep @Serializable metadata for content DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.kernelbreach.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Room/Hilt generally need no extra rules with modern AGP defaults.
