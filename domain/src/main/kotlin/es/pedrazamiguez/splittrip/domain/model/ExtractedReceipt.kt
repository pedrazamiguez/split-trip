package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Value object representing structured fields extracted from a raw receipt.
 *
 * @param amount The total transaction amount.
 * @param currency The ISO 4217 currency code.
 * @param date The transaction date.
 * @param time The transaction time.
 * @param title A brief description of what was purchased.
 * @param vendor The best-effort merchant/store name.
 * @param category The guessed category name.
 * @param paymentMethod The transaction payment method.
 * @param notes Any relevant notes or identifiers like booking code/locator.
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
    val notes: String? = null,
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
