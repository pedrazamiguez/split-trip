package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.LanguageScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.LanguageViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun LanguageFeature(viewModel: LanguageViewModel = koinViewModel()) {
    val selectedLanguage by viewModel.selectedLanguageCode.collectAsStateWithLifecycle()

    val navController = LocalRootNavController.current
    val scope = rememberCoroutineScope()

    FeatureScaffold(currentRoute = Routes.SETTINGS_LANGUAGE) {
        LanguageScreen(
            availableLanguages = viewModel.availableLanguages,
            selectedLanguageCode = selectedLanguage,
            onLanguageSelected = { newLanguageCode ->
                if (newLanguageCode != selectedLanguage) {
                    viewModel.onLanguageSelected(newLanguageCode)
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        androidx.core.os.LocaleListCompat.forLanguageTags(newLanguageCode)
                    )
                    scope.launch {
                        delay(UiConstants.NAV_FEEDBACK_DELAY)
                        navController.popBackStack()
                    }
                } else {
                    scope.launch {
                        delay(UiConstants.NAV_FEEDBACK_DELAY)
                        navController.popBackStack()
                    }
                }
            }
        )
    }
}
