package es.pedrazamiguez.splittrip.konsist

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Version Catalog Update Architecture Rules")
class VersionCatalogUpdateArchitectureTest {

    private val projectRootDir: File by lazy {
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: error("Could not find project root containing settings.gradle.kts")
    }

    private fun readProjectFile(relativePath: String): String {
        val file = File(projectRootDir, relativePath)
        assertTrue(file.exists(), "Expected $relativePath at ${file.absolutePath}")
        return file.readText()
    }

    private fun extractVersionCatalogUpdateBlock(buildScript: String): String {
        val startIndex = buildScript.indexOf("versionCatalogUpdate {")
        assertTrue(startIndex >= 0, "Root build.gradle.kts must define a versionCatalogUpdate block")
        var depth = 0
        for (index in startIndex until buildScript.length) {
            when (buildScript[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return buildScript.substring(startIndex, index + 1)
                    }
                }
            }
        }
        error("Unclosed versionCatalogUpdate block in root build.gradle.kts")
    }

    @Test
    @DisplayName("Root build.gradle.kts must configure monotonic stability versionSelector in versionCatalogUpdate")
    fun `root build file must configure monotonic stability versionSelector in versionCatalogUpdate`() {
        val buildScript = readProjectFile("build.gradle.kts")
        val block = extractVersionCatalogUpdateBlock(buildScript)

        assertTrue(
            buildScript.contains("private fun versionStabilityRank("),
            "Root build.gradle.kts must define versionStabilityRank helper"
        )
        assertTrue(
            block.contains("versionSelector {"),
            "versionCatalogUpdate must configure a custom versionSelector"
        )
        assertTrue(
            block.contains("candidateRank > 0 && candidateRank >= currentRank"),
            "versionSelector must enforce monotonic stability rank (candidateRank > 0 && candidateRank >= currentRank)"
        )
        listOf("\"rc\"", "\"beta\"", "\"alpha\"", "\"exp\"").forEach { qualifier ->
            assertTrue(
                buildScript.contains(qualifier),
                "versionStabilityRank must classify $qualifier pre-release qualifiers"
            )
        }
    }

    @Test
    @DisplayName("Root build.gradle.kts must pin composeBom, kotlin, and ksp in versionCatalogUpdate")
    fun `root build file must pin composeBom, kotlin, and ksp in versionCatalogUpdate`() {
        val buildScript = readProjectFile("build.gradle.kts")
        val block = extractVersionCatalogUpdateBlock(buildScript)

        listOf("composeBom", "kotlin", "ksp").forEach { pinnedVersion ->
            assertTrue(
                block.contains("versions.add(\"$pinnedVersion\")"),
                "versionCatalogUpdate pin block must include versions.add(\"$pinnedVersion\")"
            )
        }
    }

    @Test
    @DisplayName("update-version-catalog.yml must run checkDebugAarMetadata before committing or pushing")
    fun `update-version-catalog workflow must run checkDebugAarMetadata before committing or pushing`() {
        val workflow = readProjectFile(".github/workflows/update-version-catalog.yml")

        val updateIndex = workflow.indexOf("./gradlew versionCatalogUpdate")
        val metadataCheckIndex = workflow.indexOf("./gradlew checkDebugAarMetadata")
        val commitIndex = workflow.indexOf("git commit")
        val pushIndex = workflow.indexOf("git push")

        assertTrue(updateIndex >= 0, "Workflow must run ./gradlew versionCatalogUpdate")
        assertTrue(metadataCheckIndex >= 0, "Workflow must run ./gradlew checkDebugAarMetadata")
        assertTrue(commitIndex >= 0, "Workflow must commit updated catalog")
        assertTrue(pushIndex >= 0, "Workflow must push updated branch")
        assertTrue(
            metadataCheckIndex in (updateIndex + 1) until commitIndex,
            "checkDebugAarMetadata must run after versionCatalogUpdate and before git commit"
        )
        assertTrue(
            metadataCheckIndex < pushIndex,
            "checkDebugAarMetadata must run before git push"
        )
    }

    @Test
    @DisplayName("Version catalog must keep composeBom and material3 on non-alpha tracks while compileSdk is 37")
    fun `version catalog must keep composeBom and material3 on non-alpha tracks while compileSdk is 37`() {
        val toml = readProjectFile("gradle/libs.versions.toml")
        val compileSdk = Regex("""^compileSdk\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
            .find(toml)
            ?.groupValues
            ?.get(1)

        if (compileSdk == "37") {
            val composeBom = Regex("""^composeBom\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
                .find(toml)
                ?.groupValues
                ?.get(1)
            val material3 = Regex("""^material3\s*=\s*"([^"]+)"""", RegexOption.MULTILINE)
                .find(toml)
                ?.groupValues
                ?.get(1)

            assertTrue(
                composeBom == "2026.09.00",
                "composeBom must stay at 2026.09.00 while compileSdk is 37 (found: $composeBom)"
            )
            assertFalse(
                material3.orEmpty().contains("-alpha", ignoreCase = true) ||
                    material3.orEmpty().contains("-beta", ignoreCase = true),
                "material3 must not be on an alpha or beta track while compileSdk is 37 (found: $material3)"
            )
        }
    }
}
