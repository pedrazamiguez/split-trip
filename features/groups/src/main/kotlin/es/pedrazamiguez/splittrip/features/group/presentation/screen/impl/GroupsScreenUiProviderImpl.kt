package es.pedrazamiguez.splittrip.features.group.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.LocalProfileAvatarUrl
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CREATE_EDIT_GROUP_SHARED_ELEMENT_KEY

class GroupsScreenUiProviderImpl(
    override val route: String = Routes.GROUPS
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val avatarUrl = LocalProfileAvatarUrl.current

        DynamicTopAppBar(
            title = stringResource(R.string.groups_title),
            subtitle = stringResource(R.string.groups_subtitle),
            actions = {
                ProfileAvatarButton(
                    avatarUrl = avatarUrl,
                    onClick = { rootNavController.navigate(Routes.PROFILE) }
                )
            }
        )
    }

    override val mainAction: MainAction?
        @Composable
        get() {
            val tabNavController = LocalTabNavController.current
            return MainAction(
                icon = TablerIcons.Outline.UsersPlus,
                contentDescription = stringResource(R.string.groups_create),
                onClick = { tabNavController.navigate(Routes.CREATE_GROUP) },
                sharedTransitionKey = CREATE_EDIT_GROUP_SHARED_ELEMENT_KEY
            )
        }
}
