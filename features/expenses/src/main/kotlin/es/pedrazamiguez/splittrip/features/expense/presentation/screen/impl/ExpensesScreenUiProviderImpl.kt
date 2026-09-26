package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BasketPlus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.LocalProfileAvatarUrl
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ADD_EXPENSE_SHARED_ELEMENT_KEY
import org.koin.androidx.compose.koinViewModel

class ExpensesScreenUiProviderImpl(
    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase,
    override val route: String = Routes.EXPENSES
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val avatarUrl = LocalProfileAvatarUrl.current
        val sharedViewModel: SharedViewModel = koinViewModel(
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
        )
        val groupName by sharedViewModel.selectedGroupName.collectAsStateWithLifecycle()

        DynamicTopAppBar(
            title = stringResource(R.string.expenses_title),
            subtitle = resolveSubtitle(groupName),
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
            val selectedGroup by observeSelectedGroupUseCase().collectAsStateWithLifecycle(initialValue = null)
            val tabNavController = LocalTabNavController.current
            return MainAction(
                icon = TablerIcons.Outline.BasketPlus,
                contentDescription = stringResource(R.string.expenses_add),
                onClick = { tabNavController.navigate(Routes.ADD_EXPENSE) },
                sharedTransitionKey = ADD_EXPENSE_SHARED_ELEMENT_KEY,
                enabled = isMainActionEnabled(selectedGroup?.status)
            )
        }

    internal fun resolveSubtitle(groupName: String?): String? =
        groupName?.takeIf { it.isNotBlank() }

    internal fun isMainActionEnabled(status: GroupStatus?): Boolean =
        status != GroupStatus.ARCHIVED
}
