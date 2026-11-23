package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
internal fun ThemeDescription(currentThemeCode: String) {
    val themeName = when (AppTheme.fromCode(currentThemeCode)) {
        AppTheme.SYSTEM -> stringResource(R.string.settings_preferences_theme_system)
        AppTheme.LIGHT -> stringResource(R.string.settings_preferences_theme_light)
        AppTheme.DARK -> stringResource(R.string.settings_preferences_theme_dark)
    }
    SecondaryBodyText(text = themeName, maxLines = Int.MAX_VALUE)
}
