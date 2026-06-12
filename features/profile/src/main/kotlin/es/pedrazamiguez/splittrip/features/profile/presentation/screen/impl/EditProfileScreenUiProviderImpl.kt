package es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.profile.R

class EditProfileScreenUiProviderImpl(override val route: String = Routes.EDIT_PROFILE) : ScreenUiProvider {

    override val topBar: @Composable () -> Unit = {
        val tabNavController = LocalTabNavController.current
        DynamicTopAppBar(
            title = stringResource(R.string.edit_profile_title),
            onBack = { tabNavController.popBackStack() }
        )
    }
}
