package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierAction
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent

private val WARNING_ICON_SIZE = 16.dp

@Composable
internal fun LocalAiModelPackTierFooter(
    tier: LocalAiModelPackTierUiModel,
    onEvent: (LocalAiModelPackUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        tier.incompatibilityReason?.let { reason ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.AlertTriangle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(WARNING_ICON_SIZE)
                )
                Text(
                    text = reason.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (tier.isDownloading) {
            LinearProgressIndicator(progress = { tier.progressFraction }, modifier = Modifier.fillMaxWidth())
            tier.progressLabel?.let { label ->
                Text(
                    text = label.asString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (tier.isVerifying) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val actionLabel = tier.actionLabel
        val actionEvent = resolveTierActionEvent(tier)
        if (actionLabel != null && actionEvent != null) {
            if (tier.action == LocalAiModelPackTierAction.CANCEL) {
                SecondaryButton(
                    text = actionLabel.asString(),
                    enabled = tier.isActionEnabled,
                    onClick = { onEvent(actionEvent) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GradientButton(
                    text = actionLabel.asString(),
                    enabled = tier.isActionEnabled,
                    onClick = { onEvent(actionEvent) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun resolveTierActionEvent(tier: LocalAiModelPackTierUiModel): LocalAiModelPackUiEvent? =
    when (tier.action) {
        LocalAiModelPackTierAction.CANCEL -> LocalAiModelPackUiEvent.CancelDownload(tier.packId)
        LocalAiModelPackTierAction.RETRY -> LocalAiModelPackUiEvent.RetryDownload(tier.packId)
        LocalAiModelPackTierAction.DOWNLOAD,
        LocalAiModelPackTierAction.SWITCH,
        LocalAiModelPackTierAction.UPDATE -> LocalAiModelPackUiEvent.RequestDownload(tier.packId)
        LocalAiModelPackTierAction.NONE -> null
    }
