package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText

/**
 * Service to extract structured receipt data (amount, currency, date, merchant title)
 * from OCR raw text using on-device intelligence.
 */
interface ReceiptExtractionService {

    /**
     * Extracts structured fields from the raw OCR text.
     *
     * @param rawText The raw text blocks from receipt OCR.
     * @param engineType Optional override of the AI engine to run extraction.
     * @return A [Result] enclosing the [ExtractedReceipt] on success.
     */
    suspend fun extract(
        rawText: RawReceiptText,
        engineType: AiEngineType? = null
    ): Result<ExtractedReceipt>

    /**
     * Queries the device to check the support level of the extraction service.
     */
    fun capability(): ExtractionCapability
}
