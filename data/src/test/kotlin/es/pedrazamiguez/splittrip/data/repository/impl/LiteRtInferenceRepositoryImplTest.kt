package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LiteRtInferenceRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = LiteRtInferenceRepositoryImpl(defaultDispatcher = testDispatcher)

    @Test
    fun `getEngineType returns LITE_RT_LM`() {
        assertEquals(AiEngineType.LITE_RT_LM, repository.getEngineType())
    }

    // ── generateStructuredOutput ──────────────────────────────────────────

    @Nested
    inner class GenerateStructuredOutput {

        @Test
        fun `parses prompt correctly and extracts data`() = runTest(testDispatcher) {
            val prompt = "Input: QUICK MART Drink 25.00 Snack 15.00 Water 10.00 TOTAL 50.00 USD 2025-03-10 13:45"
            val result = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("\"amount\": \"50.00\""))
            assertTrue(json.contains("\"currency\": \"USD\""))
            assertTrue(json.contains("\"date\": \"2025-03-10\""))
        }

        @Test
        fun `extracts EUR currency from prompt`() = runTest(testDispatcher) {
            val prompt = "Restaurant TOTAL 30.00 EUR 2026-01-20 19:30"
            val result = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("\"currency\": \"EUR\""))
        }

        @Test
        fun `extracts GBP currency from prompt`() = runTest(testDispatcher) {
            val prompt = "SUPERMARKET TOTAL 15.99 GBP 2026-02-10"
            val result = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("\"currency\": \"GBP\""))
        }

        @Test
        fun `uses fallback amount when no total keyword present`() = runTest(testDispatcher) {
            val prompt = "Generic store 99.99"
            val result = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("\"amount\""))
        }

        @Test
        fun `uses fallback vendor when no matching line found`() = runTest(testDispatcher) {
            val prompt = "TOTAL 20.00 EUR"
            val result = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("\"vendor\""))
        }

        @Test
        fun `returns well-formed JSON with all expected fields`() = runTest(testDispatcher) {
            val prompt = "CAFE PARIS TOTAL 15.50 EUR 2026-05-01 10:30"
            val result = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("\"amount\""))
            assertTrue(json.contains("\"currency\""))
            assertTrue(json.contains("\"date\""))
            assertTrue(json.contains("\"time\""))
            assertTrue(json.contains("\"vendor\""))
            assertTrue(json.contains("\"title\""))
            assertTrue(json.contains("\"category\""))
            assertTrue(json.contains("\"paymentMethod\""))
        }
    }

    // ── generateContent ───────────────────────────────────────────────────

    @Nested
    inner class GenerateContent {

        @Test
        fun `returns success with JSON content`() = runTest(testDispatcher) {
            val prompt = "QUICK MART TOTAL 50.00 USD 2025-03-10 13:45"
            val result = repository.generateContent(prompt)

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.isNotBlank())
            assertTrue(json.contains("\"amount\""))
        }

        @Test
        fun `returns same extraction logic as generateStructuredOutput`() = runTest(testDispatcher) {
            val prompt = "STORE TOTAL PAGAR 100.00 USD 2026-01-01 09:00"

            val contentResult = repository.generateContent(prompt)
            val structuredResult = repository.generateStructuredOutput(prompt, "schema")

            assertTrue(contentResult.isSuccess)
            assertTrue(structuredResult.isSuccess)
            // Both should succeed with same currency extraction
            assertTrue(contentResult.getOrThrow().contains("\"currency\": \"USD\""))
            assertTrue(structuredResult.getOrThrow().contains("\"currency\": \"USD\""))
        }
    }
}
