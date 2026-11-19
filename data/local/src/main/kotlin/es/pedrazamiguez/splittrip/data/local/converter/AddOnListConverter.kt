package es.pedrazamiguez.splittrip.data.local.converter

import androidx.room.TypeConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.AddOn
import java.math.BigDecimal

/**
 * Room TypeConverter for `List<AddOn>`.
 * Stores the list as a JSON string in the database (same pattern as [CashTrancheListConverter]).
 *
 * Uses manual JSON serialization (no `org.json`) to stay compatible with pure JVM unit tests,
 * matching the approach of [StringBigDecimalMapConverter].
 *
 * Exchange rates are serialized as String (via [BigDecimal.toPlainString]) to avoid
 * IEEE 754 floating-point precision loss, consistent with the project's decimal-precision rules.
 */
class AddOnListConverter {

    @TypeConverter
    fun fromAddOnList(value: List<AddOn>?): String? {
        if (value.isNullOrEmpty()) return null
        return value.joinToString(separator = ",", prefix = "[", postfix = "]") { addOn ->
            buildString {
                append("{")
                append("\"id\":\"${escapeJson(addOn.id)}\"")
                append(",\"type\":\"${addOn.type.name}\"")
                append(",\"mode\":\"${addOn.mode.name}\"")
                append(",\"valueType\":\"${addOn.valueType.name}\"")
                append(",\"amountCents\":${addOn.amountCents}")
                append(",\"currency\":\"${escapeJson(addOn.currency)}\"")
                append(",\"exchangeRate\":\"${addOn.exchangeRate.toPlainString()}\"")
                append(",\"groupAmountCents\":${addOn.groupAmountCents}")
                append(",\"paymentMethod\":\"${addOn.paymentMethod.name}\"")
                addOn.description?.let { desc ->
                    append(",\"description\":\"${escapeJson(desc)}\"")
                }
                append("}")
            }
        }
    }

    @TypeConverter
    fun toAddOnList(value: String?): List<AddOn>? {
        if (value.isNullOrBlank()) return null

        val result = mutableListOf<AddOn>()
        // Strip outer brackets
        val inner = value.trim().removePrefix("[").removeSuffix("]")
        if (inner.isBlank()) return null

        // Split by objects: find each {...} block
        var depth = 0
        var objectStart = -1
        for (i in inner.indices) {
            when (inner[i]) {
                '{' -> {
                    if (depth == 0) objectStart = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && objectStart >= 0) {
                        val objectStr = inner.substring(objectStart + 1, i)
                        result.add(parseAddOn(objectStr))
                        objectStart = -1
                    }
                }
            }
        }
        return if (result.isEmpty()) null else result
    }

    private fun parseAddOn(objectContent: String): AddOn {
        val fields = parseJsonFields(objectContent)
        return AddOn(
            id = fields["id"] ?: "",
            type = runCatching { AddOnType.fromString(fields["type"] ?: "") }
                .getOrDefault(AddOnType.FEE),
            mode = runCatching { AddOnMode.fromString(fields["mode"] ?: "") }
                .getOrDefault(AddOnMode.ON_TOP),
            valueType = runCatching { AddOnValueType.fromString(fields["valueType"] ?: "") }
                .getOrDefault(AddOnValueType.EXACT),
            amountCents = fields["amountCents"]?.toLongOrNull() ?: 0L,
            currency = fields["currency"] ?: "EUR",
            exchangeRate = fields["exchangeRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
            groupAmountCents = fields["groupAmountCents"]?.toLongOrNull() ?: 0L,
            paymentMethod = runCatching {
                PaymentMethod.fromString(fields["paymentMethod"] ?: "")
            }.getOrDefault(PaymentMethod.OTHER),
            description = fields["description"]
        )
    }

    /**
     * Intermediate result carrying a parsed value and the cursor position after it.
     */
    private data class ParseResult(val value: String, val nextIndex: Int)

    private data class ParsedJsonField(val key: String, val value: String, val nextIndex: Int)

    /**
     * Simple JSON field parser for a flat object.
     * Handles: `"key":"stringValue"`, `"key":numericValue`, `"key":booleanValue`
     */
    private fun parseJsonFields(objectContent: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var cursor = 0

        while (cursor < objectContent.length) {
            val field = parseNextField(objectContent, cursor)
                ?: break
            result[field.key] = field.value
            cursor = field.nextIndex
        }
        return result
    }

    /**
     * Parses one `"key":value` pair starting at [from].
     * Returns `null` when there are no more fields to parse.
     */
    private fun parseNextField(
        content: String,
        from: Int
    ): ParsedJsonField? {
        val key = extractKey(content, from) ?: return null
        val value = extractValue(content, key.nextIndex) ?: return null
        val next = skipComma(content, value.nextIndex)
        return ParsedJsonField(key = key.value, value = value.value, nextIndex = next)
    }

    /** Extracts the next `"key"` and advances past the colon. */
    private fun extractKey(content: String, from: Int): ParseResult? {
        val keyStart = content.indexOf('"', from)
        if (keyStart == -1) return null
        val keyEnd = content.indexOf('"', keyStart + 1)
        if (keyEnd == -1) return null
        val colonIndex = content.indexOf(':', keyEnd + 1)
        if (colonIndex == -1) return null
        return ParseResult(content.substring(keyStart + 1, keyEnd), colonIndex + 1)
    }

    /** Parses the value at [from], dispatching to string or non-string extraction. */
    private fun extractValue(content: String, from: Int): ParseResult? {
        val start = skipSpaces(content, from)
        if (start >= content.length) return null
        return if (content[start] == '"') {
            extractStringValue(content, start)
        } else {
            extractNonStringValue(content, start)
        }
    }

    private fun extractStringValue(content: String, start: Int): ParseResult {
        val closeQuote = findClosingQuote(content, start + 1)
        val value = unescapeJson(content.substring(start + 1, closeQuote))
        return ParseResult(value, closeQuote + 1)
    }

    private fun extractNonStringValue(content: String, start: Int): ParseResult {
        val end = content.indexOfAny(charArrayOf(',', '}'), start)
        val effectiveEnd = if (end == -1) content.length else end
        return ParseResult(content.substring(start, effectiveEnd).trim(), effectiveEnd)
    }

    private fun skipSpaces(content: String, from: Int): Int {
        var pos = from
        while (pos < content.length && content[pos] == ' ') pos++
        return pos
    }

    private fun skipComma(content: String, from: Int): Int =
        if (from < content.length && content[from] == ',') from + 1 else from

    private fun findClosingQuote(str: String, startAfterQuote: Int): Int {
        var j = startAfterQuote
        while (j < str.length) {
            if (str[j] == '"' && (j == 0 || str[j - 1] != '\\')) return j
            j++
        }
        return str.length
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun unescapeJson(value: String): String =
        value.replace("\\\"", "\"").replace("\\\\", "\\")
}
