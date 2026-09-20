package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.step.archive.ArchiveConfirmationStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.step.archive.ArchiveSummaryStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowStatusStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.archive.ArchiveWizardStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.GroupSettlementOverviewScreen
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.GroupSettlementOverviewUiState
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_SETTLEMENT_ROW = SettlementRowUiModel(
    settlementId = "s-1",
    debtorId = "user-1",
    creditorId = "user-2",
    debtorName = "Antonio",
    creditorName = "María",
    directionTitle = "Antonio owes María",
    formattedAmount = "25.00 €",
    isCurrentUserDebtor = true,
    isCurrentUserCreditor = false,
    pocketTypeLabel = "Pocket",
    currencyCode = "EUR",
    statusLabel = "Pending",
    statusChipStyle = SettlementRowStatusStyle.WARNING,
    canCurrentUserConfirm = true,
    canCurrentUserDispute = true,
    disputedByCurrentUser = false,
    status = SettlementStatus.SUGGESTED
)

@PreviewComplete
@Composable
private fun GroupSettlementOverviewScreenLoadingPreview() {
    PreviewThemeWrapper {
        GroupSettlementOverviewScreen(
            uiState = GroupSettlementOverviewUiState(isLoading = true),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupSettlementOverviewScreenErrorPreview() {
    PreviewThemeWrapper {
        GroupSettlementOverviewScreen(
            uiState = GroupSettlementOverviewUiState(isLoading = false, hasError = true),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupSettlementOverviewScreenCreatorSummaryStepPreview() {
    PreviewThemeWrapper {
        GroupSettlementOverviewScreen(
            uiState = GroupSettlementOverviewUiState(
                isLoading = false,
                isUserCreator = true,
                currentStep = ArchiveWizardStep.SETTLEMENT_SUMMARY,
                activeSteps = persistentListOf(
                    ArchiveWizardStep.SETTLEMENT_SUMMARY,
                    ArchiveWizardStep.CONFIRMATION
                ),
                actionRequiredCount = 1,
                waitingOnOthersCount = 1,
                disputedCount = 1,
                areAllSettlementsResolved = false
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupSettlementOverviewScreenCreatorConfirmationStepPreview() {
    PreviewThemeWrapper {
        GroupSettlementOverviewScreen(
            uiState = GroupSettlementOverviewUiState(
                isLoading = false,
                isUserCreator = true,
                currentStep = ArchiveWizardStep.CONFIRMATION,
                activeSteps = persistentListOf(
                    ArchiveWizardStep.SETTLEMENT_SUMMARY,
                    ArchiveWizardStep.CONFIRMATION
                ),
                groupName = "Summer Trip",
                areAllSettlementsResolved = true
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupSettlementOverviewScreenNonCreatorPreview() {
    PreviewThemeWrapper {
        GroupSettlementOverviewScreen(
            uiState = GroupSettlementOverviewUiState(
                isLoading = false,
                isUserCreator = false,
                pendingSettlements = persistentListOf(PREVIEW_SETTLEMENT_ROW)
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ArchiveSummaryStepPreview() {
    PreviewThemeWrapper {
        ArchiveSummaryStep(
            pendingCount = 2,
            disputedCount = 1,
            resolvedCount = 3,
            areAllSettlementsResolved = false
        )
    }
}

@PreviewComplete
@Composable
private fun ArchiveConfirmationStepPreview() {
    PreviewThemeWrapper {
        ArchiveConfirmationStep(
            groupName = "Summer Trip",
            hasUnresolvedSettlements = true,
            onGoToSettlementsClicked = {}
        )
    }
}
