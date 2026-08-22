package es.pedrazamiguez.splittrip.data.local.converter

import androidx.room.TypeConverter
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Room TypeConverter for `List<SubExpense>`.
 * Stores the list as a JSON string in the database.
 *
 * Uses manual JSON serialization (no `org.json`) to stay compatible with pure JVM unit tests.
 */
class SubExpenseListConverter {

    private val addOnListConverter = AddOnListConverter()

    @TypeConverter
    fun fromSubExpenseList(value: List<SubExpense>?): String? {
        if (value.isNullOrEmpty()) return null
        return value.joinToString(separator = ",", prefix = "[", postfix = "]") { sub ->
            serializeSubExpense(sub, addOnListConverter)
        }
    }

    @TypeConverter
    fun toSubExpenseList(value: String?): List<SubExpense>? {
        if (value.isNullOrBlank()) return null
        val objects = extractJsonObjects(value)
        if (objects.isEmpty()) return null
        return objects.map { parseSubExpense(parseJsonFields(it), addOnListConverter) }
    }
}

private fun serializeSubExpense(sub: SubExpense, addOnConverter: AddOnListConverter): String = buildString {
    append("{")
    append("\"id\":\"${escapeJson(sub.id)}\"")
    append(",\"title\":\"${escapeJson(sub.title)}\"")
    append(",\"amountCents\":${sub.amountCents}")
    append(",\"currency\":\"${escapeJson(sub.currency)}\"")
    append(",\"groupAmountCents\":${sub.groupAmountCents}")
    append(",\"exchangeRate\":\"${sub.exchangeRate.toPlainString()}\"")
    append(",\"paymentMethod\":\"${sub.paymentMethod.name}\"")
    append(",\"paymentStatus\":\"${sub.paymentStatus.name}\"")
    append(",\"payerType\":\"${sub.payerType.name}\"")
    sub.payerId?.let { append(",\"payerId\":\"${escapeJson(it)}\"") }
    sub.dueDate?.let { append(",\"dueDate\":\"$it\"") }
    sub.operationDate?.let { append(",\"operationDate\":\"$it\"") }
    sub.notes?.let { append(",\"notes\":\"${escapeJson(it)}\"") }
    addOnConverter.fromAddOnList(sub.addOns)?.let {
        append(",\"addOns\":$it")
    }
    if (sub.cashTranches.isNotEmpty()) {
        append(",\"cashTranches\":[")
        append(
            sub.cashTranches.joinToString(",") { tranche ->
                "{\"withdrawalId\":\"${escapeJson(tranche.withdrawalId)}\"," +
                    "\"amountConsumed\":${tranche.amountConsumed}}"
            }
        )
        append("]")
    }
    append("}")
}

private fun parseSubExpense(fields: Map<String, String>, addOnConverter: AddOnListConverter): SubExpense {
    val addOns = fields["addOns"]?.let { addOnConverter.toAddOnList(it) }.orEmpty()
    val cashTranches = fields["cashTranches"]?.let { parseCashTranches(it) }.orEmpty()

    return SubExpense(
        id = fields["id"].orEmpty(),
        title = fields["title"].orEmpty(),
        amountCents = fields["amountCents"]?.toLongOrNull() ?: 0L,
        currency = fields["currency"] ?: "EUR",
        groupAmountCents = fields["groupAmountCents"]?.toLongOrNull() ?: 0L,
        exchangeRate = fields["exchangeRate"]?.toBigDecimalOrNull() ?: BigDecimal.ONE,
        paymentMethod = parsePaymentMethod(fields["paymentMethod"]),
        paymentStatus = parsePaymentStatus(fields["paymentStatus"]),
        payerType = parsePayerType(fields["payerType"]),
        payerId = fields["payerId"],
        dueDate = parseDateTime(fields["dueDate"]),
        operationDate = parseDateTime(fields["operationDate"]),
        notes = fields["notes"],
        addOns = addOns,
        cashTranches = cashTranches
    )
}

private fun parsePaymentMethod(value: String?): PaymentMethod =
    value?.let { runCatching { PaymentMethod.fromString(it) }.getOrNull() } ?: PaymentMethod.OTHER

private fun parsePaymentStatus(value: String?): PaymentStatus =
    value?.let { runCatching { PaymentStatus.fromString(it) }.getOrNull() } ?: PaymentStatus.FINISHED

private fun parsePayerType(value: String?): PayerType =
    value?.let { runCatching { PayerType.fromString(it) }.getOrNull() } ?: PayerType.GROUP

private fun parseDateTime(value: String?): LocalDateTime? =
    value?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

private fun extractJsonObjects(jsonArrayStr: String): List<String> {
    val inner = jsonArrayStr.trim().removePrefix("[").removeSuffix("]")
    if (inner.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    var depth = 0
    var objectStart = -1
    for (i in inner.indices) {
        if (inner[i] == '{') {
            if (depth == 0) objectStart = i
            depth++
        } else if (inner[i] == '}') {
            depth--
            if (depth == 0 && objectStart >= 0) {
                result.add(inner.substring(objectStart + 1, i))
                objectStart = -1
            }
        }
    }
    return result
}

private fun parseCashTranches(jsonArrayStr: String): List<CashTranche> {
    return extractJsonObjects(jsonArrayStr).mapNotNull { objectStr ->
        val fields = parseJsonFields(objectStr)
        val withdrawalId = fields["withdrawalId"] ?: return@mapNotNull null
        val amountConsumed = fields["amountConsumed"]?.toLongOrNull() ?: return@mapNotNull null
        CashTranche(withdrawalId, amountConsumed)
    }
}

private data class ParseResult(val value: String, val nextIndex: Int)

private fun parseJsonFields(objectContent: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var cursor = 0

    while (cursor < objectContent.length) {
        val (key, value, next) = parseNextField(objectContent, cursor) ?: break
        result[key] = value
        cursor = next
    }
    return result
}

private fun parseNextField(content: String, from: Int): Triple<String, String, Int>? {
    val key = extractKey(content, from) ?: return null
    val value = extractValue(content, key.nextIndex) ?: return null
    val next = if (value.nextIndex < content.length && content[value.nextIndex] == ',') {
        value.nextIndex + 1
    } else {
        value.nextIndex
    }
    return Triple(key.value, value.value, next)
}

private fun extractKey(content: String, from: Int): ParseResult? {
    val keyStart = content.indexOf('"', from)
    if (keyStart == -1) return null
    val keyEnd = content.indexOf('"', keyStart + 1)
    if (keyEnd == -1) return null
    val colonIndex = content.indexOf(':', keyEnd + 1)
    if (colonIndex == -1) return null
    return ParseResult(content.substring(keyStart + 1, keyEnd), colonIndex + 1)
}

private fun extractValue(content: String, from: Int): ParseResult? {
    var start = from
    while (start < content.length && content[start] == ' ') start++
    if (start >= content.length) return null
    return when (content[start]) {
        '"' -> extractStringValue(content, start)
        '[' -> extractDelimitedValue(content, start, '[', ']')
        '{' -> extractDelimitedValue(content, start, '{', '}')
        else -> extractNonStringValue(content, start)
    }
}

private fun extractStringValue(content: String, start: Int): ParseResult {
    var j = start + 1
    while (j < content.length) {
        if (content[j] == '"' && content[j - 1] != '\\') break
        j++
    }
    val closeQuote = if (j < content.length) j else content.length
    val value = unescapeJson(content.substring(start + 1, closeQuote))
    return ParseResult(value, closeQuote + 1)
}

private fun extractDelimitedValue(content: String, start: Int, openChar: Char, closeChar: Char): ParseResult {
    var depth = 0
    for (i in start until content.length) {
        if (content[i] == openChar) {
            depth++
        } else if (content[i] == closeChar) {
            depth--
            if (depth == 0) {
                return ParseResult(content.substring(start, i + 1), i + 1)
            }
        }
    }
    return ParseResult(content.substring(start), content.length)
}

private fun extractNonStringValue(content: String, start: Int): ParseResult {
    val end = content.indexOfAny(charArrayOf(',', '}'), start)
    val effectiveEnd = if (end == -1) content.length else end
    return ParseResult(content.substring(start, effectiveEnd).trim(), effectiveEnd)
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private fun unescapeJson(value: String): String =
    value.replace("\\\"", "\"").replace("\\\\", "\\")
