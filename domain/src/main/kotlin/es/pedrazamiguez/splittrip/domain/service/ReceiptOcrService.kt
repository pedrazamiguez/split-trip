package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment

/**
 * Service to process receipt images and PDFs and convert them to raw text.
 */
interface ReceiptOcrService {

    /**
     * OCRs the provided [attachment] and returns the extracted raw text blocks.
     *
     * @param attachment The receipt attachment to recognize.
     * @return A [Result] containing the [RawReceiptText] on success, or a failure exception.
     */
    suspend fun recogniseText(attachment: ReceiptAttachment): Result<RawReceiptText>
}
