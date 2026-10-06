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
# 🌐 RETROFIT / OKHTTP / GSON (Reflection Boundaries)
############################################################################

# Retrofit (API interfaces and annotations)
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson serialization support & warnings
-dontwarn sun.misc.**
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep DTO classes and Retrofit API interfaces
-keep class es.pedrazamiguez.splittrip.data.remote.dto.** { *; }
-keep class es.pedrazamiguez.splittrip.data.remote.api.** { *; }

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
