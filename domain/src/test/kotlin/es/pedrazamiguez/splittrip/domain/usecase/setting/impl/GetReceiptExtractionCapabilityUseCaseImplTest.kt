package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetReceiptExtractionCapabilityUseCaseImplTest {

    private lateinit var receiptExtractionService: ReceiptExtractionService
    private lateinit var useCase: GetReceiptExtractionCapabilityUseCaseImpl

    @BeforeEach
    fun setUp() {
        receiptExtractionService = mockk()
        useCase = GetReceiptExtractionCapabilityUseCaseImpl(
            receiptExtractionService = receiptExtractionService
        )
    }

    @Test
    fun `invoke returns ON_DEVICE_AI when receiptExtractionService reports ON_DEVICE_AI`() {
        every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI

        val result = useCase()

        assertEquals(ExtractionCapability.ON_DEVICE_AI, result)
        verify(exactly = 1) { receiptExtractionService.capability() }
    }

    @Test
    fun `invoke returns UNSUPPORTED when receiptExtractionService reports UNSUPPORTED`() {
        every { receiptExtractionService.capability() } returns ExtractionCapability.UNSUPPORTED

        val result = useCase()

        assertEquals(ExtractionCapability.UNSUPPORTED, result)
        verify(exactly = 1) { receiptExtractionService.capability() }
    }
}
