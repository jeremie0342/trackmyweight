-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.kps.trackmyweight.**$$serializer { *; }
-keepclassmembers class com.kps.trackmyweight.** {
    *** Companion;
}
-keepclasseswithmembers class com.kps.trackmyweight.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *

# Health Connect
-keep class androidx.health.connect.** { *; }
