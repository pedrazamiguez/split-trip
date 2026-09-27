package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.badge.ProBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.expense.R

/**
 * Promotional card prompting the user to switch to AI receipt scanning mode.
 * Shows a [ProBadge] when the feature is gated or tied to a premium tier.
 */
@Composable
fun AiScanPromptCard(
    isProFeature: Boolean,
    onSwitchToAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                Text(
                    text = stringResource(R.string.expense_autofill_prompt_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isProFeature) {
                    ProBadge()
                }
            }
            Text(
                text = stringResource(R.string.expense_autofill_prompt_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SecondaryButton(
                text = if (isProFeature) {
                    stringResource(R.string.expense_autofill_unlock_with_pro)
                } else {
                    stringResource(R.string.expense_autofill_switch_ai)
                },
                onClick = onSwitchToAi,
                leadingIcon = if (isProFeature) TablerIcons.Outline.Lock else null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
