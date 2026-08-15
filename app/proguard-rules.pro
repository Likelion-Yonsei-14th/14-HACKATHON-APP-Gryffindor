# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools proguard-defaults.txt file.

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.gryffindor.smartshopping.**$$serializer { *; }
-keepclassmembers class com.gryffindor.smartshopping.** {
    *** Companion;
}
-keepclasseswithmembers class com.gryffindor.smartshopping.** {
    kotlinx.serialization.KSerializer serializer(...);
}
