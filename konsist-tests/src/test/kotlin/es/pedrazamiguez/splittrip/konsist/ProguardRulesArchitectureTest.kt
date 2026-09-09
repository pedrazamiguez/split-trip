package es.pedrazamiguez.splittrip.konsist

import java.io.File
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
    @DisplayName("ViewModels must have keep rules for Koin and AndroidX reflection")
    fun `viewmodels must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.features.**.*ViewModel"),
            "Proguard rules must keep feature ViewModels"
        )
    }

    @Test
    @DisplayName("ML Kit classes must have keep and dontwarn rules")
    fun `ml kit must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("com.google.mlkit.**"),
            "Proguard rules must keep ML Kit classes"
        )
        assertTrue(
            rules.contains("com.google.android.gms.vision.**"),
            "Proguard rules must keep Google Mobile Vision classes"
        )
    }

    @Test
    @DisplayName("Play Integrity and App Check must have keep rules")
    fun `play integrity and app check must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("com.google.android.play.core.integrity.**"),
            "Proguard rules must keep Play Core Integrity classes"
        )
        assertTrue(
            rules.contains("com.google.firebase.appcheck.playintegrity.**"),
            "Proguard rules must keep Firebase App Check Play Integrity provider"
        )
    }

    @Test
    @DisplayName("Domain and data services must have keep rules")
    fun `domain and data services must have keep rules`() {
        val rules = readProguardRules()
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.domain.service.**"),
            "Proguard rules must keep domain services"
        )
        assertTrue(
            rules.contains("es.pedrazamiguez.splittrip.data.service.**"),
            "Proguard rules must keep data services"
        )
    }
}
