package es.pedrazamiguez.splittrip.features.expense.presentation.component

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
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.AddOnsStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.AmountStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.CategoryStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ContributionScopeStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ExchangeRateStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.FundingSourceStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.PaymentMethodStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.PaymentStatusStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ReceiptStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ReviewStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.SplitStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.TitleStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.VendorNotesStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun WizardStepContent(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        modifier = modifier,
        transitionSpec = {
            val direction = if (initialState == AddExpenseStep.RECEIPT && targetState == AddExpenseStep.TITLE) {
                1
            } else if (initialState == AddExpenseStep.TITLE && targetState == AddExpenseStep.RECEIPT) {
                -1
            } else {
                val steps = uiState.applicableSteps
                val initialIndex = steps.indexOf(initialState)
                val targetIndex = steps.indexOf(targetState)
                if (targetIndex > initialIndex) 1 else -1
            }
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
            val nextStep = { onEvent(AddExpenseUiEvent.NextStep) }
            when (step) {
                AddExpenseStep.TITLE -> TitleStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.PAYMENT_METHOD -> PaymentMethodStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.FUNDING_SOURCE -> FundingSourceStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.CONTRIBUTION_SCOPE -> ContributionScopeStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.AMOUNT -> AmountStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.EXCHANGE_RATE -> ExchangeRateStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.SPLIT -> SplitStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.CATEGORY -> CategoryStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.VENDOR_NOTES -> VendorNotesStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.PAYMENT_STATUS -> PaymentStatusStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.RECEIPT -> ReceiptStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.ADD_ONS -> AddOnsStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.REVIEW -> ReviewStep(uiState = uiState)
            }
        }
    }
}
