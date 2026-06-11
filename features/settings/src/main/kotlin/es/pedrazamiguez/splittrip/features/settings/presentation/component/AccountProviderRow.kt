package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
internal fun AccountProviderRow(
    modifier: Modifier = Modifier,
    name: String,
    isLinked: Boolean,
    onLinkClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    canUnlink: Boolean,
    isActionLoading: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    if (isLinked) {
                        R.string.account_status_linked
                    } else {
                        R.string.account_status_not_linked
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (isLinked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))

        if (isLinked) {
            SecondaryButton(
                text = stringResource(R.string.account_status_unlink_button),
                onClick = onUnlinkClick,
                enabled = !isActionLoading && canUnlink
            )
        } else {
            SecondaryButton(
                text = stringResource(R.string.account_status_link_button),
                onClick = onLinkClick,
                enabled = !isActionLoading
            )
        }
    }
}
