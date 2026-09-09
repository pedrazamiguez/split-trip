############################################################################
# 🌐 RETROFIT / OKHTTP / GSON
############################################################################

# Retrofit (API interfaces and annotations)
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Gson (JSON serialization)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep DTO / API Response classes (used by Retrofit + Gson for deserialization)
-keep class es.pedrazamiguez.splittrip.data.remote.dto.** { *; }
-keep class es.pedrazamiguez.splittrip.data.remote.api.** { *; }

############################################################################
# 🔥 FIREBASE / FIRESTORE
############################################################################

# Preserve annotations and signatures
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep Firebase SDK classes (Firestore, Auth, Messaging, etc.)
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase App Check (explicit rules; also covered by the wildcard above)
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.firebase.appcheck.**

############################################################################
# 🛡️ PLAY INTEGRITY / PLAY CORE
############################################################################
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.integrity.**
-keep class com.google.firebase.appcheck.playintegrity.** { *; }

# Keep Google Play Services (used by Firebase)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Firestore Document models (reflection-based mapping)
-keep class es.pedrazamiguez.splittrip.data.firebase.firestore.document.** {
    <fields>;
    <methods>;
    <init>();
}

############################################################################
# 🗄️ ROOM DATABASE / DAOS / ENTITIES / MIGRATIONS
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
# 📷 ML KIT (OCR & BARCODE SCANNING)
############################################################################
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

############################################################################
# 🔐 GOOGLE SIGN-IN / CREDENTIAL MANAGER
############################################################################

# AndroidX Credentials (Credential Manager)
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**

# Google Identity Services (GetSignInWithGoogleOption, GoogleIdTokenCredential)
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

############################################################################
# 🧱 DOMAIN & DATA LAYER (Models, Services, UseCases, Repositories)
############################################################################

# Keep domain models (used by serialization, mapping, or tests)
-keep class es.pedrazamiguez.splittrip.domain.model.** { *; }

# Keep domain services and validators
-keep class es.pedrazamiguez.splittrip.domain.service.** { *; }
-keepclassmembers class es.pedrazamiguez.splittrip.domain.service.** {
    <init>(...);
    *;
}

# Keep data service implementations (OCR engines, local cleaner, etc.)
-keep class es.pedrazamiguez.splittrip.data.service.** { *; }
-keepclassmembers class es.pedrazamiguez.splittrip.data.service.** {
    <init>(...);
    *;
}

# Keep use cases (for Koin reflection / constructor injection)
-keep class es.pedrazamiguez.splittrip.domain.usecase.** { *; }
-keepclassmembers class es.pedrazamiguez.splittrip.domain.usecase.** {
    <init>(...);
    *;
}

# Keep repositories and their methods
-keep class es.pedrazamiguez.splittrip.domain.repository.** { *; }
-keepclassmembers class es.pedrazamiguez.splittrip.domain.repository.** {
    <init>(...);
    *;
}

############################################################################
# 🧩 PRESENTATION LAYER (ViewModels, UI)
############################################################################

# Keep all ViewModels for Koin + Jetpack reflection
-keep class es.pedrazamiguez.splittrip.features.**.*ViewModel { *; }
-keepclassmembers class es.pedrazamiguez.splittrip.features.**.*ViewModel {
    <init>(...);
    *;
}

# Keep composables, navigation, and other UI reflection-based classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

############################################################################
# ⚙️ DEPENDENCY INJECTION / UTILITIES
############################################################################

# Koin (uses reflection for module discovery)
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Coil 3.x (image loader)
-keep class coil3.** { *; }
-dontwarn coil3.**

# Timber (logging)
-keep class timber.log.Timber { *; }
-dontwarn timber.log.Timber

############################################################################
# 💬 BUBBLE NOTIFICATIONS
############################################################################

# Protect notification and compatibility classes
-keep class androidx.core.app.** { *; }
-keep class androidx.core.graphics.drawable.IconCompat { *; }
-keep class androidx.core.content.pm.** { *; }

# Ensure MainActivity (bubble entry point) is accessible
-keep class es.pedrazamiguez.splittrip.MainActivity { *; }

############################################################################
# 🧠 DEBUGGING / REFLECTION SUPPORT
############################################################################

# Keep attribute information for better stack traces and reflection
-keepattributes SourceFile, LineNumberTable
-keepattributes Exceptions
