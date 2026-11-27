package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

internal object ReceiptExtractionJsonParser {

    private const val FIELD_COUNT_ALL = 4
    private const val FIELD_COUNT_THREE = 3
    private const val FIELD_COUNT_TWO = 2
    private val NON_NUMERIC_AMOUNT_REGEX = Regex("[^0-9.,]")

    private val REQUIRED_KEYS = listOf(
        "amounts",
        "currency",
        "date",
        "time",
        "vendor",
        "title",
        "category",
        "subcategory",
        "paymentMethod",
        "notes"
    )

    fun buildJsonSchema(): String {
        val nullableStringType = JSONArray(listOf("string", "null"))
        val properties = JSONObject().apply {
            put(
                "amounts",
                JSONObject().apply {
                    put("type", "array")
                    put("items", JSONObject().put("type", "string"))
                }
            )
            put("currency", JSONObject().put("type", "string"))
            put("date", JSONObject().put("type", nullableStringType))
            put("time", JSONObject().put("type", nullableStringType))
            put("vendor", JSONObject().put("type", nullableStringType))
            put("title", JSONObject().put("type", nullableStringType))
            put(
                "category",
                JSONObject().apply {
                    put("type", "string")
                    put("enum", JSONArray(ExpenseCategory.entries.map { it.name }))
                }
            )
            put("subcategory", JSONObject().put("type", nullableStringType))
            put(
                "paymentMethod",
                JSONObject().apply {
                    put("type", nullableStringType)
                    put("enum", JSONArray(PaymentMethod.entries.map { it.name } + JSONObject.NULL))
                }
            )
            put("notes", JSONObject().put("type", nullableStringType))
        }

        return JSONObject().apply {
            put("type", "object")
            put("properties", properties)
            put("required", JSONArray(REQUIRED_KEYS))
            put("additionalProperties", false)
        }.toString()
    }

    fun parse(cleanJson: String, activeEngine: AiEngineType): ExtractedReceipt {
        val jsonObject = JSONObject(cleanJson)

        val amount = parseAmount(jsonObject)
        val (currency, rawCurrency) = parseCurrency(jsonObject)
        val date = parseDate(jsonObject)
        val time = parseTime(jsonObject)

        val title = optNullableString(jsonObject, "title")
        val vendor = optNullableString(jsonObject, "vendor")

        val parsedCategory = parseCategoryAndSubcategory(jsonObject)
        val paymentMethod = parsePaymentMethod(jsonObject)
        val notes = optNullableString(jsonObject, "notes")

        val confidence = calculateConfidence(amount, rawCurrency, date, title, vendor)

        val source = when (activeEngine) {
            AiEngineType.AI_CORE_GEMMA_4 -> ExtractionSource.AI_CORE
            AiEngineType.LITE_RT_LM -> ExtractionSource.LITE_RT_LM
        }

        return ExtractedReceipt(
            amount = amount,
            currency = currency,
            date = date,
            time = time,
            title = title,
            vendor = vendor,
            category = parsedCategory.category,
            subcategory = parsedCategory.subcategory,
            paymentMethod = paymentMethod,
            notes = notes,
            source = source,
            confidence = confidence
        )
    }

    fun noOpFallbackReceipt(): ExtractedReceipt {
        return ExtractedReceipt(
            amount = null,
            currency = null,
            date = null,
            time = null,
            title = null,
            vendor = null,
            category = null,
            subcategory = null,
            paymentMethod = null,
            notes = null,
            source = ExtractionSource.NO_OP,
            confidence = ExtractionConfidence.LOW
        )
    }

    private fun optNullableString(jsonObject: JSONObject, key: String): String? {
        if (jsonObject.isNull(key)) return null
        val trimmed = jsonObject.optString(key).trim()
        return trimmed.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun parseAmount(jsonObject: JSONObject): BigDecimal? {
        val amountsArray = jsonObject.optJSONArray("amounts")
        if (amountsArray != null && amountsArray.length() > 0) {
            var sum = BigDecimal.ZERO
            for (i in 0 until amountsArray.length()) {
                if (amountsArray.isNull(i)) continue
                val parsed = parseNormalizedAmount(amountsArray.optString(i))
                if (parsed != null && parsed > BigDecimal.ZERO) {
                    sum += parsed
                }
            }
            if (sum > BigDecimal.ZERO) return sum
        }

        // Fallback for generic/legacy AI output
        return optNullableString(jsonObject, "amount")
            ?.let(::parseNormalizedAmount)
            ?.takeIf { it > BigDecimal.ZERO }
    }

    private fun parseNormalizedAmount(raw: String): BigDecimal? {
        val trimmed = raw.trim()
        val isNegative = trimmed.contains('-')
        val cleaned = trimmed.replace(NON_NUMERIC_AMOUNT_REGEX, "")
        if (cleaned.isEmpty()) return null
        val parsed = CurrencyConverter.normalizeAmountString(cleaned).toBigDecimalOrNull() ?: return null
        return if (isNegative) parsed.negate() else parsed
    }

    private fun parseCurrency(jsonObject: JSONObject): ParsedReceiptCurrency {
        val rawCurrency = optNullableString(jsonObject, "currency")?.uppercase(Locale.ROOT)

        // SLM/OCR safety net for common typo "FUR" -> "EUR"
        val normalizedCurrency = when (rawCurrency) {
            "FUR", "EU", "EUF" -> "EUR"
            else -> rawCurrency
        }

        val currency = normalizedCurrency ?: "EUR"
        return ParsedReceiptCurrency(currencyCode = currency, rawCurrency = rawCurrency)
    }

    private fun parseDate(jsonObject: JSONObject): LocalDate? {
        val dateStr = optNullableString(jsonObject, "date")
        return dateStr?.let {
            try {
                LocalDate.parse(it)
            } catch (_: Exception) {
                Timber.w("ReceiptExtractionService: failed to parse date string '%s'", it)
                null
            }
        }
    }

    private fun parseTime(jsonObject: JSONObject): LocalTime? {
        val timeStr = optNullableString(jsonObject, "time")
        return timeStr?.let {
            try {
                LocalTime.parse(it)
            } catch (_: Exception) {
                Timber.w("ReceiptExtractionService: failed to parse time string '%s'", it)
                null
            }
        }
    }

    private fun parseCategoryAndSubcategory(jsonObject: JSONObject): ParsedReceiptCategory {
        val parsedCategory = parseCategoryEnum(jsonObject)
        val parsedSubcategory = parseSubcategoryEnum(jsonObject)

        if (parsedSubcategory != null) {
            val derivedCategory = parsedSubcategory.parentCategory
            if (parsedCategory != null && parsedCategory != derivedCategory) {
                Timber.w(
                    "ReceiptExtractionService: mismatched category '%s' for subcategory '%s' — enforcing parent '%s'",
                    parsedCategory.name,
                    parsedSubcategory.name,
                    derivedCategory.name
                )
            }
            return ParsedReceiptCategory(
                category = derivedCategory.name,
                subcategory = parsedSubcategory.name
            )
        }

        return ParsedReceiptCategory(
            category = parsedCategory?.name,
            subcategory = null
        )
    }

    private fun parseCategoryEnum(jsonObject: JSONObject): ExpenseCategory? {
        val raw = optNullableString(jsonObject, "category") ?: return null
        return runCatching { ExpenseCategory.fromString(raw) }.getOrNull()
    }

    private fun parseSubcategoryEnum(jsonObject: JSONObject): ExpenseSubcategory? {
        val raw = optNullableString(jsonObject, "subcategory") ?: return null
        if (raw.equals(ExpenseSubcategory.UNSPECIFIED.name, ignoreCase = true)) return null
        return runCatching { ExpenseSubcategory.fromString(raw) }.getOrNull()
    }

    private fun parsePaymentMethod(jsonObject: JSONObject): String? {
        val paymentMethodStr = optNullableString(jsonObject, "paymentMethod")
        return paymentMethodStr?.let {
            runCatching { PaymentMethod.fromString(it).name }.getOrNull()
        }
    }

    private fun calculateConfidence(
        amount: BigDecimal?,
        rawCurrency: String?,
        date: LocalDate?,
        title: String?,
        vendor: String?
    ): ExtractionConfidence {
        val extractedFieldsCount = listOfNotNull(amount, rawCurrency, date, title ?: vendor).size
        return when (extractedFieldsCount) {
            FIELD_COUNT_ALL -> ExtractionConfidence.HIGH
            FIELD_COUNT_THREE, FIELD_COUNT_TWO -> ExtractionConfidence.MEDIUM
            else -> ExtractionConfidence.LOW
        }
    }

    private data class ParsedReceiptCategory(
        val category: String?,
        val subcategory: String?
    )

    private data class ParsedReceiptCurrency(
        val currencyCode: String,
        val rawCurrency: String?
    )
}
