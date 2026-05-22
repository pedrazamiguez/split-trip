package es.pedrazamiguez.splittrip.data.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AICoreReceiptParserTest {

    // ── smartTruncate — no truncation needed ──────────────────────────────────

    @Test
    fun `smartTruncate returns text unchanged when below budget`() {
        val text = "A".repeat(2_999)
        assertEquals(text, AICoreReceiptParser.smartTruncate(text))
    }

    @Test
    fun `smartTruncate returns text unchanged when exactly at budget`() {
        val text = "A".repeat(3_000)
        assertEquals(text, AICoreReceiptParser.smartTruncate(text))
    }

    // ── smartTruncate — truncation path ───────────────────────────────────────

    @Test
    fun `smartTruncate result length never exceeds budget when input is long`() {
        // Simulates a 5-page flight receipt (~6 000 chars).
        val text = "X".repeat(6_000)
        val result = AICoreReceiptParser.smartTruncate(text)
        // head(600) + "\n…\n"(3) + tail(2400) = 3003 — the ellipsis separator is tiny overhead
        assertTrue(result.length <= 3_003, "Expected ≤3003 chars, got ${result.length}")
    }

    @Test
    fun `smartTruncate preserves merchant name at the head`() {
        val airlineName = "AIRLINE NAME BOOKING REF ".repeat(24) // 600 chars exactly
        val fareDetails = "FARE BREAKDOWN GRAND TOTAL ".repeat(200) // long tail region
        val text = airlineName + fareDetails

        val result = AICoreReceiptParser.smartTruncate(text)

        // The first 600 chars (airline name block) must be intact.
        assertTrue(
            result.startsWith(airlineName),
            "Head of result should start with the merchant/airline name block"
        )
    }

    @Test
    fun `smartTruncate preserves grand total at the tail`() {
        val header = "HEADER DATA ".repeat(300) // long, will be truncated
        val grandTotalLine = "GRAND TOTAL 1234.56 EUR 2026-05-22"
        // Pad so the grand total line is the very last content, within the last 2400 chars.
        val fareSection = "item ".repeat(400) + grandTotalLine

        val text = header + fareSection

        val result = AICoreReceiptParser.smartTruncate(text)

        assertTrue(
            result.endsWith(grandTotalLine),
            "Tail of result should contain the grand total line"
        )
    }

    @Test
    fun `smartTruncate inserts ellipsis separator between head and tail`() {
        val text = "A".repeat(6_000)
        val result = AICoreReceiptParser.smartTruncate(text)
        assertTrue(result.contains("\n…\n"), "Truncated result must contain the ellipsis separator")
    }
}
