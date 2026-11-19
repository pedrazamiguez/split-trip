package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import java.util.Locale

internal data class PatternLocaleCacheKey(
    val pattern: String,
    val locale: Locale
)
