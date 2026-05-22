package es.pedrazamiguez.splittrip.data.service

import com.google.ai.edge.aicore.Candidate
import com.google.ai.edge.aicore.GenerativeModel
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import java.time.LocalDate
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

internal class AICoreReceiptParser(
    private val generativeModel: GenerativeModel,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    @Volatile private var enginePrepared = false
    private val preparationMutex = Mutex()

    suspend fun parse(rawText: RawReceiptText): Result<ExtractedReceipt> = withContext(defaultDispatcher) {
        try {
            if (rawText.fullText.isBlank()) {
                Timber.d("AICoreReceiptParser: raw text is blank — returning empty receipt")
                Result.success(emptyAiCoreReceipt())
            } else {
                val cleanJson = runInference(rawText.fullText)
                if (cleanJson == null) {
                    Result.success(emptyAiCoreReceipt())
                } else {
                    Timber.d("AICoreReceiptParser: parsing JSON response (%d chars)", cleanJson.length)
                    Result.success(parseJsonToReceipt(cleanJson))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "AICoreReceiptParser: extraction failed")
            Result.failure(e)
        }
    }

    private suspend fun ensureEngineReady() {
        if (enginePrepared) return
        preparationMutex.withLock {
            if (!enginePrepared) {
                Timber.d("AICoreReceiptParser: preparing inference engine (loading weights into NPU)…")
                val startMs = System.currentTimeMillis()
                generativeModel.prepareInferenceEngine()
                enginePrepared = true
                Timber.d("AICoreReceiptParser: inference engine ready in ${System.currentTimeMillis() - startMs}ms")
            }
        }
    }

    private suspend fun runInference(ocrText: String): String? {
        ensureEngineReady()

        val truncatedText = smartTruncate(ocrText)
        Timber.d("AICoreReceiptParser: ocr text: %d chars (raw=%d)", truncatedText.length, ocrText.length)
        Timber.d("AICoreReceiptParser: sending prompt (text length=%d chars)", truncatedText.length)
        val inferenceStartMs = System.currentTimeMillis()
        val response = generativeModel.generateContent(buildPrompt(truncatedText))
        val inferenceMs = System.currentTimeMillis() - inferenceStartMs
        val rawText = response.text?.trim() ?: ""
        val finishReason = response.candidates.firstOrNull()?.finishReason
        Timber.d(
            "AICoreReceiptParser: inference in ${inferenceMs}ms — length=%d, finishReason=%s",
            rawText.length,
            finishReason
        )
        if (finishReason == Candidate.FinishReason.MAX_TOKENS) {
            Timber.w("AICoreReceiptParser: response truncated at maxOutputTokens — consider shortening the input")
        }

        val cleanJson = rawText.removePrefix("```json").removeSuffix("```").trim()
        return if (cleanJson.startsWith("{")) {
            cleanJson
        } else {
            Timber.w("AICoreReceiptParser: not valid JSON (length=%d)", cleanJson.length)
            null
        }
    }

    private fun parseJsonToReceipt(cleanJson: String): ExtractedReceipt {
        val jsonObject = JSONObject(cleanJson)

        val amountStr = jsonObject.optString("amount").takeIf { it.isNotEmpty() && it != "null" }
        val amount = amountStr?.toBigDecimalOrNull()

        val currency = jsonObject.optString("currency")
            .takeIf { it.isNotEmpty() && it != "null" }?.uppercase(Locale.ROOT)

        val dateStr = jsonObject.optString("date").takeIf { it.isNotEmpty() && it != "null" }
        val date = dateStr?.let {
            try {
                LocalDate.parse(it)
            } catch (_: Exception) {
                Timber.w("AICoreReceiptParser: failed to parse date string '%s'", it)
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

        Timber.d(
            "AICoreReceiptParser: parsed %d/%d fields — confidence=%s",
            extractedFieldsCount,
            FIELD_COUNT_ALL,
            confidence
        )

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
        private const val FIELD_COUNT_ALL = 4
        private const val FIELD_COUNT_THREE = 3
        private const val FIELD_COUNT_TWO = 2

        // Multi-page PDFs (up to 5 pages) easily exceed 900 chars. The grand total on a
        // flight receipt always appears near the end, so a head-only `.take()` misses it.
        // Budget raised to 3 000 chars — well within Gemini Nano's 8k-token context window.
        private const val MAX_OCR_INPUT_CHARS = 3_000

        // Head captures merchant/airline name and booking reference (top of document).
        // Tail captures fare breakdown and grand total (bottom of document).
        // Together they stay within MAX_OCR_INPUT_CHARS even for long multi-page receipts.
        private const val OCR_INPUT_HEAD_CHARS = 600
        private const val SEPARATOR = "\n…\n"
        private const val OCR_INPUT_TAIL_CHARS = MAX_OCR_INPUT_CHARS - OCR_INPUT_HEAD_CHARS - SEPARATOR.length

        /**
         * Returns at most [MAX_OCR_INPUT_CHARS] of [text].
         *
         * When truncation is needed we keep the first [OCR_INPUT_HEAD_CHARS] (merchant/airline
         * name, booking ref) and the last [OCR_INPUT_TAIL_CHARS] (fare breakdown, grand total)
         * rather than blindly taking the head. This ensures the grand total — which appears at
         * the bottom of multi-page flight receipts — is always present in the prompt.
         */
        internal fun smartTruncate(text: String): String {
            if (text.length <= MAX_OCR_INPUT_CHARS) return text
            val head = text.take(OCR_INPUT_HEAD_CHARS)
            val tail = text.takeLast(OCR_INPUT_TAIL_CHARS)
            return "$head$SEPARATOR$tail"
        }

        private fun emptyAiCoreReceipt() = ExtractedReceipt(
            amount = null,
            currency = null,
            date = null,
            title = null,
            source = ExtractionSource.AI_CORE,
            confidence = ExtractionConfidence.LOW
        )

        // Few-shot completion: multi-item example teaches the model to pick the GRAND TOTAL,
        // not an individual item price. The "Output:" suffix triggers JSON completion.
        private fun buildPrompt(ocrText: String): String =
            "Grand total, ISO-4217 currency, date YYYY-MM-DD, merchant name.\n" +
                "Input: QUICK MART Drink 25.00 Snack 15.00 Water 10.00 TOTAL 50.00 USD 2025-03-10\n" +
                "Output: {\"amount\":\"50.00\",\"currency\":\"USD\"," +
                "\"date\":\"2025-03-10\",\"title\":\"Quick Mart\"}\n" +
                "Input: $ocrText\n" +
                "Output:"
    }
}
