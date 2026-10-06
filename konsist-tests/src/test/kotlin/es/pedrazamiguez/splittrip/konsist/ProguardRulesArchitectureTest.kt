package es.pedrazamiguez.splittrip.konsist

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Proguard Rules Architecture Rules")
class ProguardRulesArchitectureTest {

    private val projectRootDir: File by lazy {
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: error("Could not find project root containing settings.gradle.kts")
    }

    private val proguardRulesFile: File by lazy {
        File(projectRootDir, "app/proguard-rules.pro")
    }

    private fun readProguardRules(): String {
        assertTrue(proguardRulesFile.exists(), "app/proguard-rules.pro must exist")
        return proguardRulesFile.readText()
    }

    @Test
    @DisplayName("app/proguard-rules.pro must exist")
    fun `proguard rules file must exist`() {
        assertTrue(proguardRulesFile.exists(), "Expected app/proguard-rules.pro at ${proguardRulesFile.absolutePath}")
    }

    @Test
    @DisplayName("Class repackaging must be enabled to minimize DEX string pool")
    fun `class repackaging must be enabled`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("-repackageclasses"),
            "Proguard rules must enable -repackageclasses to optimize DEX string pool"
        )
    }

    @Test
    @DisplayName("Room entities, DAOs, database, and converters must have keep rules")
    fun `room layer must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("androidx.room.RoomDatabase"),
            "Proguard rules must keep androidx.room.RoomDatabase"
        )
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.local.entity.**"),
            "Proguard rules must keep local database entities"
        )
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.local.dao.**"),
            "Proguard rules must keep local database DAOs"
        )
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.local.database.**"),
            "Proguard rules must keep local database implementations and migrations"
        )
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.local.converter.**"),
            "Proguard rules must keep local database type converters"
        )
    }

    @Test
    @DisplayName("Firestore document models must have keep rules")
    fun `firestore documents must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.firebase.firestore.document.**"),
            "Proguard rules must keep Firestore document models"
        )
    }

    @Test
    @DisplayName("Remote DTO models and API interfaces must have keep rules")
    fun `remote dtos and apis must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.remote.dto.**"),
            "Proguard rules must keep remote DTO classes"
        )
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.remote.api.**"),
            "Proguard rules must keep remote API interfaces"
        )
    }

    @Test
    @DisplayName("Redundant blanket wildcard rules for libraries with consumer rules must not be present")
    fun `redundant library wildcard keep rules must not be present`() {
        val rules = readProguardRules()
        assertFalse(
            rules.contains("com.google.firebase.**"),
            "Blanket keep on com.google.firebase.** must be removed in favor of AAR consumer rules"
        )
        assertFalse(
            rules.contains("androidx.compose.**"),
            "Blanket keep on androidx.compose.** must be removed in favor of Compose consumer rules"
        )
        assertFalse(
            rules.contains("org.koin.**"),
            "Blanket keep on org.koin.** must be removed in favor of Koin consumer rules"
        )
        assertFalse(
            rules.contains("com.google.android.gms.**"),
            "Blanket keep on com.google.android.gms.** must be removed in favor of Play Services consumer rules"
        )
        assertFalse(
            rules.contains("coil3.**"),
            "Blanket keep on coil3.** must be removed in favor of Coil consumer rules"
        )
    }

    @Test
    @DisplayName("Pure Kotlin domain models, services, use cases, and viewmodels must not have blanket keep rules")
    fun `pure kotlin domain and presentation layers must not have blanket keep rules`() {
        val rules = readProguardRules()
        assertFalse(
            rules.contains("es.pedrazamiguez.splittrip.domain.model.**"),
            "Pure Kotlin domain models must not be kept with blanket rules"
        )
        assertFalse(
            rules.contains("es.pedrazamiguez.splittrip.domain.service.**"),
            "Pure Kotlin domain services must not be kept with blanket rules"
        )
        assertFalse(
            rules.contains("es.pedrazamiguez.splittrip.domain.usecase.**"),
            "Pure Kotlin use cases must not be kept with blanket rules"
        )
        assertFalse(
            rules.contains("es.pedrazamiguez.splittrip.features.**.*ViewModel"),
            "ViewModels constructed via Koin DSL must not be kept with blanket rules"
        )
    }
}
