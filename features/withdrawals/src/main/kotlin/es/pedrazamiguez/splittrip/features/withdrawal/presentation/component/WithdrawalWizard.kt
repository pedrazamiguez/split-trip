package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.CashWithdrawalStep

@Suppress("LongMethod")
@Composable
internal fun WithdrawalWizard(
    groupId: String?,
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val amountLabel = stringResource(R.string.withdrawal_wizard_step_amount)
    val rateLabel = stringResource(R.string.withdrawal_wizard_step_exchange_rate)
    val scopeLabel = stringResource(R.string.withdrawal_wizard_step_scope)
    val detailsLabel = stringResource(R.string.withdrawal_wizard_step_details)
    val feeLabel = stringResource(R.string.withdrawal_wizard_step_atm_fee)
    val feeRateLabel = stringResource(R.string.withdrawal_wizard_step_fee_rate)
    val reviewLabel = stringResource(R.string.withdrawal_wizard_step_review)

    val stepLabelMap = remember(amountLabel, rateLabel, scopeLabel, detailsLabel, feeLabel, feeRateLabel, reviewLabel) {
        mapOf(
            CashWithdrawalStep.AMOUNT to amountLabel,
            CashWithdrawalStep.EXCHANGE_RATE to rateLabel,
            CashWithdrawalStep.SCOPE to scopeLabel,
            CashWithdrawalStep.DETAILS to detailsLabel,
            CashWithdrawalStep.ATM_FEE to feeLabel,
            CashWithdrawalStep.FEE_EXCHANGE_RATE to feeRateLabel,
            CashWithdrawalStep.REVIEW to reviewLabel
        )
    }

    val orderedLabels = remember(uiState.applicableSteps, stepLabelMap) {
        uiState.applicableSteps.map { stepLabelMap[it] ?: "" }
    }

    val backLabel = stringResource(R.string.withdrawal_wizard_back)
    val nextLabel = stringResource(R.string.withdrawal_wizard_next)
    val submitLabel = stringResource(R.string.withdrawal_cash_submit)
    val skipToReviewLabel = stringResource(R.string.withdrawal_wizard_skip_to_review)

    val bottomPadding = LocalBottomPadding.current

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WizardStepIndicator(
                stepLabels = orderedLabels,
                currentStepIndex = uiState.currentStepIndex,
                optionalStepIndices = uiState.optionalStepIndices,
                skipToReviewLabel = if (uiState.isOnOptionalStep) skipToReviewLabel else null,
                onSkipToReview = if (uiState.isOnOptionalStep) {
                    { onEvent(AddCashWithdrawalUiEvent.JumpToReview) }
                } else {
                    null
                },
                onStepClicked = { onEvent(AddCashWithdrawalUiEvent.JumpToStep(it)) }
            )

            WizardStepContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.weight(1f)
            )
        }

        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = uiState.canGoNext,
                isOnLastStep = uiState.isOnReviewStep,
                isCurrentStepValid = uiState.isCurrentStepValid,
                isLoading = uiState.isLoading,
                backLabel = backLabel,
                nextLabel = nextLabel,
                submitLabel = submitLabel
            ),
            onBack = { onEvent(AddCashWithdrawalUiEvent.PreviousStep) },
            onNext = { onEvent(AddCashWithdrawalUiEvent.NextStep) },
            onSubmit = { onEvent(AddCashWithdrawalUiEvent.SubmitWithdrawal(groupId)) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )
    }
}
