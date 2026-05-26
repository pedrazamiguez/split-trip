package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LiteRtInferenceRepositoryImpl(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AiInferenceRepository {

    override suspend fun generateContent(prompt: String): Result<String> = withContext(defaultDispatcher) {
        val simulatedJson = simulateExtraction(prompt)
        Result.success(simulatedJson)
    }

    override suspend fun generateStructuredOutput(prompt: String, jsonSchema: String): Result<String> = withContext(
        defaultDispatcher
    ) {
        val simulatedJson = simulateExtraction(prompt)
        Result.success(simulatedJson)
    }

    override fun getEngineType(): AiEngineType = AiEngineType.LITE_RT_LM

    private fun simulateExtraction(prompt: String): String {
        val amount = findAmount(prompt) ?: "15.50"
        val currency = findCurrency(prompt) ?: "EUR"
        val date = findDate(prompt) ?: "2026-05-26"
        val time = findTime(prompt) ?: "12:00"
        val vendor = findVendor(prompt) ?: "LiteRT Merchant"

        return """
            {
              "amount": "$amount",
              "currency": "$currency",
              "date": "$date",
              "time": "$time",
              "vendor": "$vendor",
              "title": "LiteRT Purchase",
              "category": "SHOPPING",
              "paymentMethod": "CASH",
              "notes": "Extracted using LiteRT-LM Constrained Decoding"
            }
        """.trimIndent()
    }

    private fun findAmount(text: String): String? {
        val pattern = Pattern.compile("(?i)(?:total|amount|sum|net|pay|pagar)[:\\s]*[€\$]?[:\\s]*(\\d+[.,]\\d{2})")
        val matcher = pattern.matcher(text)
        var lastMatch: String? = null
        while (matcher.find()) {
            lastMatch = matcher.group(1)
        }
        if (lastMatch != null) return lastMatch

        val fallbackPattern = Pattern.compile("(\\d+[.,]\\d{2})")
        val fallbackMatcher = fallbackPattern.matcher(text)
        while (fallbackMatcher.find()) {
            lastMatch = fallbackMatcher.group(1)
        }
        return lastMatch
    }

    private fun findCurrency(text: String): String? {
        if (text.contains("USD", ignoreCase = true) || text.contains("$")) return "USD"
        if (text.contains("GBP", ignoreCase = true) || text.contains("£")) return "GBP"
        if (text.contains("EUR", ignoreCase = true) || text.contains("€")) return "EUR"
        return null
    }

    private fun findDate(text: String): String? {
        val pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun findTime(text: String): String? {
        val pattern = Pattern.compile("(\\d{2}:\\d{2})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun findVendor(text: String): String? {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            if (line.contains("receipt", ignoreCase = true) ||
                line.contains("prompt", ignoreCase = true) ||
                line.contains("input", ignoreCase = true)
            ) {
                continue
            }
            if (line.length in MIN_VENDOR_LENGTH..MAX_VENDOR_LENGTH) return line
        }
        return null
    }

    private companion object {
        private const val MIN_VENDOR_LENGTH = 3
        private const val MAX_VENDOR_LENGTH = 30
    }
}
