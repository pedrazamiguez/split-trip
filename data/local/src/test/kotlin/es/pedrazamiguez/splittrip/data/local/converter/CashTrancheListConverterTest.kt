package es.pedrazamiguez.splittrip.data.local.converter

import es.pedrazamiguez.splittrip.domain.model.CashTranche
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Uses Robolectric because [CashTrancheListConverter] depends on [org.json.JSONArray] /
 * [org.json.JSONObject], which are Android runtime classes not available in plain JVM tests.
 */
@RunWith(RobolectricTestRunner::class)
class CashTrancheListConverterTest {

    private val converter = CashTrancheListConverter()

    private val sampleTranche = CashTranche(
        withdrawalId = "withdrawal-1",
        amountConsumed = 5000L
    )

    private val anotherTranche = CashTranche(
        withdrawalId = "withdrawal-2",
        amountConsumed = 12500L
    )

    // ── fromCashTrancheList (serialize) ──────────────────────────────────────

    @Test
    fun `returns null for null input`() {
        assertNull(converter.fromCashTrancheList(null))
    }

    @Test
    fun `returns null for empty list`() {
        assertNull(converter.fromCashTrancheList(emptyList()))
    }

    @Test
    fun `serializes a single tranche to JSON string`() {
        val result = converter.fromCashTrancheList(listOf(sampleTranche))

        assertNotNull(result)
        assertTrue(result!!.contains("withdrawal-1"))
        assertTrue(result.contains("5000"))
    }

    @Test
    fun `serializes multiple tranches`() {
        val result = converter.fromCashTrancheList(listOf(sampleTranche, anotherTranche))

        assertNotNull(result)
        assertTrue(result!!.contains("withdrawal-1"))
        assertTrue(result.contains("withdrawal-2"))
    }

    @Test
    fun `produces valid JSON array format`() {
        val result = converter.fromCashTrancheList(listOf(sampleTranche))

        assertNotNull(result)
        assertTrue(result!!.startsWith("["))
        assertTrue(result.endsWith("]"))
    }

    // ── toCashTrancheList (deserialize) ─────────────────────────────────────

    @Test
    fun `returns null for null deserialize input`() {
        assertNull(converter.toCashTrancheList(null))
    }

    @Test
    fun `returns null for blank string`() {
        assertNull(converter.toCashTrancheList(""))
    }

    @Test
    fun `deserializes single tranche correctly`() {
        val json = """[{"withdrawalId":"withdrawal-1","amountConsumed":5000}]"""

        val result = converter.toCashTrancheList(json)

        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals("withdrawal-1", result[0].withdrawalId)
        assertEquals(5000L, result[0].amountConsumed)
    }

    @Test
    fun `deserializes multiple tranches`() {
        val json = """[{"withdrawalId":"w-1","amountConsumed":100},{"withdrawalId":"w-2","amountConsumed":200}]"""

        val result = converter.toCashTrancheList(json)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertEquals("w-1", result[0].withdrawalId)
        assertEquals(100L, result[0].amountConsumed)
        assertEquals("w-2", result[1].withdrawalId)
        assertEquals(200L, result[1].amountConsumed)
    }

    // ── Roundtrip ────────────────────────────────────────────────────────────

    @Test
    fun `roundtrip preserves single tranche data`() {
        val original = listOf(sampleTranche)

        val json = converter.fromCashTrancheList(original)
        val restored = converter.toCashTrancheList(json)

        assertNotNull(restored)
        assertEquals(1, restored!!.size)
        assertEquals(sampleTranche.withdrawalId, restored[0].withdrawalId)
        assertEquals(sampleTranche.amountConsumed, restored[0].amountConsumed)
    }

    @Test
    fun `roundtrip preserves multiple tranches`() {
        val original = listOf(sampleTranche, anotherTranche)

        val json = converter.fromCashTrancheList(original)
        val restored = converter.toCashTrancheList(json)

        assertNotNull(restored)
        assertEquals(2, restored!!.size)
        assertEquals(sampleTranche.withdrawalId, restored[0].withdrawalId)
        assertEquals(anotherTranche.withdrawalId, restored[1].withdrawalId)
    }
}
