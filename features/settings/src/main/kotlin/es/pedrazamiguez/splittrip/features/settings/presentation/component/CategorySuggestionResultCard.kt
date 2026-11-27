package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bolt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Category
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cpu
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sparkles
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.formatLiteRtDisplayLabel
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.toLabelRes
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo

private const val DIVIDER_ALPHA = 0.4f

private data class SuggestionSourceTagConfig(
    val text: String,
    val icon: ImageVector,
    val style: PassportTagStyle
)

@Composable
internal fun CategorySuggestionResultCard(
    category: ExpenseCategory?,
    subcategory: ExpenseSubcategory?,
    source: CategorySuggestionSource?,
    activeResolvedModel: AiEngineType?,
    installedLocalAiPack: InstalledLocalAiPackInfo?,
    modifier: Modifier = Modifier
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA)
    val sourceTag = resolveSourceTagConfig(
        source = source,
        localRulesLabel = stringResource(R.string.developer_services_category_source_local_rules),
        onDeviceAiLabel = stringResource(R.string.developer_services_category_source_on_device_ai)
    )
    val engineValue = resolveActiveEngineLabel(
        source = source,
        activeResolvedModel = activeResolvedModel,
        aiCoreLabel = stringResource(R.string.developer_services_ai_model_ai_core),
        liteRtLabel = formatLiteRtDisplayLabel(
            baseLabel = stringResource(R.string.developer_services_ai_model_lite_rt),
            installedPack = installedLocalAiPack,
            localizedTierLabel = installedLocalAiPack?.tier?.let { stringResource(it.toLabelRes()) }
        )
    )

    SectionCard(title = stringResource(R.string.developer_services_category_result_title), modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.developer_services_category_source_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                sourceTag?.let { tag ->
                    PassportTag(text = tag.text, icon = tag.icon, style = tag.style)
                }
            }
            HorizontalDivider(color = dividerColor)
            ExtractionFieldRow(
                icon = category?.toIconVector() ?: TablerIcons.Outline.Category,
                label = stringResource(R.string.developer_services_extraction_category),
                value = category?.let { "${stringResource(it.toStringRes())} (${it.name})" }
            )
            HorizontalDivider(color = dividerColor)
            ExtractionFieldRow(
                icon = subcategory?.toIconVector() ?: TablerIcons.Outline.Sitemap,
                label = stringResource(R.string.developer_services_extraction_subcategory),
                value = subcategory?.let { "${stringResource(it.toStringRes())} (${it.name})" }
            )
            engineValue?.let { resolvedEngine ->
                HorizontalDivider(color = dividerColor)
                ExtractionFieldRow(
                    icon = TablerIcons.Outline.Cpu,
                    label = stringResource(R.string.developer_services_category_engine_label),
                    value = resolvedEngine
                )
            }
        }
    }
}

private fun resolveSourceTagConfig(
    source: CategorySuggestionSource?,
    localRulesLabel: String,
    onDeviceAiLabel: String
): SuggestionSourceTagConfig? = when (source) {
    null -> null
    CategorySuggestionSource.LOCAL_RULES -> SuggestionSourceTagConfig(
        text = localRulesLabel,
        icon = TablerIcons.Outline.Bolt,
        style = PassportTagStyle.SUCCESS
    )
    CategorySuggestionSource.ON_DEVICE_AI -> SuggestionSourceTagConfig(
        text = onDeviceAiLabel,
        icon = TablerIcons.Outline.Sparkles,
        style = PassportTagStyle.INFO
    )
}

private fun resolveActiveEngineLabel(
    source: CategorySuggestionSource?,
    activeResolvedModel: AiEngineType?,
    aiCoreLabel: String,
    liteRtLabel: String
): String? {
    if (source != CategorySuggestionSource.ON_DEVICE_AI || activeResolvedModel == null) return null
    return when (activeResolvedModel) {
        AiEngineType.AI_CORE_GEMMA_4 -> aiCoreLabel
        AiEngineType.LITE_RT_LM -> liteRtLabel
    }
}
