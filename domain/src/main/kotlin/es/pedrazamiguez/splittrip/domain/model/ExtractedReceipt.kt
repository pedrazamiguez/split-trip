package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Value object representing structured fields extracted from a raw receipt.
 *
 * @param amount The total transaction amount.
 * @param currency The ISO 4217 currency code.
 * @param date The transaction date.
 * @param title The best-effort merchant/store name.
 * @param source The strategy source used for extraction.
 * @param confidence The overall extraction confidence.
 */
data class ExtractedReceipt(
    val amount: BigDecimal?,
    val currency: String?,
    val date: LocalDate?,
    val time: java.time.LocalTime? = null,
    val title: String?,
    val vendor: String? = null,
    val category: String? = null,
    val paymentMethod: String? = null,
    val source: ExtractionSource,
    val confidence: ExtractionConfidence
)

/**
 * Indicates the mechanism used to extract the receipt data.
 */
enum class ExtractionSource {
    AI_CORE,
    NO_OP
}

/**
 * Represents the on-device AI capabilities of the device for receipt extraction.
 */
enum class ExtractionCapability {
    ON_DEVICE_AI,
    UNSUPPORTED
}

/**
 * Represents the confidence level of the extracted values.
 */
enum class ExtractionConfidence {
    HIGH,
    MEDIUM,
    LOW
}
