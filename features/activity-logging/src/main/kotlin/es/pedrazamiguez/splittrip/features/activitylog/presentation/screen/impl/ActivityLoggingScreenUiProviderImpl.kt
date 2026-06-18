package es.pedrazamiguez.splittrip.features.activitylog.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AdjustmentsHorizontal
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.activitylog.R

class ActivityLoggingScreenUiProviderImpl(
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    override val route: String = Routes.ACTIVITY_LOGGING
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val profile by observeCurrentUserProfileUseCase().collectAsStateWithLifecycle(initialValue = null)

        DynamicTopAppBar(
            title = stringResource(R.string.activity_logging_title),
            subtitle = stringResource(R.string.activity_logging_subtitle),
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
        get() = MainAction(
            icon = TablerIcons.Outline.AdjustmentsHorizontal,
            // TODO: placeholder — wire to date/filter sheet when activity log feature is fully built
            contentDescription = stringResource(R.string.activity_logging_filter),
            onClick = {}
        )
}
