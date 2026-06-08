package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
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

@Composable
internal fun WizardStepContent(
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
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
        val bottomPadding = LocalBottomPadding.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + UiConstants.WIZARD_NAV_BAR_HEIGHT)
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
