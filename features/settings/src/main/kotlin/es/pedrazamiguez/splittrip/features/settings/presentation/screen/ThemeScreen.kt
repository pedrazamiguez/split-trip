package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun ThemeScreen(
    availableThemes: List<AppTheme>,
    selectedThemeCode: String,
    onThemeSelected: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(availableThemes) { theme ->

            val isSelected = theme.code == selectedThemeCode
            val displayName = stringResource(id = theme.getDisplayNameRes())

            ListItem(
                headlineContent = { Text(text = displayName) },
                supportingContent = { Text(text = theme.englishName) },
                trailingContent = {
                    if (isSelected) {
                        Icon(
                            imageVector = TablerIcons.Outline.Check,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.clickable {
                    onThemeSelected(theme.code)
                }
            )
        }
    }
}

private fun AppTheme.getDisplayNameRes(): Int = when (this) {
    AppTheme.SYSTEM -> R.string.settings_preferences_theme_system
    AppTheme.LIGHT -> R.string.settings_preferences_theme_light
    AppTheme.DARK -> R.string.settings_preferences_theme_dark
}
