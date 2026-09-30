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

# Sauvegarde : les entites Room sont serialisees telles quelles (format v3).
# Les garder entieres coute peu et ecarte tout risque de champ renomme ou
# retire entre l'ecriture et la relecture d'un fichier.
-keep @kotlinx.serialization.Serializable class com.kps.trackmyweight.** { *; }

# Tink (via androidx.security:security-crypto, photos chiffrees) lit les champs
# de ses messages protobuf par reflexion : renommes, le dechiffrement echoue.
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# Annotations de compilation referencees par Tink et Guava, absentes a
# l'execution : sans ces directives, R8 les signale comme classes manquantes
# et fait echouer le build.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**
