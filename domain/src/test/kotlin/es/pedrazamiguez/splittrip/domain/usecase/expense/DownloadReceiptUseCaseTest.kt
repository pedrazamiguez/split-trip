package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.DownloadReceiptUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DownloadReceiptUseCaseTest {

    private lateinit var receiptStorageService: ReceiptStorageService
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var useCase: DownloadReceiptUseCase

    @BeforeEach
    fun setUp() {
        receiptStorageService = mockk()
        expenseRepository = mockk(relaxed = true)
        useCase = DownloadReceiptUseCaseImpl(receiptStorageService, expenseRepository)
    }

    @Test
    fun `invoke downloads receipt and updates repository local URI`() = runTest {
        // Given
        val expenseId = "expense-123"
        val remoteUrl = "https://example.com/receipt.pdf"
        val localUri = "file:///data/user/0/es.pedrazamiguez.splittrip/files/receipts/unique-pdf.pdf"
        val attachment = ReceiptAttachment(
            localUri = localUri,
            mimeType = "application/pdf",
            capturedAtMillis = 1000L,
            remoteUrl = remoteUrl
        )

        coEvery { receiptStorageService.downloadAndStore(remoteUrl) } returns attachment

        // When
        val result = useCase(expenseId, remoteUrl)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(attachment, result.getOrNull())
        coVerify(exactly = 1) { receiptStorageService.downloadAndStore(remoteUrl) }
        coVerify(exactly = 1) { expenseRepository.updateReceiptLocalUri(expenseId, localUri) }
    }

    @Test
    fun `invoke returns failure when storage service throws`() = runTest {
        // Given
        val expenseId = "expense-123"
        val remoteUrl = "https://example.com/receipt.pdf"
        val exception = RuntimeException("Download failed")

        coEvery { receiptStorageService.downloadAndStore(remoteUrl) } throws exception

        // When
        val result = useCase(expenseId, remoteUrl)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { receiptStorageService.downloadAndStore(remoteUrl) }
        coVerify(exactly = 0) { expenseRepository.updateReceiptLocalUri(any(), any()) }
    }
}
