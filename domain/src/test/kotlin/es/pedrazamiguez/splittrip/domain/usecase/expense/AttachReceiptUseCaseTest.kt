package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttachReceiptUseCaseTest {

    private lateinit var receiptStorageService: ReceiptStorageService
    private lateinit var useCase: AttachReceiptUseCase

    @BeforeEach
    fun setUp() {
        receiptStorageService = mockk()
        useCase = AttachReceiptUseCase(receiptStorageService)
    }

    @Test
    fun `invoke calls copyAndCompress and returns success`() = runTest {
        val sourceUri = "content://picker/receipt.jpg"
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/copied.webp",
            mimeType = "image/webp",
            capturedAtMillis = 1000L
        )
        coEvery { receiptStorageService.copyAndCompress(sourceUri) } returns attachment

        val result = useCase(sourceUri)

        assertTrue(result.isSuccess)
        assertEquals(attachment, result.getOrNull())
        coVerify(exactly = 1) { receiptStorageService.copyAndCompress(sourceUri) }
    }

    @Test
    fun `invoke returns failure when storage service throws`() = runTest {
        val sourceUri = "content://picker/receipt.jpg"
        val exception = RuntimeException("Copy error")
        coEvery { receiptStorageService.copyAndCompress(sourceUri) } throws exception

        val result = useCase(sourceUri)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { receiptStorageService.copyAndCompress(sourceUri) }
    }
}
