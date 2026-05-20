package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import com.google.ai.edge.aicore.GenerativeModel
import com.google.ai.edge.aicore.generationConfig
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * On-device receipt text parser using Android AICore (Gemini Nano).
 */
internal class AICoreReceiptParser(
    private val appContext: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val generativeModelProvider: (() -> GenerativeModel)? = null
) {
    private fun createModel(): GenerativeModel {
        return generativeModelProvider?.invoke() ?: GenerativeModel(
            generationConfig = generationConfig {
                context = appContext.applicationContext
                temperature = 0.0f
                maxOutputTokens = PARSER_MAX_OUTPUT_TOKENS
            }
        )
    }

    /**
     * Sends the OCR receipt text blocks to Gemini Nano via AICore.
     * Parses the response into an [ExtractedReceipt].
     */
    suspend fun parse(rawText: RawReceiptText): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        runCatching {
            if (rawText.fullText.isBlank()) {
                return@runCatching emptyAiCoreReceipt()
            }

            val model = createModel()

            val prompt = buildPrompt(rawText.fullText)
            val response = model.generateContent(prompt)
            val jsonStr = response.text?.trim() ?: ""
            val cleanJson = jsonStr
                .removePrefix("```json")
                .removeSuffix("```")
                .trim()

            parseJsonToReceipt(cleanJson)
        }.onFailure {
            Timber.e(it, "AICoreReceiptParser failed to extract receipt data")
        }
    }

    private fun parseJsonToReceipt(cleanJson: String): ExtractedReceipt {
        val jsonObject = JSONObject(cleanJson)

        val amountStr = jsonObject.optString("amount").takeIf { it.isNotEmpty() && it != "null" }
        val amount = amountStr?.toBigDecimalOrNull()

        val currency = jsonObject.optString("currency")
            .takeIf { it.isNotEmpty() && it != "null" }?.uppercase()

        val dateStr = jsonObject.optString("date").takeIf { it.isNotEmpty() && it != "null" }
        val date = dateStr?.let {
            try {
                LocalDate.parse(it)
            } catch (_: Exception) {
                null
            }
        }

        val title = jsonObject.optString("title").takeIf { it.isNotEmpty() && it != "null" }

        val extractedFieldsCount = listOfNotNull(amount, currency, date, title).size
        val confidence = when (extractedFieldsCount) {
            FIELD_COUNT_ALL -> ExtractionConfidence.HIGH
            FIELD_COUNT_THREE, FIELD_COUNT_TWO -> ExtractionConfidence.MEDIUM
            else -> ExtractionConfidence.LOW
        }

        return ExtractedReceipt(
            amount = amount,
            currency = currency,
            date = date,
            title = title,
            source = ExtractionSource.AI_CORE,
            confidence = confidence
        )
    }

    companion object {
        private const val PARSER_MAX_OUTPUT_TOKENS = 512
        private const val FIELD_COUNT_ALL = 4
        private const val FIELD_COUNT_THREE = 3
        private const val FIELD_COUNT_TWO = 2

        private fun emptyAiCoreReceipt() = ExtractedReceipt(
            amount = null,
            currency = null,
            date = null,
            title = null,
            source = ExtractionSource.AI_CORE,
            confidence = ExtractionConfidence.LOW
        )

        private fun buildPrompt(ocrText: String): String = """
            You are an expert receipt data extractor. Analyze the following OCR raw text from a receipt and extract:
            - amount (the total paid, as a decimal number like 12.34 or null if not found)
            - currency (the 3-letter ISO 4217 code like USD, EUR, GBP, or null if not found)
            - date (the transaction date in YYYY-MM-DD format, or null if not found)
            - title (the merchant name/store name, or null if not found)

            Provide the output ONLY as a valid JSON object with the keys "amount", "currency", "date", "title". Do not include any other text, markdown formatting (like ```json), or explanations.

            OCR Raw Text:
            $ocrText
        """.trimIndent()
    }
}
