package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Camera
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.group.R

@Composable
internal fun GroupImageActions(
    hasImage: Boolean,
    onSelectClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!hasImage) {
        GradientButton(
            text = stringResource(R.string.group_image_step_select_btn),
            onClick = onSelectClick,
            modifier = modifier.fillMaxWidth(),
            leadingIcon = TablerIcons.Outline.Camera
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
        ) {
            SecondaryButton(
                text = stringResource(R.string.group_image_step_change_btn),
                onClick = onSelectClick,
                modifier = Modifier.weight(1f)
            )
            DestructiveButton(
                text = stringResource(R.string.group_image_step_remove_btn),
                onClick = onRemoveClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
