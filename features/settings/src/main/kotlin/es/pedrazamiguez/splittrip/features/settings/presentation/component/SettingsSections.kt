package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsItemModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsSectionModel
import es.pedrazamiguez.splittrip.features.settings.presentation.view.SettingItemView

/**
 * Extension function to add settings sections to a LazyColumn.
 * Each section's items are grouped inside a [FlatCard] for tonal containment
 * (Horizon Narrative Layering Principle — tonal depth replaces explicit separators).
 */
fun LazyListScope.settingsSections(sections: List<SettingsSectionModel>) {
    sections.forEach { section ->
        item(key = "section_${section.titleRes}") {
            SettingsSectionHeader(titleRes = section.titleRes)
        }

        item(key = "card_${section.titleRes}") {
            FlatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    section.items.forEach { item ->
                        SettingsItemContent(item = item)
                    }
                }
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
