package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LargeBodyText
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
internal fun AccountStatusHeader(
    email: String,
    joinDateText: String,
    isAnonymous: Boolean = false,
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.height(MaterialTheme.spacing.Medium))

    if (isAnonymous) {
        LargeBodyText(
            text = stringResource(R.string.account_status_guest_label),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
        Text(
            text = stringResource(R.string.account_status_guest_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    } else {
        LargeBodyText(
            text = email,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (joinDateText.isNotBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
            Text(
                text = stringResource(R.string.account_status_member_since, joinDateText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
