package es.pedrazamiguez.splittrip.domain.model

import java.time.Instant
import kotlinx.collections.immutable.ImmutableList

/**
 * Value object representing the raw output of the OCR process.
 *
 * @param fullText The full recognised raw text block.
 * @param blocks The individual lines/blocks of recognised text.
 * @param recognisedAt The timestamp when the OCR process occurred.
 */
data class RawReceiptText(
    val fullText: String,
    val blocks: ImmutableList<TextBlock>,
    val recognisedAt: Instant
)

/**
 * A segment/block of text recognized by the OCR engine.
 *
 * @param text The raw text content.
 * @param confidence The confidence score if available from the OCR engine, null otherwise.
 */
data class TextBlock(
    val text: String,
    val confidence: Float?
)
