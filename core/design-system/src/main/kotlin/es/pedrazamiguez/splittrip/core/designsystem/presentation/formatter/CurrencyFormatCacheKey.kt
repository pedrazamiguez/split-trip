package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import java.util.Locale

internal data class CurrencyFormatCacheKey(
    val locale: Locale,
    val currencyCode: String
)
