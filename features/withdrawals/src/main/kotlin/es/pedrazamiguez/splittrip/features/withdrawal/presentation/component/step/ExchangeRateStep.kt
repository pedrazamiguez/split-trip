package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState

/**
 * Step 2: Exchange rate + deducted amount in group currency.
 * Only shown when a foreign currency is selected.
 */
@Composable
fun ExchangeRateStep(
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        CurrencyConversionCard(
            state = CurrencyConversionCardState(
                title = stringResource(R.string.withdrawal_exchange_rate_title),
                exchangeRateValue = uiState.displayExchangeRate,
                exchangeRateLabel = uiState.exchangeRateLabel,
                groupAmountValue = uiState.deductedAmount,
                groupAmountLabel = uiState.deductedAmountLabel,
                isLoadingRate = uiState.isLoadingRate,
                isExchangeRateLocked = false,
                isExchangeRateStale = uiState.isExchangeRateStale,
                isExchangeRateError = uiState.isExchangeRateError,
                autoFocus = true
            ),
            onExchangeRateChanged = { onEvent(AddCashWithdrawalUiEvent.ExchangeRateChanged(it)) },
            onGroupAmountChanged = { onEvent(AddCashWithdrawalUiEvent.DeductedAmountChanged(it)) },
            onDone = onImeNext
        )
    }
}
