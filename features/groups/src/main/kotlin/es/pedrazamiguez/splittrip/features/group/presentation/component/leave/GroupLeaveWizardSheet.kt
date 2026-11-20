package es.pedrazamiguez.splittrip.features.group.presentation.component.leave

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.HorizonModalBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.leave.LeaveBalanceSummaryStep
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.leave.LeaveConfirmationStep
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardStep
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardUiState

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupLeaveWizardSheet(
    groupName: String,
    leaveWizardState: LeaveWizardUiState,
    onNextClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onDismissRequest: () -> Unit,

    onConfirmLeave: () -> Unit,
    onGoToSettlementsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizonModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        val activeSteps = leaveWizardState.activeSteps
        val currentStepIndex = activeSteps.indexOf(leaveWizardState.currentStep).coerceAtLeast(0)
        val isOnLastStep = leaveWizardState.currentStep == activeSteps.lastOrNull()

        val isCurrentStepValid = when (leaveWizardState.currentStep) {
            LeaveWizardStep.CONFIRMATION -> !leaveWizardState.hasUnresolvedSettlements
            else -> true
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (activeSteps.isNotEmpty()) {
                WizardStepIndicator(
                    stepLabels = activeSteps.map { stringResource(it.labelResId) },
                    currentStepIndex = currentStepIndex
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                when (leaveWizardState.currentStep) {
                    LeaveWizardStep.BALANCE_SUMMARY -> {
                        LeaveBalanceSummaryStep(
                            balanceSummary = leaveWizardState.balanceSummary,
                            modifier = Modifier.padding(bottom = UiConstants.WIZARD_NAV_BAR_HEIGHT)
                        )
                    }
                    LeaveWizardStep.CONFIRMATION -> {
                        LeaveConfirmationStep(
                            groupName = groupName,
                            subunitImpact = leaveWizardState.subunitImpact,
                            hasUnresolvedSettlements = leaveWizardState.hasUnresolvedSettlements,
                            onGoToSettlementsClicked = onGoToSettlementsClicked,
                            modifier = Modifier.padding(bottom = UiConstants.WIZARD_NAV_BAR_HEIGHT)
                        )
                    }
                }
            }

            WizardNavigationBar(
                config = WizardNavigationBarConfig(
                    canGoNext = isCurrentStepValid,
                    isOnLastStep = isOnLastStep,
                    isCurrentStepValid = isCurrentStepValid,
                    isLoading = leaveWizardState.isLoading,
                    backLabel = stringResource(R.string.group_wizard_back),
                    nextLabel = stringResource(R.string.group_wizard_next),
                    submitLabel = stringResource(R.string.leave_wizard_submit_button)
                ),
                onBack = onBackClicked,
                onNext = onNextClicked,
                onSubmit = onConfirmLeave,
                applyWindowInsets = false
            )
        }
    }
}
