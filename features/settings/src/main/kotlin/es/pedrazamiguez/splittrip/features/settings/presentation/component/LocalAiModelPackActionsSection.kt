package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.LocalAiModelPackUiState

@Composable
internal fun LocalAiModelPackActionsSection(
    uiState: LocalAiModelPackUiState,
    onEvent: (LocalAiModelPackUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val fallbackPrimaryAction = resolveFallbackPrimaryAction(uiState)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        if (uiState.showUpgradeButton) {
            GradientButton(
                text = stringResource(R.string.local_ai_pack_action_upgrade),
                onClick = { onEvent(LocalAiModelPackUiEvent.RequestUpgrade) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        fallbackPrimaryAction?.let { (labelRes, event) ->
            GradientButton(
                text = stringResource(labelRes),
                onClick = { onEvent(event) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.catalogTiers.isEmpty() && uiState.showCancelButton) {
            SecondaryButton(
                text = stringResource(R.string.local_ai_pack_action_cancel),
                onClick = { onEvent(LocalAiModelPackUiEvent.CancelDownload(uiState.activeTargetPackId)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.showDeleteButton) {
            DestructiveButton(
                text = stringResource(R.string.local_ai_pack_action_delete),
                onClick = { onEvent(LocalAiModelPackUiEvent.RequestDelete) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun resolveFallbackPrimaryAction(
    uiState: LocalAiModelPackUiState
): Pair<Int, LocalAiModelPackUiEvent>? {
    if (uiState.catalogTiers.isNotEmpty()) return null
    return when {
        uiState.showDownloadButton -> {
            val labelRes = if (uiState.isUpdateAvailable) {
                R.string.local_ai_pack_action_update
            } else {
                R.string.local_ai_pack_action_download
            }
            labelRes to LocalAiModelPackUiEvent.RequestDownload(uiState.activeTargetPackId)
        }

        uiState.showRetryButton ->
            R.string.local_ai_pack_action_retry to
                LocalAiModelPackUiEvent.RetryDownload(uiState.activeTargetPackId)

        else -> null
    }
}
