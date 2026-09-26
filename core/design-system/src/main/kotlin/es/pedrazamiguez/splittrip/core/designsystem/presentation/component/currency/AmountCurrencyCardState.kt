package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Immutable state holder for [AmountCurrencyCard].
 *
 * Bundles all display-only fields so the composable signature stays concise
 * and callers can construct the state from their own UI state model
 * (e.g., `AddCashWithdrawalUiState`, `AddContributionUiState`).
 *
 * @param amount              Current amount text.
 * @param isAmountError       Whether the amount field should show error styling.
 * @param selectedCurrency    Currently selected [CurrencyUiModel].
 * @param availableCurrencies All available currencies for the dropdown.
 * @param amountLabel         Localised hint label for the amount field.
 * @param currencyLabel       Localised hint label for the currency dropdown.
 * @param title               Optional card title shown above the fields.
 * @param autoFocus           If `true`, the amount field requests focus on first composition.
 * @param displayAmount       Optional custom display-formatted amount text for unfocused state.
 */
data class AmountCurrencyCardState(
    val amount: String,
    val isAmountError: Boolean,
    val selectedCurrency: CurrencyUiModel?,
    val availableCurrencies: ImmutableList<CurrencyUiModel>,
    val amountLabel: String,
    val currencyLabel: String,
    val title: String? = null,
    val autoFocus: Boolean = false,
    val displayAmount: String? = null
)
