package es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.EditProfileViewModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.EditProfileUiEvent
import org.koin.androidx.compose.koinViewModel

class EditProfileScreenUiProviderImpl(override val route: String = Routes.EDIT_PROFILE) : ScreenUiProvider {

    override val topBar: @Composable () -> Unit = {
        val navController = LocalRootNavController.current
        val backStackEntry = navController.currentBackStackEntry
        val vm: EditProfileViewModel? = backStackEntry?.let {
            koinViewModel(viewModelStoreOwner = it)
        }

        DynamicTopAppBar(
            title = stringResource(R.string.edit_profile_title),
            onBack = { navController.popBackStack() },
            actions = {
                if (vm != null) {
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    if (uiState.isSaving) {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current
                            )
                        }
                    } else {
                        val isSaveEnabled = uiState.isSaveEnabled
                        val debouncedSave = debounced {
                            vm.onEvent(EditProfileUiEvent.OnSaveClicked)
                        }
                        IconButton(
                            onClick = debouncedSave,
                            enabled = isSaveEnabled
                        ) {
                            Icon(
                                imageVector = TablerIcons.Outline.Check,
                                contentDescription = stringResource(R.string.edit_profile_save),
                                tint = if (isSaveEnabled) {
                                    LocalContentColor.current
                                } else {
                                    LocalContentColor.current.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}
