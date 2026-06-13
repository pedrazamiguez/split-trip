package es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.balance.R

class BalancesScreenUiProviderImpl(
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    override val route: String = Routes.BALANCES
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val profile by observeCurrentUserProfileUseCase().collectAsStateWithLifecycle(initialValue = null)

        DynamicTopAppBar(
            title = stringResource(R.string.balances_title),
            actions = {
                ProfileAvatarButton(
                    avatarUrl = profile?.profileImagePath,
                    onClick = { rootNavController.navigate(Routes.PROFILE) }
                )
            }
        )
    }
}
