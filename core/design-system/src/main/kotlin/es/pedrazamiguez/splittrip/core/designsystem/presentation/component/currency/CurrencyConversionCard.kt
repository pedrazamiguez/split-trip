package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.InlineWarningBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.rememberAutoFocusRequester
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText

private const val EXCHANGE_RATE_FIELD_WEIGHT = 0.6f
private const val GROUP_AMOUNT_FIELD_WEIGHT = 0.4f

/**
 * Reusable currency-conversion section that displays an exchange rate input,
 * a calculated group-amount input, an optional loading indicator, and an
 * optional locked-rate hint.
 *
 * Renders as a plain [Column] — no card wrapper — so it blends seamlessly
 * on any step background (wizard surface, add-on editor, etc.).
 * The focus manager is resolved internally via [LocalFocusManager].
 *
 * @param state               Immutable display state for the component.
 * @param onExchangeRateChanged Called when the user edits the rate field.
 * @param onGroupAmountChanged  Called when the user edits the group-amount field.
 * @param onDone              Optional callback invoked when the keyboard Done action
 *                            fires on the last field. Triggered after clearing focus.
 * @param modifier            Outer modifier applied to the root [Column].
 */
@Composable
fun CurrencyConversionCard(
    state: CurrencyConversionCardState,
    onExchangeRateChanged: (String) -> Unit,
    onGroupAmountChanged: (String) -> Unit,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = rememberAutoFocusRequester(state.autoFocus)

    Column(modifier = modifier.fillMaxWidth()) {
        ConversionCardTitleRow(
            title = state.title,
            isLoadingRate = state.isLoadingRate
        )
        Spacer(Modifier.height(MaterialTheme.spacing.Medium))
        ConversionCardInputRow(
            state = state,
            onExchangeRateChanged = onExchangeRateChanged,
            onGroupAmountChanged = onGroupAmountChanged,
            onDone = {
                focusManager.clearFocus()
                onDone?.invoke()
            },
            focusRequester = if (state.autoFocus) focusRequester else null,
            moveCursorToEndOnFocus = state.autoFocus
        )
        ConversionCardLockedHint(
            exchangeRateLockedHint = state.exchangeRateLockedHint,
            isInsufficientCash = state.isInsufficientCash
        )
        val staleRateWarning = if (state.isExchangeRateStale) {
            UiText.StringResource(R.string.stale_rate_warning)
        } else {
            null
        }
        // Top padding on the AnimatedVisibility container animates in/out with the banner,
        // matching the 8 dp gap the former StaleRateBanner Spacer provided.
        InlineWarningBanner(
            warning = staleRateWarning,
            modifier = Modifier.padding(top = MaterialTheme.spacing.Small)
        )
    }
}

@Composable
private fun ConversionCardTitleRow(
    title: String,
    isLoadingRate: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        CardSectionLabelText(text = title)
        if (isLoadingRate) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConversionCardInputRow(
    state: CurrencyConversionCardState,
    onExchangeRateChanged: (String) -> Unit,
    onGroupAmountChanged: (String) -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester? = null,
    moveCursorToEndOnFocus: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        StyledOutlinedTextField(
            value = state.exchangeRateValue,
            onValueChange = onExchangeRateChanged,
            label = state.exchangeRateLabel,
            modifier = Modifier.weight(EXCHANGE_RATE_FIELD_WEIGHT),
            readOnly = state.isExchangeRateLocked,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            focusRequester = focusRequester,
            moveCursorToEndOnFocus = moveCursorToEndOnFocus
        )
        StyledOutlinedTextField(
            value = state.groupAmountValue,
            onValueChange = onGroupAmountChanged,
            label = state.groupAmountLabel,
            modifier = Modifier.weight(GROUP_AMOUNT_FIELD_WEIGHT),
            readOnly = state.isExchangeRateLocked,
            keyboardType = KeyboardType.Decimal,
            isError = state.isGroupAmountError,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            )
        )
    }
}

@Composable
private fun ConversionCardLockedHint(
    exchangeRateLockedHint: UiText?,
    isInsufficientCash: Boolean
) {
    exchangeRateLockedHint?.let { hint ->
        Spacer(Modifier.height(MaterialTheme.spacing.Small))
        Text(
            text = hint.asString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isInsufficientCash) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
