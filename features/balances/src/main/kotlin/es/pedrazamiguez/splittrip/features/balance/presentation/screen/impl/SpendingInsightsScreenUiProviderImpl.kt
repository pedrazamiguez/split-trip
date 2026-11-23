package es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Share
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.navigation.KEY_EXPORT_REPORT_REQUESTED
import org.koin.androidx.compose.koinViewModel

class SpendingInsightsScreenUiProviderImpl(
    override val route: String = Routes.SPENDING_INSIGHTS
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val tabNavController = LocalTabNavController.current
        val sharedViewModel: SharedViewModel = koinViewModel(
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
        )
        val groupName by sharedViewModel.selectedGroupName.collectAsStateWithLifecycle()
        val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
        val insightsBackStackEntry = remember(currentBackStackEntry, tabNavController) {
            runCatching { tabNavController.getBackStackEntry(Routes.SPENDING_INSIGHTS) }.getOrNull()
        }
        val onExportClick = debounced {
            insightsBackStackEntry?.savedStateHandle?.set(KEY_EXPORT_REPORT_REQUESTED, true)
        }

        DynamicTopAppBar(
            title = stringResource(R.string.balances_insights_title),
            subtitle = groupName?.takeIf { it.isNotBlank() },
            onBack = { tabNavController.navigateUp() },
            actions = {
                IconButton(onClick = onExportClick) {
                    Icon(
                        imageVector = TablerIcons.Outline.Share,
                        contentDescription = stringResource(R.string.balances_export_action_cd)
                    )
                }
            }
        )
    }

    override val mainAction: MainAction?
        @Composable
        get() = null
}
