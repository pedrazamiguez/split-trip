package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.HorizonModalBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.rememberHorizonBottomSheetState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.formatLiteRtDisplayLabel
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.toLabelRes
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class AiModelOptionCopy(
    val headline: String,
    val supportingText: String
)

private data class AiModelPickerStrings(
    val automaticTitle: String,
    val automaticSubtitle: String,
    val aiCoreTitle: String,
    val aiCoreSubtitle: String,
    val liteRtBaseTitle: String,
    val liteRtInstalledSubtitle: String?,
    val liteRtNotInstalledSubtitle: String,
    val localizedTierLabel: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AiModelPickerBottomSheet(
    availableModels: ImmutableList<AiEngineType?>,
    selectedModel: AiEngineType?,
    installedLocalAiPack: InstalledLocalAiPackInfo?,
    onModelSelected: (AiEngineType?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberHorizonBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val localizedTierLabel = installedLocalAiPack?.tier?.let { stringResource(it.toLabelRes()) }
    val strings = AiModelPickerStrings(
        automaticTitle = stringResource(R.string.developer_services_ai_model_automatic),
        automaticSubtitle = stringResource(R.string.developer_services_ai_model_automatic_subtitle),
        aiCoreTitle = stringResource(R.string.developer_services_ai_model_ai_core),
        aiCoreSubtitle = stringResource(R.string.developer_services_ai_model_ai_core_subtitle),
        liteRtBaseTitle = stringResource(R.string.developer_services_ai_model_lite_rt),
        liteRtInstalledSubtitle = if (installedLocalAiPack != null && localizedTierLabel != null) {
            stringResource(
                R.string.developer_services_ai_model_lite_rt_installed_subtitle,
                localizedTierLabel,
                installedLocalAiPack.packId
            )
        } else {
            null
        },
        liteRtNotInstalledSubtitle = stringResource(
            R.string.developer_services_ai_model_lite_rt_not_installed_subtitle
        ),
        localizedTierLabel = localizedTierLabel
    )

    HorizonModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.ExtraLarge, bottom = MaterialTheme.spacing.ExtraLarge)
        ) {
            SheetTitleText(
                text = stringResource(R.string.developer_services_ai_model_picker_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.Large)
                    .padding(bottom = MaterialTheme.spacing.Default)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items = availableModels, key = { it?.name ?: "automatic" }) { model ->
                    val optionCopy = resolveAiModelOptionCopy(model, installedLocalAiPack, strings)
                    AiModelPickerOptionRow(
                        headline = optionCopy.headline,
                        supportingText = optionCopy.supportingText,
                        isSelected = selectedModel == model,
                        onClick = {
                            hideAndSelectModel(coroutineScope, sheetState, model, onModelSelected, onDismiss)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun hideAndSelectModel(
    coroutineScope: CoroutineScope,
    sheetState: SheetState,
    model: AiEngineType?,
    onModelSelected: (AiEngineType?) -> Unit,
    onDismiss: () -> Unit
) {
    coroutineScope.launch {
        sheetState.hide()
    }.invokeOnCompletion { cause ->
        if (cause == null) {
            onModelSelected(model)
            onDismiss()
        }
    }
}

private fun resolveAiModelOptionCopy(
    model: AiEngineType?,
    installedLocalAiPack: InstalledLocalAiPackInfo?,
    strings: AiModelPickerStrings
): AiModelOptionCopy = when (model) {
    null -> AiModelOptionCopy(strings.automaticTitle, strings.automaticSubtitle)
    AiEngineType.AI_CORE_GEMMA_4 -> AiModelOptionCopy(strings.aiCoreTitle, strings.aiCoreSubtitle)
    AiEngineType.LITE_RT_LM -> AiModelOptionCopy(
        headline = formatLiteRtDisplayLabel(
            baseLabel = strings.liteRtBaseTitle,
            installedPack = installedLocalAiPack,
            localizedTierLabel = strings.localizedTierLabel
        ),
        supportingText = strings.liteRtInstalledSubtitle
            ?: installedLocalAiPack?.packId
            ?: strings.liteRtNotInstalledSubtitle
    )
}
