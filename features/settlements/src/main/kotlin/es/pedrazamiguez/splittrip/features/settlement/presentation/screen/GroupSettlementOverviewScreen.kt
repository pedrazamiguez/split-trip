package es.pedrazamiguez.splittrip.features.settlement.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.DisputeSettlementBottomSheet
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.SettlementContent
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.step.archive.ArchiveConfirmationStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.step.archive.ArchiveSummaryStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.archive.ArchiveWizardStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.GroupSettlementOverviewUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.GroupSettlementOverviewUiState

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun GroupSettlementOverviewScreen(
    uiState: GroupSettlementOverviewUiState,
    onEvent: (GroupSettlementOverviewUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.activeDisputeSettlementId != null) {
        DisputeSettlementBottomSheet(
            reason = uiState.disputeReasonInput,
            onReasonChanged = { onEvent(GroupSettlementOverviewUiEvent.DisputeReasonChanged(it)) },
            onSubmit = { onEvent(GroupSettlementOverviewUiEvent.DisputeSubmitted) },
            onDismiss = { onEvent(GroupSettlementOverviewUiEvent.DisputeCancelled) }
        )
    }

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        when {
            uiState.hasError -> EmptyStateView(
                title = stringResource(R.string.settlement_overview_error_loading)
            )
            uiState.isUserCreator -> {
                val activeSteps = uiState.activeSteps
                val currentStepIndex = activeSteps.indexOf(uiState.currentStep).coerceAtLeast(0)
                val isOnLastStep = uiState.currentStep == activeSteps.lastOrNull()
                val bottomPadding = LocalBottomPadding.current

                val isCurrentStepValid = when (uiState.currentStep) {
                    ArchiveWizardStep.CONFIRMATION -> uiState.areAllSettlementsResolved
                    else -> true
                }

                Column(modifier = modifier.fillMaxSize()) {
                    if (activeSteps.isNotEmpty()) {
                        WizardStepIndicator(
                            stepLabels = activeSteps.map { stringResource(it.labelResId) },
                            currentStepIndex = currentStepIndex
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (uiState.currentStep) {
                            ArchiveWizardStep.SETTLEMENT_SUMMARY -> {
                                ArchiveSummaryStep(
                                    pendingCount = uiState.actionRequiredCount + uiState.waitingOnOthersCount,
                                    disputedCount = uiState.disputedCount,
                                    resolvedCount = uiState.resolvedSettlements.size,
                                    areAllSettlementsResolved = uiState.areAllSettlementsResolved,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }

                            ArchiveWizardStep.CONFIRMATION -> {
                                ArchiveConfirmationStep(
                                    groupName = uiState.groupName,
                                    hasUnresolvedSettlements = !uiState.areAllSettlementsResolved,
                                    onGoToSettlementsClicked = {
                                        onEvent(
                                            GroupSettlementOverviewUiEvent.NavigateToYourBalanceClicked
                                        )
                                    },
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }

                    WizardNavigationBar(
                        config = WizardNavigationBarConfig(
                            canGoNext = isCurrentStepValid,
                            isOnLastStep = isOnLastStep,
                            isCurrentStepValid = isCurrentStepValid,
                            isLoading = uiState.isArchiving,
                            backLabel = stringResource(R.string.group_wizard_back),
                            nextLabel = stringResource(R.string.group_wizard_next),
                            submitLabel = stringResource(R.string.settlement_overview_close_trip)
                        ),
                        onBack = { onEvent(GroupSettlementOverviewUiEvent.WizardBackClicked) },
                        onNext = { onEvent(GroupSettlementOverviewUiEvent.WizardNextClicked) },
                        onSubmit = { onEvent(GroupSettlementOverviewUiEvent.CloseTripClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottomPadding)
                    )
                }
            }
            else -> SettlementContent(uiState = uiState, onEvent = onEvent, modifier = modifier)
        }
    }
}
