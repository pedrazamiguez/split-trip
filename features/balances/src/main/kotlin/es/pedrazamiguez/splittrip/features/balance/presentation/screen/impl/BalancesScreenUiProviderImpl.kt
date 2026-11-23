package es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartBar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReportAnalytics
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Share
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.LocalProfileAvatarUrl
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.navigation.KEY_EXPORT_REPORT_REQUESTED

class BalancesScreenUiProviderImpl(
    override val route: String = Routes.BALANCES
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val tabNavController = LocalTabNavController.current
        val avatarUrl = LocalProfileAvatarUrl.current
        val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
        val balancesBackStackEntry = remember(currentBackStackEntry, tabNavController) {
            runCatching { tabNavController.getBackStackEntry(Routes.BALANCES) }.getOrNull()
                ?: currentBackStackEntry
        }
        val onExportClick = debounced {
            balancesBackStackEntry?.savedStateHandle?.set(KEY_EXPORT_REPORT_REQUESTED, true)
        }
        val onInsightsClick = debounced {
            tabNavController.navigate(Routes.SPENDING_INSIGHTS) {
                launchSingleTop = true
            }
        }

        DynamicTopAppBar(
            title = stringResource(R.string.balances_title),
            subtitle = stringResource(R.string.balances_subtitle),
            actions = {
                IconButton(onClick = onExportClick) {
                    Icon(
                        imageVector = TablerIcons.Outline.Share,
                        contentDescription = stringResource(R.string.balances_export_action_cd)
                    )
                }
                IconButton(onClick = onInsightsClick) {
                    Icon(
                        imageVector = TablerIcons.Outline.ChartBar,
                        contentDescription = stringResource(R.string.balances_insights_action_cd)
                    )
                }
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
                icon = TablerIcons.Outline.ReportAnalytics,
                contentDescription = stringResource(R.string.balances_your_balance_title),
                onClick = { tabNavController.navigate(Routes.YOUR_BALANCE) },
                sharedTransitionKey = SharedElementKeys.YOUR_BALANCE
            )
        }
}
