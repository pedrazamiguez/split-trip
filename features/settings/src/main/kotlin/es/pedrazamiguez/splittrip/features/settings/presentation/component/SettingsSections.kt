package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsItemModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsSectionModel
import es.pedrazamiguez.splittrip.features.settings.presentation.view.SettingItemView

/**
 * Extension function to add settings sections to a LazyColumn.
 *
 * Each row remains its own lazy item (preserving virtualization and stable keys).
 * Tonal grouping is achieved by painting all rows in a section with
 * [MaterialTheme.colorScheme.surfaceContainerLow] and rounding only the
 * outer top/bottom corners — giving the visual appearance of a card without
 * collapsing the section into a single non-virtualised lazy item.
 */
fun LazyListScope.settingsSections(sections: List<SettingsSectionModel>) {
    sections.forEach { section ->
        item(key = "section_${section.titleRes}") {
            SettingsSectionHeader(titleRes = section.titleRes)
        }

        itemsIndexed(
            items = section.items,
            key = { _, item ->
                when (item) {
                    is SettingsItemModel.Standard -> "item_${item.titleRes}"
                    is SettingsItemModel.WithTrailing -> "item_trailing_${item.titleRes}"
                    is SettingsItemModel.WithCustomDescription -> "item_custom_desc_${item.titleRes}"
                    is SettingsItemModel.Custom -> "item_custom_${item.hashCode()}"
                }
            }
        ) { index, item ->
            val isFirst = index == 0
            val isLast = index == section.items.lastIndex
            // Round only the corners that form the outer edge of the card group.
            val sectionShape = MaterialTheme.shapes.large
            val noCorner = CornerSize(0.dp)
            val rowShape = when {
                isFirst && isLast -> sectionShape
                isFirst -> sectionShape.copy(bottomStart = noCorner, bottomEnd = noCorner)
                isLast -> sectionShape.copy(topStart = noCorner, topEnd = noCorner)
                else -> RectangleShape
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.Default)
                    .clip(rowShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                SettingsItemContent(item = item)
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(titleRes: Int) {
    SettingsSection(title = stringResource(titleRes))
}

@Composable
private fun SettingsItemContent(item: SettingsItemModel) {
    when (item) {
        is SettingsItemModel.Standard -> {
            SettingsRow(
                item = SettingItemView(
                    icon = item.icon,
                    title = stringResource(item.titleRes),
                    description = item.descriptionRes?.let { stringResource(it) },
                    onClick = item.onClick
                )
            )
        }

        is SettingsItemModel.WithTrailing -> {
            SettingsRow(
                item = SettingItemView(
                    icon = item.icon,
                    title = stringResource(item.titleRes),
                    description = item.descriptionRes?.let { stringResource(it) },
                    onClick = item.onClick
                ),
                trailingContent = item.trailingContent
            )
        }

        is SettingsItemModel.WithCustomDescription -> {
            SettingsRow(
                item = SettingItemView(
                    icon = item.icon,
                    title = stringResource(item.titleRes),
                    description = null,
                    onClick = item.onClick
                ),
                descriptionContent = item.descriptionContent
            )
        }

        is SettingsItemModel.Custom -> {
            item.content()
        }
    }
}
