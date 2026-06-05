package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.ThemeScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThemeFeature(viewModel: ThemeViewModel = koinViewModel()) {
    val selectedTheme by viewModel.selectedThemeCode.collectAsStateWithLifecycle()

    val navController = LocalRootNavController.current
    val scope = rememberCoroutineScope()
    val pillController = LocalTopPillController.current

    val themeNames = viewModel.availableThemes.associate { theme ->
        theme.code to when (theme) {
            AppTheme.SYSTEM -> stringResource(R.string.settings_preferences_theme_system)
            AppTheme.LIGHT -> stringResource(R.string.settings_preferences_theme_light)
            AppTheme.DARK -> stringResource(R.string.settings_preferences_theme_dark)
        }
    }
    val themeChangedFormat = stringResource(R.string.settings_preferences_theme_changed_format)

    FeatureScaffold(currentRoute = Routes.SETTINGS_THEME) {
        ThemeScreen(
            availableThemes = viewModel.availableThemes,
            selectedThemeCode = selectedTheme,
            onThemeSelected = { newThemeCode ->
                viewModel.onThemeSelected(newThemeCode)

                val themeName = themeNames[newThemeCode] ?: newThemeCode

                pillController.showPill(
                    themeChangedFormat.format(themeName)
                )

                scope.launch {
                    delay(UiConstants.NAV_FEEDBACK_DELAY)
                    navController.popBackStack()
                }
            }
        )
    }
}
