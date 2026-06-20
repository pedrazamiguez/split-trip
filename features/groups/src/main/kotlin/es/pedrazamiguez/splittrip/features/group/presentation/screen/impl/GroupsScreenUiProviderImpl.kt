package es.pedrazamiguez.splittrip.features.group.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CREATE_GROUP_SHARED_ELEMENT_KEY

class GroupsScreenUiProviderImpl(
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    override val route: String = Routes.GROUPS
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val profile by observeCurrentUserProfileUseCase().collectAsStateWithLifecycle(initialValue = null)

        DynamicTopAppBar(
            title = stringResource(R.string.groups_title),
            subtitle = stringResource(R.string.groups_subtitle),
            actions = {
                ProfileAvatarButton(
                    avatarUrl = profile?.profileImagePath,
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
                sharedTransitionKey = CREATE_GROUP_SHARED_ELEMENT_KEY
            )
        }
}
