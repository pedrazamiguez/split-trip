package es.pedrazamiguez.splittrip.data.firebase.repository

import com.google.gson.Gson
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
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
    private val defaultCatalog = ModelPackCatalog(packs = listOf(defaultConfig))

    @Test
    fun `parses multi-pack catalog JSON into ordered ModelPackCatalog with tiers and priorities`() {
        val json = """
            {
              "schema_version": 2,
              "packs": [
                {
                  "pack_id": "qwen3-0.6b-litert-v1",
                  "display_name": "Compact (Qwen 3 0.6B)",
                  "tier": "compact",
                  "priority": 10,
                  "version": "1.0.0",
                  "download_url": "https://models.splittrip.eu/compact.litertlm",
                  "sha256_checksum": "deadbeef",
                  "size_bytes": 343490560,
                  "min_ram_mb": 2048,
                  "min_free_storage_bytes": 1073741824,
                  "is_enabled": true
                },
                {
                  "pack_id": "gemma4-e2b-litert-v1",
                  "display_name": "High Quality (Gemma 4 E2B)",
                  "tier": "high_quality",
                  "priority": 30,
                  "version": "1.1.0",
                  "download_url": "https://models.splittrip.eu/high.litertlm",
                  "sha256_checksum": "cafebabe",
                  "size_bytes": 2588147712,
                  "min_ram_mb": 6144,
                  "min_free_storage_bytes": 5368709120,
                  "is_enabled": true
                }
              ]
            }
        """.trimIndent()

        val parsed = FirebaseModelPackConfigParser.parseCatalog(gson, json, defaultCatalog)

        assertEquals(2, parsed.packs.size)
        val compact = parsed.packs[0]
        assertEquals("qwen3-0.6b-litert-v1", compact.packId)
        assertEquals("Compact (Qwen 3 0.6B)", compact.displayName)
        assertEquals(ModelPackTier.COMPACT, compact.tier)
        assertEquals(10, compact.priority)
        assertEquals("1.0.0", compact.version)
        assertEquals("https://models.splittrip.eu/compact.litertlm", compact.downloadUrl)
        assertEquals("deadbeef", compact.sha256Checksum)
        assertEquals(343_490_560L, compact.sizeBytes)
        assertEquals(2048, compact.minRamMb)
        assertEquals(1_073_741_824L, compact.minFreeStorageBytes)
        assertTrue(compact.isEnabled)

        val high = parsed.packs[1]
        assertEquals("gemma4-e2b-litert-v1", high.packId)
        assertEquals(ModelPackTier.HIGH_QUALITY, high.tier)
        assertEquals(30, high.priority)
        assertEquals("1.1.0", high.version)
    }

    @Test
    fun `deduplicates packs with duplicate pack_id and skips entries with blank pack_id`() {
        val json = """
            {
              "packs": [
                { "pack_id": "   ", "version": "1.0.0" },
                { "pack_id": "pack-a", "version": "1.0.0", "is_enabled": true },
                { "pack_id": "pack-a", "version": "2.0.0", "is_enabled": false },
                { "pack_id": "pack-b", "version": "1.2.0", "is_enabled": true }
              ]
            }
        """.trimIndent()

        val parsed = FirebaseModelPackConfigParser.parseCatalog(gson, json, defaultCatalog)

        assertEquals(2, parsed.packs.size)
        assertEquals("pack-a", parsed.packs[0].packId)
        assertEquals("1.0.0", parsed.packs[0].version)
        assertEquals("pack-b", parsed.packs[1].packId)
    }

    @Test
    fun `returns default catalog when JSON is blank, malformed, or packs array is empty`() {
        assertEquals(
            defaultCatalog,
            FirebaseModelPackConfigParser.parseCatalog(gson, "   ", defaultCatalog)
        )
        assertEquals(
            defaultCatalog,
            FirebaseModelPackConfigParser.parseCatalog(gson, "{not valid json", defaultCatalog)
        )
        assertEquals(
            defaultCatalog,
            FirebaseModelPackConfigParser.parseCatalog(gson, """{"packs":[]}""", defaultCatalog)
        )
        assertEquals(
            defaultCatalog,
            FirebaseModelPackConfigParser.parseCatalog(gson, """{"packs":[{"pack_id":"  "}]}""", defaultCatalog)
        )
    }

    @Test
    fun `clamps negative sizeBytes, minRamMb, and minFreeStorageBytes to zero and defaults displayName and priority`() {
        val json = """
            {
              "packs": [
                {
                  "pack_id": "pack-clamp",
                  "tier": "balanced",
                  "size_bytes": -100,
                  "min_ram_mb": -512,
                  "min_free_storage_bytes": -1
                }
              ]
            }
        """.trimIndent()

        val parsed = FirebaseModelPackConfigParser.parseCatalog(gson, json, defaultCatalog)

        assertEquals(1, parsed.packs.size)
        val pack = parsed.packs.first()
        assertEquals("pack-clamp", pack.displayName)
        assertEquals(ModelPackTier.BALANCED, pack.tier)
        assertEquals(20, pack.priority)
        assertEquals(0L, pack.sizeBytes)
        assertEquals(0, pack.minRamMb)
        assertEquals(0L, pack.minFreeStorageBytes)
        assertFalse(pack.isEnabled)
    }
}
