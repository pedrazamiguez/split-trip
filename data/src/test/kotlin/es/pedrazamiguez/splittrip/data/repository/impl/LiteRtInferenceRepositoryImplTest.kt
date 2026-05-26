package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiteRtInferenceRepositoryImplTest {

    private val repository = LiteRtInferenceRepositoryImpl()

    @Test
    fun `getEngineType returns LITE_RT_LM`() {
        assertEquals(AiEngineType.LITE_RT_LM, repository.getEngineType())
    }

    @Test
    fun `generateStructuredOutput parses prompt correctly and extracts data`() = runTest {
        val prompt = "Input: QUICK MART Drink 25.00 Snack 15.00 Water 10.00 TOTAL 50.00 USD 2025-03-10 13:45"
        val result = repository.generateStructuredOutput(prompt, "schema")

        assertTrue(result.isSuccess)
        val json = result.getOrThrow()
        assertTrue(json.contains("\"amount\": \"50.00\""))
        assertTrue(json.contains("\"currency\": \"USD\""))
        assertTrue(json.contains("\"date\": \"2025-03-10\""))
    }
}
