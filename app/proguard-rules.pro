# Keep kotlinx.serialization generated serializers (needed because release builds use R8).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.musicmania.app.**$$serializer { *; }
-keepclassmembers class com.musicmania.app.** { *** Companion; }
-keepclasseswithmembers class com.musicmania.app.** { kotlinx.serialization.KSerializer serializer(...); }
