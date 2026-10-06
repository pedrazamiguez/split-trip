package es.pedrazamiguez.splittrip.core.designsystem.preview.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun EmptyStateViewPreview() {
    PreviewThemeWrapper {
        EmptyStateView(
            title = stringResource(R.string.empty_state_no_expenses_title),
            description = stringResource(R.string.empty_state_no_expenses_description)
        )
    }
}

@PreviewComplete
@Composable
private fun EmptyStateViewWithActionPreview() {
    PreviewThemeWrapper {
        EmptyStateView(
            title = stringResource(R.string.empty_state_no_expenses_title),
            description = stringResource(R.string.empty_state_no_expenses_description),
            action = {
                SecondaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = {}
                )
            }
        )
    }
}
