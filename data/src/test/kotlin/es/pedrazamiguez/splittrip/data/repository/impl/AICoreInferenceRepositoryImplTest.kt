package es.pedrazamiguez.splittrip.data.repository.impl

import com.google.ai.edge.aicore.GenerateContentResponse
import com.google.ai.edge.aicore.GenerativeModel
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AICoreInferenceRepositoryImplTest {

    private lateinit var generativeModel: GenerativeModel
    private lateinit var repository: AICoreInferenceRepositoryImpl

    @BeforeEach
    fun setUp() {
        generativeModel = mockk(relaxed = true)
        repository = AICoreInferenceRepositoryImpl(generativeModel)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `getEngineType returns AI_CORE_GEMMA_4`() {
        assertEquals(AiEngineType.AI_CORE_GEMMA_4, repository.getEngineType())
    }

    @Test
    fun `generateContent prepares engine and returns text`() = runTest {
        val mockResponse = mockk<GenerateContentResponse>()
        every { mockResponse.text } returns "Hello World"
        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = repository.generateContent("prompt")

        assertTrue(result.isSuccess)
        assertEquals("Hello World", result.getOrThrow())
        coVerify(exactly = 1) { generativeModel.prepareInferenceEngine() }
        coVerify(exactly = 1) { generativeModel.generateContent("prompt") }
    }

    @Test
    fun `generateContent returns failure when exception is thrown`() = runTest {
        val exception = RuntimeException("inference error")
        coEvery { generativeModel.generateContent(any<String>()) } throws exception

        val result = repository.generateContent("prompt")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
