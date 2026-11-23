package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bulb
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.MoonStars
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Settings
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.component.SettingsSelectionRow

@Composable
fun ThemeScreen(
    availableThemes: List<AppTheme>,
    selectedThemeCode: String,
    onThemeSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.Default)
    ) {
        item {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.ExtraSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                ) {
                    availableThemes.forEach { theme ->
                        SettingsSelectionRow(
                            title = stringResource(id = theme.getDisplayNameRes()),
                            subtitle = theme.englishName,
                            isSelected = theme.code == selectedThemeCode,
                            leadingIcon = theme.toIcon(),
                            onClick = { onThemeSelected(theme.code) }
                        )
                    }
                }
            }
        }
    }
}

private fun AppTheme.getDisplayNameRes(): Int = when (this) {
    AppTheme.SYSTEM -> R.string.settings_preferences_theme_system
    AppTheme.LIGHT -> R.string.settings_preferences_theme_light
    AppTheme.DARK -> R.string.settings_preferences_theme_dark
}

private fun AppTheme.toIcon(): ImageVector = when (this) {
    AppTheme.SYSTEM -> TablerIcons.Outline.Settings
    AppTheme.LIGHT -> TablerIcons.Outline.Bulb
    AppTheme.DARK -> TablerIcons.Outline.MoonStars
}
