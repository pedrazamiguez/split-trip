package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService

/**
 * Orchestrates the receipt processing pipeline: OCR (text recognition) followed by
 * structured field extraction (amount, currency, date, title).
 *
 * This use case provides a clean boundary for the presentation layer to request
 * receipt analysis without needing direct dependencies on both OCR and Extraction services.
 */
class ExtractReceiptFieldsUseCase(
    private val ocrService: ReceiptOcrService,
    private val extractionService: ReceiptExtractionService
) {
    /**
     * OCRs the provided [attachment] and extracts structured fields from the raw text.
     *
     * @param attachment The receipt attachment to process.
     * @return A [Result] enclosing the [ExtractedReceipt] on success.
     */
    suspend operator fun invoke(attachment: ReceiptAttachment): Result<ExtractedReceipt> {
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
