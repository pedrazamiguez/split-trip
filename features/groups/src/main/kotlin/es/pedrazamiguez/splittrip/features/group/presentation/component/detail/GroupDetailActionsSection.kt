package es.pedrazamiguez.splittrip.features.group.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.R

@Suppress("LongParameterList")
@Composable
fun GroupDetailActionsSection(
    isActiveGroup: Boolean,
    isOnlyGroup: Boolean,
    isUserAdmin: Boolean,
    isLeaving: Boolean,
    status: GroupStatus,
    onSelectGroup: () -> Unit,
    onArchiveGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
        modifier = modifier.fillMaxWidth()
    ) {
        if (isActiveGroup) {
            if (!isOnlyGroup) {
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

        if (status == GroupStatus.ACTIVE && isUserAdmin) {
            DestructiveButton(
                text = stringResource(DesignSystemR.string.group_detail_end_trip),
                onClick = onArchiveGroup,
                leadingIcon = TablerIcons.Outline.Lock,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (status == GroupStatus.ACTIVE && !isUserAdmin) {
            DestructiveButton(
                text = stringResource(R.string.action_leave_group),
                onClick = onLeaveGroup,
                leadingIcon = TablerIcons.Outline.X,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLeaving
            )
        }
    }
}
