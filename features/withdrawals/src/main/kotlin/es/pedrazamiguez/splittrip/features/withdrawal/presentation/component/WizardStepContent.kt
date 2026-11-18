package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardFastTrackBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardScrollableStepContainer
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.AmountStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.AtmFeeStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.DetailsStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.ExchangeRateStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.FeeExchangeRateStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.ReviewStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step.ScopeStep
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.CashWithdrawalStep

@Suppress("LongMethod")
@Composable
internal fun WizardStepContent(
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val skipToReviewLabel = stringResource(R.string.withdrawal_wizard_skip_to_review)

    AnimatedContent(
        targetState = uiState.currentStep,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn())
                .togetherWith(
                    slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut()
                )
                .using(SizeTransform(clip = false))
        },
        label = "wizardStep"
    ) { step ->
        WizardScrollableStepContainer(
            fastTrackBanner = {
                AnimatedVisibility(
                    visible = uiState.isOnOptionalStep && !uiState.isOnReviewStep,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    WizardFastTrackBanner(
                        label = skipToReviewLabel,
                        enabled = !uiState.isLoading,
                        onSkipToReview = { onEvent(AddCashWithdrawalUiEvent.JumpToReview) }
                    )
                }
            }
        ) {
            val nextStep = { onEvent(AddCashWithdrawalUiEvent.NextStep) }
            when (step) {
                CashWithdrawalStep.AMOUNT -> AmountStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CashWithdrawalStep.EXCHANGE_RATE -> ExchangeRateStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CashWithdrawalStep.SCOPE -> ScopeStep(uiState = uiState, onEvent = onEvent)
                CashWithdrawalStep.DETAILS -> DetailsStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CashWithdrawalStep.ATM_FEE -> AtmFeeStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CashWithdrawalStep.FEE_EXCHANGE_RATE -> FeeExchangeRateStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CashWithdrawalStep.REVIEW -> ReviewStep(uiState = uiState)
            }
        }
    }
}
