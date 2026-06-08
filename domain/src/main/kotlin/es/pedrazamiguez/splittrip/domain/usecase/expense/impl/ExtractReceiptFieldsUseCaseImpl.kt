package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase

class ExtractReceiptFieldsUseCaseImpl(
    private val ocrService: ReceiptOcrService,
    private val extractionService: ReceiptExtractionService
) : ExtractReceiptFieldsUseCase {

    /**
     * OCRs the provided [attachment] and extracts structured fields from the raw text.
     *
     * @param attachment The receipt attachment to process.
     * @return A [Result] enclosing the [ExtractedReceipt] on success.
     */
    override suspend operator fun invoke(attachment: ReceiptAttachment): Result<ExtractedReceipt> {
        return ocrService.recogniseText(attachment).fold(
            onSuccess = { rawText ->
                extractionService.extract(rawText)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
