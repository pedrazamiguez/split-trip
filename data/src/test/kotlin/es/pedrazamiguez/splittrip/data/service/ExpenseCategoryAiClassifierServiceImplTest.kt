package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseCategoryAiClassifierServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var aiCoreCapabilityProvider: AICoreCapabilityProvider
    private lateinit var aiInferenceRepository: AiInferenceRepository
    private var lazyInitialized = false

    @BeforeEach
    fun setUp() {
        aiCoreCapabilityProvider = mockk()
        aiInferenceRepository = mockk()
        lazyInitialized = false
    }

    private fun createService(
        timeoutMs: Long = ExpenseCategoryAiClassifierServiceImpl.DEFAULT_INFERENCE_TIMEOUT_MS
    ): ExpenseCategoryAiClassifierServiceImpl {
        return ExpenseCategoryAiClassifierServiceImpl(
            aiCoreCapabilityProvider = aiCoreCapabilityProvider,
            aiCoreInferenceRepository = lazy {
                lazyInitialized = true
                aiInferenceRepository
            },
            defaultDispatcher = testDispatcher,
            inferenceTimeoutMs = timeoutMs
        )
    }

    @Test
    fun `returns null without touching AiInferenceRepository when AICore is unsupported`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns false
        val service = createService()

        val result = service.classify("Dinner", GroupContext.TRAVEL)

        assertNull(result)
        assertFalse(lazyInitialized)
        coVerify(exactly = 0) { aiInferenceRepository.generateContent(any()) }
    }

    @Test
    fun `returns null for blank title even when AICore is supported`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        val service = createService()

        val result = service.classify("  \n\"\"  ", GroupContext.TRAVEL)

        assertNull(result)
        assertFalse(lazyInitialized)
        coVerify(exactly = 0) { aiInferenceRepository.generateContent(any()) }
    }

    @Test
    fun `parses valid category and matching subcategory JSON from AICore with ON_DEVICE_AI source`() =
        runTest(testDispatcher) {
            every { aiCoreCapabilityProvider.isSupported() } returns true
            coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
                """{"category":"ACTIVITIES","subcategory":"MUSEUM_CULTURE"}"""
            )
            val service = createService()

            val result = service.classify("Louvre entry", GroupContext.TRAVEL)

            assertEquals(
                CategorySuggestion(
                    category = ExpenseCategory.ACTIVITIES,
                    subcategory = ExpenseSubcategory.MUSEUM_CULTURE,
                    source = CategorySuggestionSource.ON_DEVICE_AI
                ),
                result
            )
        }

    @Test
    fun `sanitizes markdown code fences and prose around JSON via JsonSanitizer`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
            """
            Sure! Here is the classification:
            ```json
            {"category":"HOME","subcategory":"UTILITIES"}
            ```
            """.trimIndent()
        )
        val service = createService()

        val result = service.classify("Endesa monthly charge", GroupContext.EVERYDAY)

        assertEquals(
            CategorySuggestion(
                category = ExpenseCategory.HOME,
                subcategory = ExpenseSubcategory.UTILITIES,
                source = CategorySuggestionSource.ON_DEVICE_AI
            ),
            result
        )
    }

    @Test
    fun `degrades mismatched subcategory to null while preserving valid category`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
            """{"category":"FOOD","subcategory":"HOTEL"}"""
        )
        val service = createService()

        val result = service.classify("Special meal", GroupContext.TRAVEL)

        assertEquals(
            CategorySuggestion(
                category = ExpenseCategory.FOOD,
                subcategory = null,
                source = CategorySuggestionSource.ON_DEVICE_AI
            ),
            result
        )
    }

    @Test
    fun `degrades UNSPECIFIED subcategory to null`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
            """{"category":"TRANSPORT","subcategory":"UNSPECIFIED"}"""
        )
        val service = createService()

        val result = service.classify("City pass", GroupContext.TRAVEL)

        assertEquals(
            CategorySuggestion(
                category = ExpenseCategory.TRANSPORT,
                subcategory = null,
                source = CategorySuggestionSource.ON_DEVICE_AI
            ),
            result
        )
    }

    @Test
    fun `returns null when AI returns uncertain OTHER with null subcategory`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
            """{"category":"OTHER","subcategory":null}"""
        )
        val service = createService()

        val result = service.classify("Ambiguous item", GroupContext.TRAVEL)

        assertNull(result)
    }

    @Test
    fun `keeps OTHER when AI returns a valid specific OTHER subcategory like PET_CARE`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
            """{"category":"OTHER","subcategory":"PET_CARE"}"""
        )
        val service = createService()

        val result = service.classify("Kibble bag", GroupContext.EVERYDAY)

        assertEquals(
            CategorySuggestion(
                category = ExpenseCategory.OTHER,
                subcategory = ExpenseSubcategory.PET_CARE,
                source = CategorySuggestionSource.ON_DEVICE_AI
            ),
            result
        )
    }

    @Test
    fun `returns null on malformed JSON or unknown category`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        val service = createService()

        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success("not json at all")
        assertNull(service.classify("Some title", GroupContext.TRAVEL))

        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success("{malformed:")
        assertNull(service.classify("Some title", GroupContext.TRAVEL))

        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.success(
            """{"category":"UNKNOWN_CATEGORY","subcategory":null}"""
        )
        assertNull(service.classify("Some title", GroupContext.TRAVEL))
    }

    @Test
    fun `returns null when AiInferenceRepository returns failure`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.failure(
            IllegalStateException("AICore unavailable")
        )
        val service = createService()

        val result = service.classify("Some title", GroupContext.TRAVEL)

        assertNull(result)
    }

    @Test
    fun `returns null when inference exceeds configured timeout`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } coAnswers {
            delay(5_000L)
            Result.success("""{"category":"FOOD","subcategory":"RESTAURANT"}""")
        }
        val service = createService(timeoutMs = 100L)

        val result = service.classify("Slow item", GroupContext.TRAVEL)

        assertNull(result)
    }

    @Test
    fun `rethrows CancellationException instead of swallowing it`() {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiInferenceRepository.generateContent(any()) } returns Result.failure(
            CancellationException("Cancelled")
        )
        val service = createService()

        assertThrows(CancellationException::class.java) {
            runTest(testDispatcher) {
                service.classify("Cancelled item", GroupContext.TRAVEL)
            }
        }
    }

    @Test
    fun `builds prompt constrained to TRAVEL categories and excludes EVERYDAY-only categories like HOME`() =
        runTest(testDispatcher) {
            every { aiCoreCapabilityProvider.isSupported() } returns true
            val promptSlot = slot<String>()
            coEvery { aiInferenceRepository.generateContent(capture(promptSlot)) } returns Result.success(
                """{"category":"LODGING","subcategory":"HOTEL"}"""
            )
            val service = createService()

            service.classify("Stay", GroupContext.TRAVEL)

            val prompt = promptSlot.captured
            assertTrue(prompt.contains("LODGING:"))
            assertTrue(prompt.contains("ACTIVITIES:"))
            assertFalse(prompt.contains("HOME:"))
            assertFalse(prompt.contains("HEALTH_PERSONAL_CARE:"))
        }

    @Test
    fun `builds prompt constrained to EVERYDAY categories and excludes TRAVEL-only categories like LODGING`() =
        runTest(testDispatcher) {
            every { aiCoreCapabilityProvider.isSupported() } returns true
            val promptSlot = slot<String>()
            coEvery { aiInferenceRepository.generateContent(capture(promptSlot)) } returns Result.success(
                """{"category":"HOME","subcategory":"RENT_MORTGAGE"}"""
            )
            val service = createService()

            service.classify("Monthly payment", GroupContext.EVERYDAY)

            val prompt = promptSlot.captured
            assertTrue(prompt.contains("HOME:"))
            assertTrue(prompt.contains("HEALTH_PERSONAL_CARE:"))
            assertFalse(prompt.contains("LODGING:"))
            assertFalse(prompt.contains("ACTIVITIES:"))
        }
}
