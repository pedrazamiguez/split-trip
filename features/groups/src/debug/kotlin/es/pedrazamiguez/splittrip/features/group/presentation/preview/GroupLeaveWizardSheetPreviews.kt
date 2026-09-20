package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.group.presentation.component.leave.GroupLeaveWizardSheet
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveBalanceSummaryUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveSubunitImpactUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardStep
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardUiState
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.NetPositionUiModel
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun GroupLeaveWizardSheetUnresolvedPreview() {
    PreviewThemeWrapper {
        GroupLeaveWizardSheet(
            groupName = "Summer Trip",
            leaveWizardState = LeaveWizardUiState(
                showSheet = true,
                currentStep = LeaveWizardStep.BALANCE_SUMMARY,
                activeSteps = persistentListOf(
                    LeaveWizardStep.BALANCE_SUMMARY,
                    LeaveWizardStep.CONFIRMATION
                ),
                balanceSummary = LeaveBalanceSummaryUiModel(
                    pocketBalanceFormatted = "12.50 €",
                    cashInHandFormatted = "0.00 €",
                    totalBalanceFormatted = "12.50 €",
                    perPersonNetPositions = persistentListOf(
                        NetPositionUiModel(
                            memberName = "María López",
                            amountFormatted = "12.50 €",
                            isPositive = true,
                            isNegative = false
                        )
                    )
                ),
                hasUnresolvedSettlements = true
            ),
            onNextClicked = {},
            onBackClicked = {},
            onDismissRequest = {},
            onConfirmLeave = {},
            onGoToSettlementsClicked = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupLeaveWizardSheetSettledPreview() {
    PreviewThemeWrapper {
        GroupLeaveWizardSheet(
            groupName = "Summer Trip",
            leaveWizardState = LeaveWizardUiState(
                showSheet = true,
                currentStep = LeaveWizardStep.BALANCE_SUMMARY,
                activeSteps = persistentListOf(
                    LeaveWizardStep.BALANCE_SUMMARY,
                    LeaveWizardStep.CONFIRMATION
                ),
                balanceSummary = LeaveBalanceSummaryUiModel(
                    pocketBalanceFormatted = "0.00 €",
                    cashInHandFormatted = "0.00 €",
                    totalBalanceFormatted = "0.00 €",
                    perPersonNetPositions = persistentListOf()
                ),
                hasUnresolvedSettlements = false
            ),
            onNextClicked = {},
            onBackClicked = {},
            onDismissRequest = {},
            onConfirmLeave = {},
            onGoToSettlementsClicked = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupLeaveWizardSheetConfirmationStepPreview() {
    PreviewThemeWrapper {
        GroupLeaveWizardSheet(
            groupName = "Summer Trip",
            leaveWizardState = LeaveWizardUiState(
                showSheet = true,
                currentStep = LeaveWizardStep.CONFIRMATION,
                activeSteps = persistentListOf(
                    LeaveWizardStep.BALANCE_SUMMARY,
                    LeaveWizardStep.CONFIRMATION
                ),
                subunitImpact = LeaveSubunitImpactUiModel(
                    hasSubunitImpact = true,
                    affectedSubunitNames = persistentListOf("Food & Drinks"),
                    message = "You are a member of 1 subunit."
                ),
                hasUnresolvedSettlements = false
            ),
            onNextClicked = {},
            onBackClicked = {},
            onDismissRequest = {},
            onConfirmLeave = {},
            onGoToSettlementsClicked = {}
        )
    }
}
