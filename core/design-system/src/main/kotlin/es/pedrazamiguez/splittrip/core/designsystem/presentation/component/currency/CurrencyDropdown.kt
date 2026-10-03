package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable currency selector dropdown backed by [CurrencySelectorField].
 *
 * @deprecated Use [CurrencySelectorField] instead. Currency selection is now presented
 * via [CurrencyPickerBottomSheet].
 *
 * @param selectedCurrency    Currently selected currency (may be `null` when nothing is chosen yet).
 * @param availableCurrencies Full list of selectable currencies.
 * @param onCurrencySelected  Called with the ISO 4217 [CurrencyUiModel.code] of the picked currency.
 * @param label               Localised hint label for the text field.
 * @param modifier            Outer modifier applied to the text field.
 * @param isLoading           When `true`, shows a loading spinner instead of the arrow icon and
 *                            prevents the bottom sheet from opening. Defaults to `false`.
 */
@Deprecated(
    message = "Use CurrencySelectorField instead. Currency selection is now presented via CurrencyPickerBottomSheet.",
    replaceWith = ReplaceWith(
        expression = "CurrencySelectorField(" +
            "selectedCurrency, availableCurrencies, onCurrencySelected, label, modifier, isLoading" +
            ")"
    )
)
@Composable
fun CurrencyDropdown(
    selectedCurrency: CurrencyUiModel?,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    onCurrencySelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    CurrencySelectorField(
        selectedCurrency = selectedCurrency,
        availableCurrencies = availableCurrencies,
        onCurrencySelected = onCurrencySelected,
        label = label,
        modifier = modifier,
        isLoading = isLoading
    )
}
