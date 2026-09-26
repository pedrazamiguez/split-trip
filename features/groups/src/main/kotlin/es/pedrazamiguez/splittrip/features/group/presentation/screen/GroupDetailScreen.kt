package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.SelectedGroupCoverImage
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailActionsSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailInfoSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailMembersSection
import es.pedrazamiguez.splittrip.features.group.presentation.component.detail.GroupDetailSubunitCard
import es.pedrazamiguez.splittrip.features.group.presentation.component.leave.GroupLeaveWizardSheet
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupDetailUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState

private val CONTENT_HORIZONTAL_PADDING = 16.dp
private val SECTION_VERTICAL_SPACING = 16.dp

@Suppress("LongMethod", "CognitiveComplexMethod", "CyclomaticComplexMethod")
@Composable
fun GroupDetailScreen(
    uiState: GroupDetailUiState = GroupDetailUiState(),
    isActiveGroup: Boolean = false,
    onSelectGroup: () -> Unit = {},
    onManageSubunits: () -> Unit = {},
    onEvent: (GroupDetailUiEvent) -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        when {
            uiState.hasError || uiState.group == null -> {
                EmptyStateView(
                    title = stringResource(R.string.group_detail_error_loading),
                    icon = TablerIcons.Outline.UsersGroup
                )
            }
            else -> {
                val group = uiState.group

                if (uiState.showDeleteConfirmation) {
                    DestructiveConfirmationDialog(
                        title = stringResource(R.string.group_delete_title),
                        text = stringResource(R.string.group_delete_warning, group.name),
                        onConfirm = { onEvent(GroupDetailUiEvent.DeleteConfirmed) },
                        onDismiss = { onEvent(GroupDetailUiEvent.DeleteCancelled) }
                    )
                }

                if (uiState.leaveWizardState.showSheet) {
                    GroupLeaveWizardSheet(
                        groupName = group.name,
                        leaveWizardState = uiState.leaveWizardState,
                        onNextClicked = { onEvent(GroupDetailUiEvent.WizardNextClicked) },
                        onBackClicked = { onEvent(GroupDetailUiEvent.WizardBackClicked) },
                        onDismissRequest = { onEvent(GroupDetailUiEvent.WizardCancelled) },
                        onConfirmLeave = { onEvent(GroupDetailUiEvent.LeaveConfirmed) },
                        onGoToSettlementsClicked = {
                            onEvent(GroupDetailUiEvent.NavigateToYourBalanceClicked)
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectedGroupCoverImage(
                        imageUrl = group.imageUrl,
                        groupName = group.name,
                        showActiveBadge = isActiveGroup
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CONTENT_HORIZONTAL_PADDING),
                        verticalArrangement = Arrangement.spacedBy(SECTION_VERTICAL_SPACING)
                    ) {
                        if (group.status == GroupStatus.ARCHIVED) {
                            FlatCard(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.Outline.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = stringResource(DesignSystemR.string.group_detail_archived_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        GroupDetailInfoSection(
                            group = group,
                            currencyRates = uiState.currencyRates
                        )

                        GroupDetailSubunitCard(
                            subunitsCount = uiState.subunitsCount,
                            onManageSubunits = onManageSubunits
                        )

                        if (group.members.isNotEmpty()) {
                            GroupDetailMembersSection(members = group.members)
                        }

                        GroupDetailActionsSection(
                            isActiveGroup = isActiveGroup,
                            isOnlyGroup = uiState.isOnlyGroup,
                            isUserAdmin = uiState.isUserAdmin,
                            isLeaving = uiState.isLeaving,
                            status = group.status,
                            onSelectGroup = onSelectGroup,
                            onArchiveGroup = { onEvent(GroupDetailUiEvent.ArchiveClicked) },
                            onLeaveGroup = { onEvent(GroupDetailUiEvent.LeaveClicked) }
                        )

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge + bottomPadding))
                    }
                }
            }
        }
    }
}
