package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

/**
 * Immutable state holder for [CurrencyConversionCard].
 *
 * Bundles all display-only fields so the composable signature stays concise
 * and callers can construct the state from their own model (e.g.,
 * `AddExpenseUiState` or `AddCashWithdrawalUiState`).
 *
 * @param title               Localised card title (e.g. "Currency conversion").
 * @param exchangeRateValue   Current exchange-rate text shown in the rate field.
 * @param exchangeRateLabel   Label for the rate field (e.g. "1 EUR = X THB").
 * @param groupAmountValue    Current group-amount text shown in the amount field.
 * @param groupAmountLabel    Label for the amount field (e.g. "Cost in EUR").
 * @param isLoadingRate       Whether a rate-fetch spinner should be shown.
 * @param isExchangeRateLocked When true both fields become read-only.
 * @param exchangeRateLockedHint Optional hint explaining why the rate is locked.
 * @param isInsufficientCash  Drives error colouring on the locked hint text.
 * @param isGroupAmountError  Shows error styling on the group-amount field.
 * @param isExchangeRateStale When true, shows a warning that the rate may be outdated.
 * @param autoFocus           If `true`, the exchange-rate field requests focus on first composition.
 */
data class CurrencyConversionCardState(
    val title: String,
    val exchangeRateValue: String,
    val exchangeRateLabel: String,
    val groupAmountValue: String,
    val groupAmountLabel: String,
    val isLoadingRate: Boolean,
    val isExchangeRateLocked: Boolean,
    val exchangeRateLockedHint: UiText? = null,
    val isInsufficientCash: Boolean = false,
    val isGroupAmountError: Boolean = false,
    val isExchangeRateStale: Boolean = false,
    val isExchangeRateError: Boolean = false,
    val autoFocus: Boolean = false
)
