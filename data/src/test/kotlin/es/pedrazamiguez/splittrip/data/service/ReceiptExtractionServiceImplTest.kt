package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptExtractionServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var aiCoreCapabilityProvider: AICoreCapabilityProvider
    private lateinit var aiCoreReceiptParser: AICoreReceiptParser
    private lateinit var service: ReceiptExtractionServiceImpl

    private val rawReceiptText = RawReceiptText(
        fullText = "Store Name\nTotal: 12.34 EUR\nDate: 2026-05-20",
        blocks = persistentListOf(),
        recognisedAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        aiCoreCapabilityProvider = mockk()
        aiCoreReceiptParser = mockk()
        service = ReceiptExtractionServiceImpl(
            aiCoreCapabilityProvider = aiCoreCapabilityProvider,
            aiCoreReceiptParser = lazy { aiCoreReceiptParser },
            defaultDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `capability returns UNSUPPORTED when AICore capability is not supported`() {
        every { aiCoreCapabilityProvider.isSupported() } returns false

        val capability = service.capability()

        assertEquals(ExtractionCapability.UNSUPPORTED, capability)
    }

    @Test
    fun `capability returns ON_DEVICE_AI when AICore capability is supported`() {
        every { aiCoreCapabilityProvider.isSupported() } returns true

        val capability = service.capability()

        assertEquals(ExtractionCapability.ON_DEVICE_AI, capability)
    }

    @Test
    fun `extract returns NO_OP fallback immediately when capability is unsupported`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns false

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(ExtractionSource.NO_OP, receipt.source)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
        assertNull(receipt.amount)
        assertNull(receipt.currency)
        assertNull(receipt.date)
        assertNull(receipt.title)
    }

    @Test
    fun `extract delegates to AICore parser and returns parsed receipt when capability is supported`() = runTest(
        testDispatcher
    ) {
        every { aiCoreCapabilityProvider.isSupported() } returns true

        val expectedReceipt = ExtractedReceipt(
            amount = BigDecimal("12.34"),
            currency = "EUR",
            date = LocalDate.of(2026, 5, 20),
            title = "Store Name",
            source = ExtractionSource.AI_CORE,
            confidence = ExtractionConfidence.HIGH
        )
        coEvery { aiCoreReceiptParser.parse(rawReceiptText) } returns Result.success(expectedReceipt)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("12.34"), receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertEquals(LocalDate.of(2026, 5, 20), receipt.date)
        assertEquals("Store Name", receipt.title)
        assertEquals(ExtractionSource.AI_CORE, receipt.source)
        assertEquals(ExtractionConfidence.HIGH, receipt.confidence)
    }

    @Test
    fun `extract returns NO_OP fallback when delegate AICore parser throws exception or returns failure`() = runTest(
        testDispatcher
    ) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiCoreReceiptParser.parse(rawReceiptText) } returns Result.failure(Exception("Model loading error"))

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(ExtractionSource.NO_OP, receipt.source)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
        assertNull(receipt.amount)
        assertNull(receipt.currency)
        assertNull(receipt.date)
        assertNull(receipt.title)
    }
}
