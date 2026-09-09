package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.features.profile.R

@Composable
internal fun ProfileErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        title = stringResource(R.string.profile_error_loading),
        icon = TablerIcons.Outline.Refresh,
        modifier = modifier,
        action = {
            SecondaryButton(
                text = stringResource(R.string.profile_retry_button),
                onClick = onRetry,
                leadingIcon = TablerIcons.Outline.Refresh
            )
        }
    )
}
