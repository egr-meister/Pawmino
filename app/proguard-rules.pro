# Pawmino ProGuard / R8 rules
# The app has no networking, reflection-heavy frameworks, or native SDKs.
# The only reflection-sensitive area is kotlinx.serialization.

# --- kotlinx.serialization ---
# Keep the generated serializers and companion serializer accessors.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers, allowshrinking, allowoptimization class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable model classes in the app's model package and their members.
-keep, includedescriptorclasses class com.pawmino.app.model.** { *; }
-keep, includedescriptorclasses class com.pawmino.app.data.** { *; }

# Keep Serializable annotation and the SerialName metadata.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# kotlinx.serialization runtime internals.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Kotlin metadata ---
-keep class kotlin.Metadata { *; }

# --- Compose ---
# Compose ships with its own consumer rules; nothing extra required here.
