############################################################################
# ⚡ R8 OPTIMIZATION & REPACKAGING
############################################################################

# Repackage all obfuscated classes into the default root package to minimize
# repetitive package prefix strings in the DEX String ID pool.
-repackageclasses ''

# Preserve critical attribute information for crash deobfuscation and reflection
-keepattributes SourceFile, LineNumberTable
-keepattributes Exceptions
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

############################################################################
# 🌐 GSON (Reflection Boundaries)
############################################################################

# Gson serialization support & warnings
-dontwarn sun.misc.**
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

############################################################################
# 🔥 FIRESTORE DOCUMENT MODELS (Reflection Boundary)
############################################################################

# Firestore uses reflection to instantiate document classes and populate fields
-keep class es.pedrazamiguez.splittrip.data.firebase.firestore.document.** {
    <fields>;
    <methods>;
    <init>();
}

############################################################################
# 🗄️ ROOM DATABASE / DAOS / ENTITIES / MIGRATIONS (Reflection Boundary)
############################################################################

-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.migration.Migration
-keep class es.pedrazamiguez.splittrip.data.local.entity.** { *; }
-keep class es.pedrazamiguez.splittrip.data.local.dao.** { *; }
-keep class es.pedrazamiguez.splittrip.data.local.database.** { *; }
-keep class es.pedrazamiguez.splittrip.data.local.converter.** { *; }

############################################################################
# 🪵 LOGGING
############################################################################

-dontwarn timber.log.Timber

############################################################################
# 🤖 LITERT-LM (JNI Reflection Boundary — AAR ships no consumer rules)
############################################################################

# liblitertlm_jni.so resolves LiteRT-LM classes, constructors, config getters
# (SamplerConfig, ThinkingConfig, RepetitionPenaltyConfig, NoRepeatNgramConfig),
# exceptions (LiteRtLmJniException), InputData subtypes, and callback interfaces
# by name via JNI FindClass/GetMethodID.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class * implements com.google.ai.edge.litertlm.LiteRtLmJni$JniMessageCallback { *; }
-keep class * implements com.google.ai.edge.litertlm.LiteRtLmJni$JniInferenceCallback { *; }

# Dynamic feature SPI boundary (:data -> :dynamic_litertlm via ServiceLoader)
-keep interface es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClient { *; }
-keep interface es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClientFactory { *; }
-keep class * implements es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClientFactory {
    public <init>();
    *;
}
