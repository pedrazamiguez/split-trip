package es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.settings.R

class LanguageScreenUiProviderImpl(override val route: String = Routes.SETTINGS_LANGUAGE) :
    ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalRootNavController.current
        DynamicTopAppBar(
            title = stringResource(R.string.settings_preferences_language_dialog_title),
            onBack = { navController.popBackStack() }
        )
    }
}
