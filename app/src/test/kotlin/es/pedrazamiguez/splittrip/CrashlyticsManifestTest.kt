package es.pedrazamiguez.splittrip

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.w3c.dom.Element

class CrashlyticsManifestTest {

    private fun resolveAppFile(relativePath: String): File {
        val direct = File(relativePath)
        return if (direct.exists()) direct else File("app/$relativePath")
    }

    private fun parseMetaData(manifestFile: File): List<Pair<String, String>> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifestFile)
        val metaDataNodes = document.getElementsByTagName("meta-data")
        return buildList {
            for (i in 0 until metaDataNodes.length) {
                val element = metaDataNodes.item(i) as Element
                val name = element.getAttribute("android:name")
                val value = element.getAttribute("android:value")
                add(name to value)
            }
        }
    }

    @Test
    fun `debug manifest disables firebase_crashlytics_collection_enabled inside application`() {
        val debugManifest = resolveAppFile("src/debug/AndroidManifest.xml")
        assertTrue(debugManifest.exists(), "Expected debug AndroidManifest.xml to exist")

        val metaData = parseMetaData(debugManifest)
        assertTrue(
            metaData.contains("firebase_crashlytics_collection_enabled" to "false"),
            "Expected debug AndroidManifest.xml to set firebase_crashlytics_collection_enabled=false"
        )
    }

    @Test
    fun `main manifest does not disable firebase_crashlytics_collection_enabled`() {
        val mainManifest = resolveAppFile("src/main/AndroidManifest.xml")
        assertTrue(mainManifest.exists(), "Expected main AndroidManifest.xml to exist")

        val metaData = parseMetaData(mainManifest)
        assertFalse(
            metaData.any { (name, value) ->
                name == "firebase_crashlytics_collection_enabled" && value == "false"
            },
            "Expected main AndroidManifest.xml not to disable firebase_crashlytics_collection_enabled"
        )
    }

    @Test
    fun `release and internalRelease source sets do not override manifests to disable Crashlytics`() {
        val variantManifests = listOf(
            resolveAppFile("src/release/AndroidManifest.xml"),
            resolveAppFile("src/internalRelease/AndroidManifest.xml")
        )

        variantManifests.forEach { manifest ->
            if (manifest.exists()) {
                val metaData = parseMetaData(manifest)
                val disablesCrashlytics = metaData.any { (name, value) ->
                    name == "firebase_crashlytics_collection_enabled" && value == "false"
                }
                assertEquals(
                    false,
                    disablesCrashlytics,
                    "Expected ${manifest.path} not to disable firebase_crashlytics_collection_enabled"
                )
            }
        }
    }
}
