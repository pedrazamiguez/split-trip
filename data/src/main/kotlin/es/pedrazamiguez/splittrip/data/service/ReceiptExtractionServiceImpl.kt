package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import es.pedrazamiguez.splittrip.data.R
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
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

@Suppress("LongParameterList")
internal class ReceiptExtractionServiceImpl(
    private val context: Context,
    private val appConfigRepository: AppConfigRepository,
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider,
    private val aiCoreInferenceRepository: Lazy<AiInferenceRepository>,
    private val liteRtInferenceRepository: Lazy<AiInferenceRepository>,
    private val aiModelResolver: AiModelResolverService,
    private val localAiModelPackService: LocalAiModelPackService,
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

    @Suppress("ReturnCount")
    override suspend fun extract(
        rawText: RawReceiptText,
        engineType: AiEngineType?
    ): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        if (rawText.fullText.isBlank()) {
            return@withContext Result.success(getNoOpFallbackReceipt())
        }

        val resolvedEngine = engineType ?: aiModelResolver.getActiveModel().first()
        var executionEngine = resolveExecutionEngine(
            resolvedEngine = resolvedEngine,
            isAutomaticMode = engineType == null
        ) ?: return@withContext Result.success(getNoOpFallbackReceipt())

        val startMs = System.currentTimeMillis()
        val blacklist = appConfigRepository.ocrSafetyFalsePositivesBlacklist.value
        val sanitizedText = sanitizeOcrText(rawText.fullText, blacklist)

        var inferenceResult = runInferenceWithRetry(executionEngine, sanitizedText, startMs)

        if (inferenceResult.isFailure && engineType == null && executionEngine == AiEngineType.LITE_RT_LM) {
            val error = inferenceResult.exceptionOrNull()
            if (aiCoreCapabilityProvider.isSupported()) {
                Timber.w(
                    error,
                    "ReceiptExtractionService: LITE_RT_LM failed in automatic mode — falling back to AI_CORE_GEMMA_4"
                )
                executionEngine = AiEngineType.AI_CORE_GEMMA_4
                inferenceResult = runInferenceWithRetry(executionEngine, sanitizedText, startMs)
            } else if (error is LocalAiModelUnavailableException || error is LocalAiModelNotEntitledException) {
                Timber.w(error, "ReceiptExtractionService: LITE_RT_LM unavailable — returning NO_OP fallback")
                return@withContext Result.success(getNoOpFallbackReceipt())
            }
        }

        val result = inferenceResult.mapCatching { rawOutput ->
            Timber.d(
                "ReceiptExtractionService: raw AI output (len=%d)",
                rawOutput.length
            )
            parseInferenceResult(rawOutput, executionEngine)
        }.onFailure { error ->
            Timber.w(
                error,
                "ReceiptExtractionService: Inference/Parsing failed mid-flight (elapsed=%dms)",
                System.currentTimeMillis() - startMs
            )
        }

        Timber.d(
            "ReceiptExtractionService: extraction finished in %dms — source=%s",
            System.currentTimeMillis() - startMs,
            result.getOrNull()?.source
        )
        result
    }

    private fun resolveExecutionEngine(
        resolvedEngine: AiEngineType,
        isAutomaticMode: Boolean
    ): AiEngineType? {
        val isSupported = isEngineSupported(resolvedEngine)
        Timber.d(
            "ReceiptExtractionService: resolvedEngine=%s, capability=%s",
            resolvedEngine,
            if (isSupported) "SUPPORTED" else "UNSUPPORTED"
        )
        if (isSupported) return resolvedEngine
        if (isAutomaticMode &&
            resolvedEngine == AiEngineType.LITE_RT_LM &&
            aiCoreCapabilityProvider.isSupported()
        ) {
            Timber.w("ReceiptExtractionService: LITE_RT_LM pack not installed — falling back to AI_CORE_GEMMA_4")
            return AiEngineType.AI_CORE_GEMMA_4
        }
        return null
    }

    private suspend fun runInferenceWithRetry(
        executionEngine: AiEngineType,
        sanitizedText: String,
        startMs: Long
    ): Result<String> {
        val truncatedText = smartTruncate(sanitizedText)
        val prompt = buildPrompt(truncatedText)
        var inferenceResult = runInference(executionEngine, prompt)

        if (inferenceResult.isFailure) {
            Timber.e(
                inferenceResult.exceptionOrNull(),
                "ReceiptExtractionService: Primary inference failed " +
                    "(elapsed=%dms). Triggering fallback. Offending payload:\n%s",
                System.currentTimeMillis() - startMs,
                truncatedText
            )
            val aggressiveText = aggressiveTruncate(sanitizedText)
            val aggressivePrompt = buildPrompt(aggressiveText)
            inferenceResult = runInference(executionEngine, aggressivePrompt)
        }
        return inferenceResult
    }

    private fun sanitizeOcrText(text: String, blacklist: List<String>): String {
        var sanitized = text
        for (word in blacklist) {
            // Remove the word case-insensitively, allowing for boundary matching
            sanitized = sanitized.replace(Regex("(?i)\\b$word\\b"), "")
        }
        // Clean up any double spaces created by the removal
        return sanitized.replace(Regex(" {2,}"), " ").trim()
    }

    private fun isEngineSupported(activeEngine: AiEngineType): Boolean {
        return when (activeEngine) {
            AiEngineType.AI_CORE_GEMMA_4 -> aiCoreCapabilityProvider.isSupported()
            AiEngineType.LITE_RT_LM -> localAiModelPackService.isPackInstalled()
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
        val cleanJson = JsonSanitizer.sanitize(rawOutput)

        require(cleanJson.startsWith("{")) {
            "Output is not valid JSON (length=${cleanJson.length})"
        }
        return parseJsonToReceipt(cleanJson, activeEngine)
    }

    override fun capability(): ExtractionCapability {
        return if (isEngineSupported(activeEngine) || aiCoreCapabilityProvider.isSupported()) {
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

        val parsedCategory = parseCategoryAndSubcategory(jsonObject)
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
            category = parsedCategory.category,
            subcategory = parsedCategory.subcategory,
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

        // Referenced by name in DEFAULT_PROMPT_TEMPLATE to avoid ${'$'} escaping inside triple-quoted strings.
        private const val OCR_PLACEHOLDER = "%1\$s"

        private const val AGGRESSIVE_TRUNCATION_CHARS = 1_000

        internal fun smartTruncate(text: String): String {
            if (text.length <= MAX_OCR_INPUT_CHARS) return text
            val head = text.take(OCR_INPUT_HEAD_CHARS)
            val tail = text.takeLast(OCR_INPUT_TAIL_CHARS)
            return "$head$SEPARATOR$tail"
        }

        internal fun aggressiveTruncate(text: String): String {
            return if (text.length <= AGGRESSIVE_TRUNCATION_CHARS) text else text.take(AGGRESSIVE_TRUNCATION_CHARS)
        }

        @Suppress("MaxLineLength")
        private val DEFAULT_PROMPT_TEMPLATE = """
            Extract the following fields from the receipt in JSON format.
            CRITICAL: If the document contains multiple separate tickets or passenger sections and each shows its own TOTAL, extract ALL of these individual totals into the "amounts" array. IVA/tax values shown next to a TOTAL are already included in that TOTAL — do not extract them.

            Fields to extract:
            - Array of the final total amounts paid as decimal strings (amounts). If there are multiple tickets/passengers with their own totals, list each separate total. If there is one grand total, list just that one total. Do not list individual grocery items. If you cannot find any total amount, do not guess or invent a value — leave the amounts array empty [].
            - ISO-4217 currency code. Look for the precise code or symbol (e.g., EUR, $, £) next to the total amount. Correct common OCR typos (like extracting FUR instead of EUR). Do not guess based on vendor or country name. If it cannot be determined, default to EUR (currency).
            - Date of the transaction/purchase in YYYY-MM-DD format (date). Do not use the travel/event date for this field.
            - Time of the transaction in HH:MM format (time).
            - Merchant or store name (vendor).
            - Brief description of what was purchased, maximum 3-4 words (title).
            - Category (must be one of: TRANSPORT, FOOD, LODGING, HOME, HEALTH_PERSONAL_CARE, ACTIVITIES, INSURANCE, ENTERTAINMENT, SHOPPING, OTHER) (category).
            - Optional subcategory belonging to the chosen category from the allowed taxonomy below, or null if uncertain (subcategory).
            - Payment method (must be one of: CASH, BIZUM, PIX, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, PAYPAL, VENMO, ALIPAY, WECHAT_PAY, OTHER) (paymentMethod).
            - Any relevant notes or identifiers like travel/event dates, booking code, reservation ID, locator ("localizador") code, ticket ID, seats, flight or train numbers, reference number, etc. (notes).

            Allowed subcategories per category:
            TRANSPORT: INTERNATIONAL_FLIGHT,DOMESTIC_FLIGHT,TRAIN,BUS,TAXI_RIDESHARE,PRIVATE_TRANSFER,CAR_RENTAL,MOTORBIKE_RENTAL,FUEL,TOLLS_PARKING,FERRY_BOAT,PUBLIC_TRANSIT,CAR_REPAIR_MAINTENANCE
            FOOD: RESTAURANT,STREET_FOOD,GROCERIES_SUPERMARKET,CAFE_BREAKFAST,BAR_DRINKS,DELIVERY
            LODGING: HOTEL,HOSTEL,VACATION_RENTAL,RESORT,CAMPING,HOMESTAY
            HOME: RENT_MORTGAGE,UTILITIES,HOME_REPAIR,FURNITURE_APPLIANCES,HOME_SUPPLIES
            HEALTH_PERSONAL_CARE: DOCTOR_DENTIST,HAIRDRESSER_BARBER,GYM_FITNESS,PERSONAL_CARE
            ACTIVITIES: MUSEUM_CULTURE,TOUR_EXCURSION,NATURE_PARK,WATER_SPORTS,ADVENTURE_SPORTS,SPA_WELLNESS,TICKETS_ATTRACTIONS
            ENTERTAINMENT: CONCERT_FESTIVAL,NIGHTLIFE_CLUB,CINEMA_THEATER,GAMES_ARCADE,SPORTS_EVENT,SUBSCRIPTIONS_STREAMING
            SHOPPING: CLOTHING,SOUVENIRS_GIFTS,ELECTRONICS,PHARMACY_HEALTH,CONVENIENCE_STORE,GIFTS,TOBACCO_VAPING,HOBBIES
            INSURANCE: TRAVEL_INSURANCE,MEDICAL_HEALTH,VEHICLE_INSURANCE,EQUIPMENT_INSURANCE,HOME_INSURANCE
            OTHER: TIPS_GRATUITIES,SIM_COMMUNICATIONS,LAUNDRY,FEES_SURCHARGES,PET_CARE

            Write the "title" in English. Keep it brief and descriptive. For transport, include origin and destination when available, e.g. "Train Seville-Madrid", "Flight to Barcelona". For other purchases, describe what was bought, e.g. "Dinner at restaurant", "Grocery shopping". Do not write full sentences. Max 3-4 words.

            Input: QUICK MART Drink 25.00 Snack 15.00 Water 10.00 TOTAL 50.00 USD 2025-03-10 13:45 Cash BOOKID: ABC123D Seats: 14A, 14B
            Output: {"amounts":["50.00"],"currency":"USD","date":"2025-03-10","time":"13:45","vendor":"Quick Mart","title":"Snacks and drinks","category":"FOOD","subcategory":"GROCERIES_SUPERMARKET","paymentMethod":"CASH","notes":"Book ID: ABC123D, Seats: 14A, 14B"}
            Input: TRAINLINE London to Paris ticket 1: 55.00 EUR ticket 2: 55.00 EUR Purchase Date: 2026-05-25 09:00 Departure: 2026-06-15 14:00 CreditCard Seats: 2A, 2B Booking: TX12345
            Output: {"amounts":["55.00","55.00"],"currency":"EUR","date":"2026-05-25","time":"09:00","vendor":"Trainline","title":"Train London-Paris","category":"TRANSPORT","subcategory":"TRAIN","paymentMethod":"CREDIT_CARD","notes":"Departure: 2026-06-15 14:00, Booking: TX12345, Seats: 2A, 2B"}
            Input: RENFE Localizador: HXDC8B A.PEDRAZA Origen: SEVILLA S JUSTA Destino: MADRID P.ATOCHA 10/10/2026 09:34 Coche: 6 Plaza: 1B AVE 02091 ESTANDAR TOTAL 42,20 € IVA (10%) 3,84 € Fecha operacion: 02/10/2026 15:20 RENFE Localizador: HXDC8B A.NARANJO Origen: SEVILLA S JUSTA Destino: MADRID P.ATOCHA 10/10/2026 09:34 Coche: 6 Plaza: 1A AVE 02091 ESTANDAR TOTAL 42,20 € IVA (10%) 3,84 €
            Output: {"amounts":["42.20","42.20"],"currency":"EUR","date":"2026-10-02","time":"15:20","vendor":"Renfe","title":"Train Seville-Madrid","category":"TRANSPORT","subcategory":"TRAIN","paymentMethod":"CREDIT_CARD","notes":"Departure: 2026-10-10 09:34, Locator: HXDC8B, Seats: 1A, 1B, Train: AVE 02091"}
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
                "subcategory": { "type": "string" },
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

private fun parseCurrency(jsonObject: JSONObject): ParsedReceiptCurrency {
    val rawCurrency = jsonObject.optString("currency")
        .takeIf { it.isNotEmpty() && it != "null" }?.uppercase(Locale.ROOT)

    // SLM/OCR safety net for common typo "FUR" -> "EUR"
    val normalizedCurrency = when (rawCurrency) {
        "FUR", "EU", "EUF" -> "EUR"
        else -> rawCurrency
    }

    val currency = normalizedCurrency ?: "EUR"
    return ParsedReceiptCurrency(currencyCode = currency, rawCurrency = rawCurrency)
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

private fun parseTime(jsonObject: JSONObject): LocalTime? {
    val timeStr = jsonObject.optString("time").takeIf { it.isNotEmpty() && it != "null" }
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
    val category = parseCategoryEnum(jsonObject) ?: return ParsedReceiptCategory(null, null)
    val subcategory = parseSubcategoryEnum(jsonObject)
    val sanitized = CategorySuggestion.sanitized(
        category = category,
        subcategory = subcategory
    )
    return ParsedReceiptCategory(
        category = sanitized.category.name,
        subcategory = sanitized.subcategory?.name
    )
}

private fun parseCategoryEnum(jsonObject: JSONObject): ExpenseCategory? {
    val raw = jsonObject.optString("category").trim()
    if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) return null
    return runCatching { ExpenseCategory.fromString(raw) }.getOrNull()
}

private fun parseSubcategoryEnum(jsonObject: JSONObject): ExpenseSubcategory? {
    val raw = jsonObject.optString("subcategory").trim()
    val isMissing = raw.isEmpty() ||
        raw.equals("null", ignoreCase = true) ||
        raw.equals(ExpenseSubcategory.UNSPECIFIED.name, ignoreCase = true)
    if (isMissing) return null
    return runCatching { ExpenseSubcategory.fromString(raw) }.getOrNull()
}

private data class ParsedReceiptCategory(
    val category: String?,
    val subcategory: String?
)

private fun parsePaymentMethod(jsonObject: JSONObject): String? {
    val paymentMethodStr = jsonObject.optString("paymentMethod").takeIf { it.isNotEmpty() && it != "null" }
    return paymentMethodStr?.let {
        try {
            PaymentMethod.fromString(it).name
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
        subcategory = null,
        paymentMethod = null,
        notes = null,
        source = ExtractionSource.NO_OP,
        confidence = ExtractionConfidence.LOW
    )
}

private data class ParsedReceiptCurrency(
    val currencyCode: String,
    val rawCurrency: String?
)
