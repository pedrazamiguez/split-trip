package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import es.pedrazamiguez.splittrip.data.R
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

internal class ReceiptExtractionServiceImpl(
    private val context: Context,
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreInferenceRepository: Lazy<AiInferenceRepository>,
    private val liteRtInferenceRepository: Lazy<AiInferenceRepository>,
    private val aiModelResolver: AiModelResolverService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ReceiptExtractionService, AutoCloseable {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + defaultDispatcher)

    @Volatile
    private var activeEngine: AiEngineType = AiEngineType.AI_CORE_GEMMA_4

    init {
        serviceScope.launch {
            aiModelResolver.getActiveModel().collect { engine ->
                activeEngine = engine
            }
        }
    }

    override suspend fun extract(
        rawText: RawReceiptText,
        engineType: AiEngineType?
    ): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        val resolvedEngine = engineType ?: aiModelResolver.getActiveModel().first()
        val isSupported = isEngineSupported(resolvedEngine)

        Timber.d(
            "ReceiptExtractionService: resolvedEngine=%s, capability=%s — %s",
            resolvedEngine,
            if (isSupported) "SUPPORTED" else "UNSUPPORTED",
            if (isSupported) "running inference" else "returning NO_OP fallback immediately"
        )

        if (!isSupported || rawText.fullText.isBlank()) {
            return@withContext Result.success(getNoOpFallbackReceipt())
        }

        val startMs = System.currentTimeMillis()
        val truncatedText = smartTruncate(rawText.fullText)
        val prompt = buildPrompt(truncatedText)

        val inferenceResult = runInference(resolvedEngine, prompt)

        val result = inferenceResult.mapCatching { rawOutput ->
            Timber.d(
                "ReceiptExtractionService: raw AI output (len=%d): %s",
                rawOutput.length,
                rawOutput.take(OCR_DIAGNOSTIC_SAMPLE_CHARS)
            )
            parseInferenceResult(rawOutput, resolvedEngine)
        }.recover { error ->
            Timber.w(
                error,
                "ReceiptExtractionService: Inference/Parsing failed mid-flight — falling back to NO_OP (elapsed=%dms)",
                System.currentTimeMillis() - startMs
            )
            getNoOpFallbackReceipt()
        }

        Timber.d(
            "ReceiptExtractionService: extraction finished in %dms — source=%s",
            System.currentTimeMillis() - startMs,
            result.getOrNull()?.source
        )
        result
    }

    private fun isEngineSupported(activeEngine: AiEngineType): Boolean {
        return when (activeEngine) {
            AiEngineType.AI_CORE_GEMMA_4 -> aiCoreCapabilityProvider.isSupported()
            AiEngineType.LITE_RT_LM -> true
        }
    }

    private suspend fun runInference(activeEngine: AiEngineType, prompt: String): Result<String> {
        return when (activeEngine) {
            AiEngineType.AI_CORE_GEMMA_4 -> {
                aiCoreInferenceRepository.value.generateContent(prompt)
            }
            AiEngineType.LITE_RT_LM -> {
                val schema = getJsonSchema()
                liteRtInferenceRepository.value.generateStructuredOutput(prompt, schema)
            }
        }
    }

    private fun parseInferenceResult(rawOutput: String, activeEngine: AiEngineType): ExtractedReceipt {
        val cleanJson = if (activeEngine == AiEngineType.AI_CORE_GEMMA_4) {
            JsonSanitizer.sanitize(rawOutput)
        } else {
            rawOutput.trim()
        }

        require(cleanJson.startsWith("{")) {
            "Output is not valid JSON (length=${cleanJson.length})"
        }
        return parseJsonToReceipt(cleanJson, activeEngine)
    }

    override fun capability(): ExtractionCapability {
        return if (activeEngine == AiEngineType.LITE_RT_LM || aiCoreCapabilityProvider.isSupported()) {
            ExtractionCapability.ON_DEVICE_AI
        } else {
            ExtractionCapability.UNSUPPORTED
        }
    }

    override fun close() {
        serviceJob.cancel()
    }

    private fun loadPromptTemplate(): String {
        return try {
            context.resources.openRawResource(R.raw.ai_prompt).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Timber.e(e, "ReceiptExtractionService: failed to load prompt template from raw resources")
            DEFAULT_PROMPT_TEMPLATE
        }
    }

    private fun buildPrompt(ocrText: String): String {
        val template = loadPromptTemplate()
        return template.replace("%1\$s", ocrText)
    }

    private fun parseJsonToReceipt(cleanJson: String, activeEngine: AiEngineType): ExtractedReceipt {
        val jsonObject = JSONObject(cleanJson)

        val amount = parseAmount(jsonObject)
        val (currency, rawCurrency) = parseCurrency(jsonObject)
        val date = parseDate(jsonObject)
        val time = parseTime(jsonObject)

        val title = jsonObject.optString("title").takeIf { it.isNotEmpty() && it != "null" }
        val vendor = jsonObject.optString("vendor").takeIf { it.isNotEmpty() && it != "null" }

        val category = parseCategory(jsonObject)
        val paymentMethod = parsePaymentMethod(jsonObject)
        val notes = jsonObject.optString("notes").takeIf { it.isNotEmpty() && it != "null" }

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
            category = category,
            paymentMethod = paymentMethod,
            notes = notes,
            source = source,
            confidence = confidence
        )
    }

    companion object {
        private const val MAX_OCR_INPUT_CHARS = 8_000
        private const val OCR_INPUT_HEAD_CHARS = 2_000
        private const val SEPARATOR = "\n…\n"
        private const val OCR_INPUT_TAIL_CHARS = MAX_OCR_INPUT_CHARS - OCR_INPUT_HEAD_CHARS - SEPARATOR.length
        private const val OCR_DIAGNOSTIC_SAMPLE_CHARS = 300

        // Referenced by name in DEFAULT_PROMPT_TEMPLATE to avoid ${'$'} escaping inside triple-quoted strings.
        private const val OCR_PLACEHOLDER = "%1\$s"

        internal fun smartTruncate(text: String): String {
            if (text.length <= MAX_OCR_INPUT_CHARS) return text
            val head = text.take(OCR_INPUT_HEAD_CHARS)
            val tail = text.takeLast(OCR_INPUT_TAIL_CHARS)
            return "$head$SEPARATOR$tail"
        }

        @Suppress("MaxLineLength")
        private val DEFAULT_PROMPT_TEMPLATE = """
            Extract the following fields from the receipt in JSON format.
            CRITICAL: If the document contains multiple separate tickets or passenger sections and each shows its own TOTAL, extract ALL of these individual totals into the "amounts" array. IVA/tax values shown next to a TOTAL are already included in that TOTAL — do not extract them.

            Fields to extract:
            - Array of the final total amounts paid as decimal strings (amounts). If there are multiple tickets/passengers with their own totals, list each separate total. If there is one grand total, list just that one total. Do not list individual grocery items. If you cannot find any total amount, do not guess or invent a value — leave the amounts array empty [].
            - ISO-4217 currency code. If currency cannot be determined, default to EUR (currency).
            - Date of the transaction in YYYY-MM-DD format (date).
            - Time of the transaction in HH:MM format (time).
            - Merchant or store name (vendor).
            - Brief description of what was purchased, maximum 3-4 words (title).
            - Category (must be one of: TRANSPORT, FOOD, LODGING, ACTIVITIES, INSURANCE, ENTERTAINMENT, SHOPPING, OTHER) (category).
            - Payment method (must be one of: CASH, BIZUM, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, PAYPAL, VENMO, ALIPAY, WECHAT_PAY, OTHER) (paymentMethod).
            - Any relevant notes or identifiers like booking code, reservation ID, locator ("localizador") code, ticket ID, seats, flight or train numbers, reference number, etc. (notes).

            Write the "title" in English. Keep it brief and descriptive. For transport, include origin and destination when available, e.g. "Train Seville-Madrid", "Flight to Barcelona". For other purchases, describe what was bought, e.g. "Dinner at restaurant", "Grocery shopping". Do not write full sentences. Max 3-4 words.

            Input: QUICK MART Drink 25.00 Snack 15.00 Water 10.00 TOTAL 50.00 USD 2025-03-10 13:45 Cash BOOKID: ABC123D Seats: 14A, 14B
            Output: {"amounts":["50.00"],"currency":"USD","date":"2025-03-10","time":"13:45","vendor":"Quick Mart","title":"Snacks and drinks","category":"FOOD","paymentMethod":"CASH","notes":"Book ID: ABC123D, Seats: 14A, 14B"}
            Input: TRAINLINE London to Paris ticket 1: 55.00 EUR ticket 2: 55.00 EUR Date: 2026-05-25 09:00 CreditCard Seats: 2A, 2B Booking: TX12345
            Output: {"amounts":["55.00","55.00"],"currency":"EUR","date":"2026-05-25","time":"09:00","vendor":"Trainline","title":"Train London-Paris","category":"TRANSPORT","paymentMethod":"CREDIT_CARD","notes":"Booking: TX12345, Seats: 2A, 2B"}
            Input: RENFE Localizador: HXDC8B A.PEDRAZA Origen: SEVILLA S JUSTA Destino: MADRID P.ATOCHA 10/10/2026 09:34 Coche: 6 Plaza: 1B AVE 02091 ESTANDAR TOTAL 42,20 € IVA (10%) 3,84 € RENFE Localizador: HXDC8B A.NARANJO Origen: SEVILLA S JUSTA Destino: MADRID P.ATOCHA 10/10/2026 09:34 Coche: 6 Plaza: 1A AVE 02091 ESTANDAR TOTAL 42,20 € IVA (10%) 3,84 €
            Output: {"amounts":["42.20","42.20"],"currency":"EUR","date":"2026-10-10","time":"09:34","vendor":"Renfe","title":"Train Sevilla-Madrid","category":"TRANSPORT","paymentMethod":"CREDIT_CARD","notes":"Locator: HXDC8B, Seats: 1A, 1B, Train: AVE 02091"}
            Input: $OCR_PLACEHOLDER
            Output:
        """.trimIndent()
    }
}

// ── File-Level Helpers ───────────────────────────────────────────────────────

private const val FIELD_COUNT_ALL = 4
private const val FIELD_COUNT_THREE = 3
private const val FIELD_COUNT_TWO = 2

private fun getJsonSchema(): String {
    return """
            {
              "type": "object",
              "properties": {
                "amounts": {
                  "type": "array",
                  "items": { "type": "string" }
                },
                "currency": { "type": "string" },
                "date": { "type": "string" },
                "time": { "type": "string" },
                "vendor": { "type": "string" },
                "title": { "type": "string" },
                "category": { "type": "string" },
                "paymentMethod": { "type": "string" },
                "notes": { "type": "string" }
              },
              "required": ["amounts", "currency", "date", "vendor", "title"]
            }
    """.trimIndent()
}

private fun parseAmount(jsonObject: JSONObject): BigDecimal? {
    val amountsArray = jsonObject.optJSONArray("amounts")
    if (amountsArray != null && amountsArray.length() > 0) {
        var sum = BigDecimal.ZERO
        for (i in 0 until amountsArray.length()) {
            val str = amountsArray.optString(i).replace(Regex("[^0-9.,]"), "")
            if (str.isNotEmpty()) {
                sum += CurrencyConverter.normalizeAmountString(str).toBigDecimalOrNull() ?: BigDecimal.ZERO
            }
        }
        if (sum > BigDecimal.ZERO) return sum
    }

    // Fallback for generic/legacy AI output
    val amountStr = jsonObject.optString("amount").takeIf { it.isNotEmpty() && it != "null" }
    return amountStr?.let {
        val cleaned = it.replace(Regex("[^0-9.,]"), "")
        if (cleaned.isNotEmpty()) {
            CurrencyConverter.normalizeAmountString(cleaned).toBigDecimalOrNull()
        } else {
            null
        }
    }
}

private fun parseCurrency(jsonObject: JSONObject): Pair<String, String?> {
    val rawCurrency = jsonObject.optString("currency")
        .takeIf { it.isNotEmpty() && it != "null" }?.uppercase(Locale.ROOT)
    val currency = rawCurrency ?: "EUR"
    return Pair(currency, rawCurrency)
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

private fun parseDate(jsonObject: JSONObject): LocalDate? {
    val dateStr = jsonObject.optString("date").takeIf { it.isNotEmpty() && it != "null" }
    return dateStr?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            Timber.w("ReceiptExtractionService: failed to parse date string '%s'", it)
            null
        }
    }
}

private fun parseTime(jsonObject: JSONObject): java.time.LocalTime? {
    val timeStr = jsonObject.optString("time").takeIf { it.isNotEmpty() && it != "null" }
    return timeStr?.let {
        try {
            java.time.LocalTime.parse(it)
        } catch (_: Exception) {
            Timber.w("ReceiptExtractionService: failed to parse time string '%s'", it)
            null
        }
    }
}

private fun parseCategory(jsonObject: JSONObject): String? {
    val categoryStr = jsonObject.optString("category").takeIf { it.isNotEmpty() && it != "null" }
    return categoryStr?.let {
        try {
            es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory.fromString(it).name
        } catch (_: Exception) {
            null
        }
    }
}

private fun parsePaymentMethod(jsonObject: JSONObject): String? {
    val paymentMethodStr = jsonObject.optString("paymentMethod").takeIf { it.isNotEmpty() && it != "null" }
    return paymentMethodStr?.let {
        try {
            es.pedrazamiguez.splittrip.domain.enums.PaymentMethod.fromString(it).name
        } catch (_: Exception) {
            null
        }
    }
}

private fun getNoOpFallbackReceipt(): ExtractedReceipt {
    return ExtractedReceipt(
        amount = null,
        currency = null,
        date = null,
        time = null,
        title = null,
        vendor = null,
        category = null,
        paymentMethod = null,
        notes = null,
        source = ExtractionSource.NO_OP,
        confidence = ExtractionConfidence.LOW
    )
}
