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
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.MemberAvatarStack
import es.pedrazamiguez.splittrip.features.group.presentation.component.SelectedGroupCoverImage
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState

private val CONTENT_HORIZONTAL_PADDING = 16.dp
private val SECTION_VERTICAL_SPACING = 16.dp
private val SECTION_LABEL_ICON_SIZE = 16.dp

@Composable
fun GroupDetailScreen(
    uiState: GroupDetailUiState = GroupDetailUiState(),
    isActiveGroup: Boolean = false,
    onSelectGroup: () -> Unit = {},
    onManageSubunits: () -> Unit = {}
) {
    when {
        uiState.isLoading -> ShimmerLoadingList()
        uiState.hasError || uiState.group == null -> {
            EmptyStateView(
                title = stringResource(R.string.group_detail_error_loading),
                icon = TablerIcons.Outline.UsersGroup
            )
        }
        else -> {
            GroupDetailContent(
                group = uiState.group,
                isActiveGroup = isActiveGroup,
                subunitsCount = uiState.subunitsCount,
                onSelectGroup = onSelectGroup,
                onManageSubunits = onManageSubunits
            )
        }
    }
}

@Composable
private fun GroupDetailContent(
    group: GroupUiModel,
    isActiveGroup: Boolean,
    subunitsCount: Int,
    onSelectGroup: () -> Unit,
    onManageSubunits: () -> Unit
) {
    val bottomPadding = LocalBottomPadding.current

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
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))

            GroupDetailMembersSection(group = group)

            GroupDetailSubunitsSection(
                subunitsCount = subunitsCount,
                onManageSubunits = onManageSubunits
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

            GroupDetailActions(
                isActiveGroup = isActiveGroup,
                onSelectGroup = onSelectGroup,
                onManageSubunits = onManageSubunits
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge + bottomPadding))
        }
    }
}

@Composable
private fun GroupDetailMembersSection(group: GroupUiModel) {
    GroupDetailSectionLabel(
        label = stringResource(R.string.group_detail_section_members),
        icon = { SectionIcon(imageVector = TablerIcons.Outline.UsersGroup) }
    )
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
            Text(
                text = group.membersCountText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupDetailSubunitsSection(
    subunitsCount: Int,
    onManageSubunits: () -> Unit
) {
    GroupDetailSectionLabel(
        label = stringResource(R.string.group_detail_section_subunits),
        icon = { SectionIcon(imageVector = TablerIcons.Outline.Sitemap) }
    )
    val subunitsText = if (subunitsCount == 0) {
        stringResource(R.string.group_detail_no_subunits)
    } else {
        pluralStringResource(R.plurals.group_detail_subunit_count, subunitsCount, subunitsCount)
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
                .padding(horizontal = MaterialTheme.spacing.Default, vertical = MaterialTheme.spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subunitsText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subunitsCount > 0) {
                Text(
                    text = stringResource(R.string.group_detail_manage_subunits),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GroupDetailActions(
    isActiveGroup: Boolean,
    onSelectGroup: () -> Unit,
    onManageSubunits: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isActiveGroup) {
            SecondaryButton(
                text = stringResource(R.string.action_deselect_group),
                onClick = onSelectGroup,
                leadingIcon = TablerIcons.Outline.X,
                modifier = Modifier.fillMaxWidth()
            )
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
    }
}

@Composable
private fun GroupDetailSectionLabel(
    label: String,
    icon: @Composable () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(SECTION_LABEL_ICON_SIZE)
    )
}
