package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.CaretDownFilled
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable currency selector field backed by [StyledOutlinedTextField] in read-only mode.
 *
 * Shows the currently selected currency's [CurrencyUiModel.displayText] and opens a
 * [CurrencyPickerBottomSheet] on tap. Emits the selected ISO 4217 code via [onCurrencySelected].
 *
 * @param selectedCurrency    Currently selected currency (may be `null` when nothing is chosen yet).
 * @param availableCurrencies Full list of selectable currencies.
 * @param onCurrencySelected  Called with the ISO 4217 [CurrencyUiModel.code] of the picked currency.
 * @param label               Localised hint label for the text field.
 * @param modifier            Outer modifier applied to the text field.
 * @param isLoading           When `true`, shows a loading spinner instead of the caret icon and
 *                            prevents the bottom sheet from opening. Defaults to `false`.
 * @param enabled             Whether the selector field is interactive. Defaults to `true`.
 */
@Composable
fun CurrencySelectorField(
    selectedCurrency: CurrencyUiModel?,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    onCurrencySelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    StyledOutlinedTextField(
        value = selectedCurrency?.displayText ?: "",
        onValueChange = {},
        readOnly = true,
        focusable = false,
        enabled = enabled,
        label = label,
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Icon(TablerIcons.Filled.CaretDownFilled, null)
            }
        },
        onClick = {
            if (!isLoading && enabled) {
                focusManager.clearFocus()
                isSheetOpen = true
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (isSheetOpen) {
        CurrencyPickerBottomSheet(
            selectedCurrency = selectedCurrency,
            availableCurrencies = availableCurrencies,
            onCurrencySelected = onCurrencySelected,
            onDismiss = { isSheetOpen = false }
        )
    }
}
