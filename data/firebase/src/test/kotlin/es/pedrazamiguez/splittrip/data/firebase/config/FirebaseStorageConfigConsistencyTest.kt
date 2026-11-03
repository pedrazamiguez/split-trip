package es.pedrazamiguez.splittrip.data.firebase.config

import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseStorageConfigConsistencyTest {

    @Test
    fun `firebase json configures storage rules pointing to storage rules file`() {
        val rootDir = resolveProjectRoot()
        val firebaseJsonFile = File(rootDir, "firebase.json")
        val storageRulesFile = File(rootDir, "storage.rules")

        assertTrue(firebaseJsonFile.exists(), "firebase.json not found: ${firebaseJsonFile.absolutePath}")
        assertTrue(storageRulesFile.exists(), "storage.rules not found: ${storageRulesFile.absolutePath}")

        val jsonElement = JsonParser.parseString(firebaseJsonFile.readText(StandardCharsets.UTF_8))
        val rootObj = jsonElement.asJsonObject

        assertTrue(rootObj.has("storage"), "firebase.json must contain 'storage' configuration")
        val storageObj = rootObj.getAsJsonObject("storage")
        assertTrue(storageObj.has("rules"), "storage configuration must contain 'rules'")
        assertEquals("storage.rules", storageObj.get("rules").asString)
    }

    @Test
    fun `storage rules enforces rules_version 2 and public read only for public avatars`() {
        val rootDir = resolveProjectRoot()
        val storageRulesFile = File(rootDir, "storage.rules")
        assertTrue(storageRulesFile.exists(), "storage.rules not found: ${storageRulesFile.absolutePath}")

        val content = storageRulesFile.readText(StandardCharsets.UTF_8)

        assertTrue(
            content.contains("rules_version = '2';"),
            "storage.rules must declare rules_version = '2';"
        )
        assertTrue(
            content.contains("service firebase.storage"),
            "storage.rules must declare service firebase.storage"
        )
        assertTrue(
            content.contains("match /public/avatars/{imageId}"),
            "storage.rules must contain match /public/avatars/{imageId}"
        )
        assertTrue(
            content.contains("allow read: if true;"),
            "storage.rules must allow read if true for public avatars"
        )
        assertTrue(
            content.contains("allow write: if false;"),
            "storage.rules must explicitly deny write for public avatars"
        )
        assertTrue(
            content.contains("match /{allPaths=**}"),
            "storage.rules must contain fallback match /{allPaths=**}"
        )
        assertTrue(
            content.contains("allow read, write: if request.auth != null;"),
            "storage.rules must require authentication for all other paths"
        )
    }

    private fun resolveProjectRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".")
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("Could not find project root containing settings.gradle.kts")
        }
        return dir
    }
}
