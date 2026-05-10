package es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
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

@Composable
fun AddCashWithdrawalScreen(
    groupId: String? = null,
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit = {}
) {
    LaunchedEffect(groupId) {
        onEvent(AddCashWithdrawalUiEvent.LoadGroupConfig(groupId))
    }

    SharedTransitionSurface(sharedElementKey = SharedElementKeys.ADD_CASH_WITHDRAWAL) {
        when {
            uiState.isReady -> {
                WithdrawalWizard(
                    groupId = groupId,
                    uiState = uiState,
                    onEvent = onEvent
                )
            }

            uiState.configLoadFailed -> {
                WithdrawalConfigLoadFailedContent(
                    onRetry = { onEvent(AddCashWithdrawalUiEvent.RetryLoadConfig(groupId)) }
                )
            }

            else -> {
                ShimmerLoadingList()
            }
        }
    }
}

@Composable
private fun WithdrawalWizard(
    groupId: String?,
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val stepLabelMap = rememberStepLabelMap()
    // Build an ordered list of labels that matches the current applicable steps exactly.
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

@Composable
private fun WizardStepContent(
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

@Composable
private fun WithdrawalConfigLoadFailedContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Refresh,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
        Text(
            text = stringResource(R.string.withdrawal_error_load_config),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
        SecondaryButton(
            text = stringResource(R.string.withdrawal_retry),
            onClick = onRetry,
            leadingIcon = TablerIcons.Outline.Refresh
        )
    }
}

/**
 * Creates a remembered map of [CashWithdrawalStep] → localised label.
 *
 * Using a map (not a list) here keeps the table readable and ensures that
 * [WithdrawalWizard] can derive the correctly-ordered list from the dynamic
 * [AddCashWithdrawalUiState.applicableSteps] at composition time.
 */
@Composable
private fun rememberStepLabelMap(): Map<CashWithdrawalStep, String> {
    val amountLabel = stringResource(R.string.withdrawal_wizard_step_amount)
    val rateLabel = stringResource(R.string.withdrawal_wizard_step_exchange_rate)
    val scopeLabel = stringResource(R.string.withdrawal_wizard_step_scope)
    val detailsLabel = stringResource(R.string.withdrawal_wizard_step_details)
    val feeLabel = stringResource(R.string.withdrawal_wizard_step_atm_fee)
    val feeRateLabel = stringResource(R.string.withdrawal_wizard_step_fee_rate)
    val reviewLabel = stringResource(R.string.withdrawal_wizard_step_review)

    return remember(amountLabel, rateLabel, scopeLabel, detailsLabel, feeLabel, feeRateLabel, reviewLabel) {
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
}
