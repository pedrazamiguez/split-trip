package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

@Composable
internal fun GroupArchivedTag(
    modifier: Modifier = Modifier
) {
    PassportTag(
        text = stringResource(DesignR.string.group_detail_archived_label),
        icon = TablerIcons.Outline.Lock,
        style = PassportTagStyle.WARNING,
        iconContentDescription = null,
        modifier = modifier
    )
}
