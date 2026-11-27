package es.pedrazamiguez.splittrip.konsist

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Dynamic Feature Architecture Rules")
class DynamicFeatureArchitectureTest {

    private val projectRootDir: File by lazy {
        generateSequence(File(".").canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: error("Could not find project root containing settings.gradle.kts")
    }

    @Test
    @DisplayName("Base :app and :data modules must not depend on litertlm-android")
    fun `base app and data modules must not depend on litertlm-android`() {
        val appBuild = File(projectRootDir, "app/build.gradle.kts").readText()
        val dataBuild = File(projectRootDir, "data/build.gradle.kts").readText()

        assertFalse(
            appBuild.contains("google.ai.edge.litertlm") || appBuild.contains("litertlm-android"),
            ":app must not depend on litertlm-android directly"
        )
        assertFalse(
            dataBuild.contains("google.ai.edge.litertlm") || dataBuild.contains("litertlm-android"),
            ":data must not depend on litertlm-android directly"
        )
    }

    @Test
    @DisplayName("App module must register :dynamic_litertlm in dynamicFeatures and settings.gradle.kts")
    fun `app module must register dynamic_litertlm in dynamicFeatures`() {
        val appBuild = File(projectRootDir, "app/build.gradle.kts").readText()
        val settings = File(projectRootDir, "settings.gradle.kts").readText()

        assertTrue(
            appBuild.contains("dynamicFeatures") && appBuild.contains(":dynamic_litertlm"),
            "app/build.gradle.kts must declare :dynamic_litertlm in dynamicFeatures"
        )
        assertTrue(
            settings.contains(":dynamic_litertlm"),
            "settings.gradle.kts must include :dynamic_litertlm"
        )
    }

    @Test
    @DisplayName(":dynamic_litertlm module must configure on-demand delivery without fusing")
    fun `dynamic_litertlm module must configure on-demand delivery without fusing`() {
        val manifest = File(projectRootDir, "dynamic_litertlm/src/main/AndroidManifest.xml").readText()

        assertTrue(
            manifest.contains("<dist:on-demand"),
            "dynamic_litertlm manifest must configure <dist:on-demand /> delivery"
        )
        assertTrue(
            manifest.contains("dist:fusing dist:include=\"false\""),
            "dynamic_litertlm manifest must disable fusing"
        )
        assertTrue(
            manifest.contains("dist:title=\"@string/dynamic_litertlm_module_title\""),
            "dynamic_litertlm manifest must reference @string/dynamic_litertlm_module_title"
        )
    }

    @Test
    @DisplayName(":dynamic_litertlm build script must depend on litertlm and must not enable minification")
    fun `dynamic_litertlm build script must depend on litertlm and must not enable minification`() {
        val dynamicBuild = File(projectRootDir, "dynamic_litertlm/build.gradle.kts").readText()

        assertTrue(
            dynamicBuild.contains("libs.google.ai.edge.litertlm"),
            "dynamic_litertlm/build.gradle.kts must depend on libs.google.ai.edge.litertlm"
        )
        assertFalse(
            dynamicBuild.contains("isMinifyEnabled = true"),
            "dynamic_litertlm/build.gradle.kts must not set isMinifyEnabled = true"
        )
    }

    @Test
    @DisplayName(":dynamic_litertlm SPI service descriptor must point to LiteRtLmEngineClientFactoryImpl")
    fun `dynamic_litertlm SPI service descriptor must point to LiteRtLmEngineClientFactoryImpl`() {
        val descriptorFile = File(
            projectRootDir,
            "dynamic_litertlm/src/main/resources/META-INF/services/" +
                "es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClientFactory"
        )

        assertTrue(descriptorFile.exists(), "SPI service descriptor must exist at ${descriptorFile.path}")
        assertTrue(
            descriptorFile.readText().trim() ==
                "es.pedrazamiguez.splittrip.dynamic.litertlm.LiteRtLmEngineClientFactoryImpl",
            "SPI service descriptor must reference LiteRtLmEngineClientFactoryImpl"
        )
    }
}
