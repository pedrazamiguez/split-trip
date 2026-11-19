package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CurrenciesDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExchangeRatesDocument
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRate
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates
import java.math.BigDecimal
import java.time.Instant

private val ISO_CURRENCY_CODE_REGEX = Regex("[A-Z]{3}")
private const val DEFAULT_BASE_CURRENCY = "USD"

private val currencySymbols = mapOf(
    "USD" to "$",
    "EUR" to "€",
    "GBP" to "£",
    "JPY" to "¥",
    "CNY" to "¥",
    "CHF" to "CHF",
    "CAD" to "C$",
    "AUD" to "A$",
    "NZD" to "NZ$",
    "INR" to "₹",
    "RUB" to "₽",
    "BRL" to "R$",
    "ZAR" to "R",
    "MXN" to "$",
    "SEK" to "kr",
    "NOK" to "kr",
    "DKK" to "kr",
    "PLN" to "zł",
    "THB" to "฿",
    "IDR" to "Rp",
    "HUF" to "Ft",
    "CZK" to "Kč",
    "ILS" to "₪",
    "CLP" to "$",
    "PHP" to "₱",
    "AED" to "د.إ",
    "COP" to "$",
    "SAR" to "﷼",
    "MYR" to "RM",
    "RON" to "lei",
    "SGD" to "S$",
    "HKD" to "HK$",
    "KRW" to "₩",
    "TRY" to "₺",
    "ARS" to "$",
    "VND" to "₫",
    "UAH" to "₴",
    "BGN" to "лв",
    "HRK" to "kn",
    "ISK" to "kr",
    "NGN" to "₦",
    "EGP" to "E£",
    "PKR" to "₨",
    "QAR" to "﷼",
    "KWD" to "د.ك",
    "BHD" to "د.ب",
    "OMR" to "﷼"
)

private fun getCurrencySymbol(code: String): String = currencySymbols[code] ?: code

private fun getDecimalDigits(code: String): Int = when (code) {
    "JPY", "KRW", "VND", "CLP", "ISK" -> 0
    "BHD", "KWD", "OMR", "TND" -> 3
    else -> 2
}

fun CurrenciesDocument.toDomain(): List<Currency> =
    currencies
        .filter { (code, _) -> ISO_CURRENCY_CODE_REGEX.matches(code) }
        .map { (code, name) ->
            Currency(
                code = code,
                symbol = getCurrencySymbol(code),
                defaultName = name,
                decimalDigits = getDecimalDigits(code)
            )
        }

fun ExchangeRatesDocument.toDomain(): ExchangeRates {
    val baseCode = base.takeIf { ISO_CURRENCY_CODE_REGEX.matches(it) } ?: DEFAULT_BASE_CURRENCY
    val baseCurrency = Currency(
        code = baseCode,
        symbol = getCurrencySymbol(baseCode),
        defaultName = baseCode,
        decimalDigits = getDecimalDigits(baseCode)
    )

    val exchangeRates = rates.mapNotNull { (code, rawValue) ->
        if (!ISO_CURRENCY_CODE_REGEX.matches(code)) {
            return@mapNotNull null
        }

        val parsedRate = rawValue.toBigDecimalOrNull()
        if (parsedRate == null || parsedRate < BigDecimal.ZERO) {
            return@mapNotNull null
        }

        ExchangeRate(
            currency = Currency(
                code = code,
                symbol = getCurrencySymbol(code),
                defaultName = code,
                decimalDigits = getDecimalDigits(code)
            ),
            rate = parsedRate
        )
    }

    val resolvedLastUpdated = when {
        updatedAt != null -> Instant.ofEpochSecond(updatedAt.seconds)
        timestamp > 0L -> Instant.ofEpochSecond(timestamp)
        else -> Instant.EPOCH
    }

    return ExchangeRates(
        baseCurrency = baseCurrency,
        exchangeRates = exchangeRates,
        lastUpdated = resolvedLastUpdated
    )
}
