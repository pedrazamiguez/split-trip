package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.CashTrancheFundedFromSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.WithdrawalPoolSelectorSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 4: Exchange rate + calculated group amount.
 * Only shown when a foreign currency is selected.
 *
 * When the payment method is CASH and a positive amount has been entered, a "Funded from"
 * section is shown below the conversion card, listing the ATM withdrawal tranche(s) that
 * will cover this expense. The section title ("Funded from") is rendered outside the card,
 * matching the visual style of the "Currency conversion" header above.
 */
@Composable
fun ExchangeRateStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        CurrencyConversionCard(
            state = CurrencyConversionCardState(
                title = stringResource(R.string.add_expense_exchange_rate_title),
                exchangeRateValue = uiState.displayExchangeRate,
                exchangeRateLabel = uiState.exchangeRateLabel,
                groupAmountValue = uiState.calculatedGroupAmount,
                groupAmountLabel = uiState.groupAmountLabel,
                isLoadingRate = uiState.isLoadingRate,
                isExchangeRateLocked = uiState.isExchangeRateLocked,
                exchangeRateLockedHint = uiState.exchangeRateLockedHint,
                isInsufficientCash = uiState.isInsufficientCash,
                isGroupAmountError = !uiState.isAmountValid,
                isExchangeRateStale = uiState.isExchangeRateStale,
                isExchangeRateError = uiState.isExchangeRateError,
                autoFocus = true
            ),
            onExchangeRateChanged = { onEvent(AddExpenseUiEvent.ExchangeRateChanged(it)) },
            onGroupAmountChanged = { onEvent(AddExpenseUiEvent.GroupAmountChanged(it)) },
            onDone = onImeNext
        )

        if (uiState.availableWithdrawalPools.size > 1) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
            WithdrawalPoolSelectorSection(
                pools = uiState.availableWithdrawalPools,
                selectedPool = uiState.selectedWithdrawalPool,
                onPoolSelected = { scope, scopeOwnerId ->
                    onEvent(AddExpenseUiEvent.WithdrawalPoolSelected(scope, scopeOwnerId))
                }
            )
        }

        if (uiState.cashTranchePreviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
            CashTrancheFundedFromSection(tranches = uiState.cashTranchePreviews)
        }
    }
}
