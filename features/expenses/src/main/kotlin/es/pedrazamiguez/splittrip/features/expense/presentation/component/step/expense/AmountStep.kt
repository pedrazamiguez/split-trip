package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.AmountCurrencyCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.AmountCurrencyCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.CashTrancheFundedFromSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 3: Amount + Currency.
 * Uses the shared [AmountCurrencyCard] component for consistency
 * with other wizard flows (e.g. cash-withdrawal, contributions).
 *
 * When the payment method is CASH and the expense uses the group's default currency
 * (no exchange-rate step), the "Funded from" tranche breakdown is shown here instead,
 * so the user always sees which withdrawal(s) fund their cash expense regardless of
 * currency setup.
 */
@Composable
fun AmountStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        AmountCurrencyCard(
            state = AmountCurrencyCardState(
                amount = uiState.sourceAmount,
                isAmountError = !uiState.isAmountValid,
                selectedCurrency = uiState.selectedCurrency,
                availableCurrencies = uiState.availableCurrencies,
                amountLabel = stringResource(R.string.add_expense_amount_paid),
                currencyLabel = stringResource(R.string.add_expense_currency_label),
                autoFocus = true
            ),
            onAmountChanged = { onEvent(AddExpenseUiEvent.SourceAmountChanged(it)) },
            onCurrencySelected = { onEvent(AddExpenseUiEvent.CurrencySelected(it)) },
            onImeAction = onImeNext
        )

        // Show the "Funded from" breakdown here only when same currency is used.
        // For foreign currency CASH, the breakdown is shown in ExchangeRateStep instead.
        val isSameCurrency = !uiState.showExchangeRateSection
        if (isSameCurrency && uiState.isInsufficientCash) {
            Spacer(modifier = Modifier.height(16.dp))
            FormErrorBanner(error = UiText.StringResource(R.string.add_expense_cash_insufficient_hint))
        } else if (isSameCurrency && uiState.cashTranchePreviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            CashTrancheFundedFromSection(tranches = uiState.cashTranchePreviews)
        }
    }
}
