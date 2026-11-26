package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.gson.Gson
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseModelPackConfigParserTest {

    private val gson = Gson()
    private val defaultConfig = ModelPackConfig(
        packId = "default-pack",
        version = "1.0.0",
        downloadUrl = "",
        sha256Checksum = "",
        sizeBytes = 0L,
        minRamMb = 4096,
        minFreeStorageBytes = 1_073_741_824L,
        isEnabled = false
    )

    @Test
    fun `parses valid remote config JSON into ModelPackConfig`() {
        val json = """
            {
              "pack_id": "gemma-litert-receipt-v1",
              "version": "2.1.0",
              "download_url": "https://cdn.example.com/model.litertlm",
              "sha256_checksum": "deadbeef",
              "size_bytes": 524288000,
              "min_ram_mb": 6144,
              "min_free_storage_bytes": 2147483648,
              "is_enabled": true
            }
        """.trimIndent()

        val parsed = FirebaseModelPackConfigParser.parse(gson, json, defaultConfig)

        assertEquals("gemma-litert-receipt-v1", parsed.packId)
        assertEquals("2.1.0", parsed.version)
        assertEquals("https://cdn.example.com/model.litertlm", parsed.downloadUrl)
        assertEquals("deadbeef", parsed.sha256Checksum)
        assertEquals(524_288_000L, parsed.sizeBytes)
        assertEquals(6144, parsed.minRamMb)
        assertEquals(2_147_483_648L, parsed.minFreeStorageBytes)
        assertTrue(parsed.isEnabled)
    }

    @Test
    fun `returns default config when JSON is blank or malformed`() {
        assertEquals(defaultConfig, FirebaseModelPackConfigParser.parse(gson, "   ", defaultConfig))
        assertEquals(defaultConfig, FirebaseModelPackConfigParser.parse(gson, "{not valid json", defaultConfig))
    }

    @Test
    fun `clamps negative sizeBytes and minRamMb to zero`() {
        val json = """
            {
              "size_bytes": -100,
              "min_ram_mb": -512,
              "min_free_storage_bytes": -1,
              "is_enabled": false
            }
        """.trimIndent()

        val parsed = FirebaseModelPackConfigParser.parse(gson, json, defaultConfig)

        assertEquals(0L, parsed.sizeBytes)
        assertEquals(0, parsed.minRamMb)
        assertEquals(0L, parsed.minFreeStorageBytes)
        assertFalse(parsed.isEnabled)
    }
}
