package es.pedrazamiguez.splittrip.core.designsystem.presentation.model

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.AmountCurrencyCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyPickerBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencySelectorField

/**
 * Presentation model for a selectable currency option.
 *
 * Shared across features (expenses, balances) and consumed by common UI components
 * such as [CurrencySelectorField], [CurrencyPickerBottomSheet], and [AmountCurrencyCard].
 *
 * @param code          ISO 4217 currency code (e.g. "EUR", "THB").
 * @param displayText   Human-readable label shown in the UI (e.g. "EUR (€)").
 * @param decimalDigits Number of decimal places for amounts in this currency.
 * @param defaultName   Full English/API fallback name of the currency (e.g. "Euro"). Used for
 *                      fallback display and name-based search matching when a localized value is
 *                      unavailable. Defaults to empty string.
 * @param localizedName Preferred locale-aware name of the currency (e.g. "Libra esterlina" for
 *                      GBP in Spanish). Resolved via string resources for known currencies; falls
 *                      back to [defaultName] for currencies not in the domain enum. Used as the
 *                      preferred secondary display and localized search name when available.
 *                      Defaults to empty string.
 */
data class CurrencyUiModel(
    val code: String,
    val displayText: String,
    val decimalDigits: Int,
    val defaultName: String = "",
    val localizedName: String = ""
)
