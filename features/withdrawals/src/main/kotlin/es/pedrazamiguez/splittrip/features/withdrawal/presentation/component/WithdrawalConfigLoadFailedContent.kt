package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.features.withdrawal.R

@Composable
internal fun WithdrawalConfigLoadFailedContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        title = stringResource(R.string.withdrawal_error_load_config),
        icon = TablerIcons.Outline.Refresh,
        modifier = modifier,
        action = {
            SecondaryButton(
                text = stringResource(R.string.withdrawal_retry),
                onClick = onRetry,
                leadingIcon = TablerIcons.Outline.Refresh
            )
        }
    )
}
