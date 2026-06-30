package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.MemberAvatarStack
import es.pedrazamiguez.splittrip.features.group.presentation.component.SelectedGroupCoverImage
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupDetailUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState

private val CONTENT_HORIZONTAL_PADDING = 16.dp
private val SECTION_VERTICAL_SPACING = 16.dp
private val SECTION_LABEL_ICON_SIZE = 16.dp

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

    when {
        uiState.isLoading -> ShimmerLoadingList()
        uiState.hasError || uiState.group == null -> {
            EmptyStateView(
                title = stringResource(R.string.group_detail_error_loading),
                icon = TablerIcons.Outline.UsersGroup
            )
        }
        else -> {
            val group = uiState.group

            if (uiState.showArchiveConfirmation) {
                DestructiveConfirmationDialog(
                    title = stringResource(DesignSystemR.string.group_detail_end_trip_title),
                    text = stringResource(DesignSystemR.string.group_detail_end_trip_message),
                    onConfirm = { onEvent(GroupDetailUiEvent.ArchiveConfirmed) },
                    onDismiss = { onEvent(GroupDetailUiEvent.ArchiveCancelled) },
                    confirmLabel = stringResource(DesignSystemR.string.group_detail_end_trip_confirm)
                )
            }

            if (uiState.showDeleteConfirmation && group != null) {
                DestructiveConfirmationDialog(
                    title = stringResource(R.string.group_delete_title),
                    text = stringResource(R.string.group_delete_warning, group.name),
                    onConfirm = { onEvent(GroupDetailUiEvent.DeleteConfirmed) },
                    onDismiss = { onEvent(GroupDetailUiEvent.DeleteCancelled) }
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

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))

                    // Members label
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.UsersGroup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(SECTION_LABEL_ICON_SIZE)
                        )
                        LabelText(text = stringResource(R.string.group_detail_section_members))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                    ) {
                        if (group.memberAvatarUrls.isNotEmpty() || group.memberOverflowCount > 0) {
                            MemberAvatarStack(
                                avatarUrls = group.memberAvatarUrls,
                                overflowCount = group.memberOverflowCount
                            )
                        }
                        if (group.membersCountText.isNotEmpty()) {
                            BodyText(
                                text = group.membersCountText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Subunits label
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.Sitemap,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(SECTION_LABEL_ICON_SIZE)
                        )
                        LabelText(text = stringResource(R.string.group_detail_section_subunits))
                    }
                    val subunitsText = if (uiState.subunitsCount == 0) {
                        stringResource(R.string.group_detail_no_subunits)
                    } else {
                        pluralStringResource(
                            R.plurals.group_detail_subunit_count,
                            uiState.subunitsCount,
                            uiState.subunitsCount
                        )
                    }
                    FlatCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable(onClick = onManageSubunits)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.spacing.Default,
                                    vertical = MaterialTheme.spacing.Medium
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BodyText(
                                text = subunitsText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.subunitsCount > 0) {
                                Text(
                                    text = stringResource(R.string.group_detail_manage_subunits),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

                    // Group actions
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isActiveGroup) {
                            if (!uiState.isOnlyGroup) {
                                SecondaryButton(
                                    text = stringResource(R.string.action_deselect_group),
                                    onClick = onSelectGroup,
                                    leadingIcon = TablerIcons.Outline.X,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            GradientButton(
                                text = stringResource(R.string.group_detail_select_as_active),
                                onClick = onSelectGroup,
                                leadingIcon = TablerIcons.Outline.CircleCheck,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        SecondaryButton(
                            text = stringResource(R.string.group_detail_manage_subunits),
                            onClick = onManageSubunits,
                            leadingIcon = TablerIcons.Outline.Sitemap,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (group.status == GroupStatus.ACTIVE && uiState.isUserAdmin) {
                            DestructiveButton(
                                text = stringResource(DesignSystemR.string.group_detail_end_trip),
                                onClick = { onEvent(GroupDetailUiEvent.ArchiveClicked) },
                                leadingIcon = TablerIcons.Outline.Lock,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isArchiving
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge + bottomPadding))
                }
            }
        }
    }
}
