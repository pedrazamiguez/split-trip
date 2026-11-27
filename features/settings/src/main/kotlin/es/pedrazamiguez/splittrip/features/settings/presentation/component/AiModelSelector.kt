package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.CaretDownFilled
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.formatLiteRtDisplayLabel
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.toLabelRes
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AiModelSelector(
    availableModels: ImmutableList<AiEngineType?>,
    selectedModel: AiEngineType?,
    resolvedModel: AiEngineType?,
    installedLocalAiPack: InstalledLocalAiPackInfo?,
    onModelSelected: (AiEngineType?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val localizedTierLabel = installedLocalAiPack?.tier?.let { stringResource(it.toLabelRes()) }
    val selectedLabel = when (selectedModel) {
        null -> stringResource(R.string.developer_services_ai_model_automatic)
        AiEngineType.AI_CORE_GEMMA_4 -> stringResource(R.string.developer_services_ai_model_ai_core)
        AiEngineType.LITE_RT_LM -> formatLiteRtDisplayLabel(
            baseLabel = stringResource(R.string.developer_services_ai_model_lite_rt),
            installedPack = installedLocalAiPack,
            localizedTierLabel = localizedTierLabel
        )
    }

    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            StyledOutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                focusable = false,
                singleLine = true,
                label = stringResource(R.string.developer_services_ai_model_selection),
                trailingIcon = { Icon(TablerIcons.Filled.CaretDownFilled, contentDescription = null) },
                onClick = {
                    focusManager.clearFocus()
                    isSheetOpen = true
                },
                modifier = Modifier.fillMaxWidth()
            )
            resolvedModel?.let { resolved ->
                AiResolvedEngineRow(
                    resolvedModel = resolved,
                    installedLocalAiPack = installedLocalAiPack
                )
            }
        }
    }

    if (isSheetOpen) {
        AiModelPickerBottomSheet(
            availableModels = availableModels,
            selectedModel = selectedModel,
            installedLocalAiPack = installedLocalAiPack,
            onModelSelected = onModelSelected,
            onDismiss = { isSheetOpen = false }
        )
    }
}
