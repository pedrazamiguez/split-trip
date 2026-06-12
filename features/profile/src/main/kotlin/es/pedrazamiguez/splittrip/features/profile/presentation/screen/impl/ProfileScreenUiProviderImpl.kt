package es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Settings
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.profile.R

class ProfileScreenUiProviderImpl(override val route: String = Routes.PROFILE) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalRootNavController.current
        val tabNavController = LocalTabNavController.current
        DynamicTopAppBar(
            title = stringResource(R.string.profile_title),
            subtitle = stringResource(R.string.profile_subtitle),
            actions = {
                IconButton(onClick = {
                    tabNavController.navigate(Routes.EDIT_PROFILE)
                }) {
                    Icon(
                        imageVector = TablerIcons.Outline.Edit,
                        contentDescription = stringResource(R.string.profile_edit)
                    )
                }
                IconButton(onClick = {
                    navController.navigate(Routes.SETTINGS)
                }) {
                    Icon(
                        imageVector = TablerIcons.Outline.Settings,
                        contentDescription = stringResource(R.string.profile_settings)
                    )
                }
            }
        )
    }
}
