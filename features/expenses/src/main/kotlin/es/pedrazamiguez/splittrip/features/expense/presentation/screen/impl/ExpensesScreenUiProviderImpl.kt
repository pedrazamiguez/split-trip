package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BasketPlus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartDonut
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
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.navigation.KEY_INITIAL_FILTER_CRITERIA
import es.pedrazamiguez.splittrip.features.expense.navigation.KEY_RESET_FILTERS_REQUESTED
import es.pedrazamiguez.splittrip.features.expense.presentation.component.dialog.ResetFiltersConfirmationDialog
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpensesFilterActionButton
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ADD_EXPENSE_SHARED_ELEMENT_KEY
import org.koin.androidx.compose.koinViewModel

class ExpensesScreenUiProviderImpl(
    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase,
    override val route: String = Routes.EXPENSES
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val tabNavController = LocalTabNavController.current
        val avatarUrl = LocalProfileAvatarUrl.current
        val sharedViewModel: SharedViewModel = koinViewModel(
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
        )
        val groupName by sharedViewModel.selectedGroupName.collectAsStateWithLifecycle()
        val currentBackStackEntry by tabNavController.currentBackStackEntryAsState()
        val expensesBackStackEntry = remember(currentBackStackEntry, tabNavController) {
            runCatching { tabNavController.getBackStackEntry(Routes.EXPENSES) }.getOrNull()
        }
        val filterCriteriaFlow = remember(expensesBackStackEntry) {
            expensesBackStackEntry?.savedStateHandle
                ?.getStateFlow(KEY_INITIAL_FILTER_CRITERIA, ExpenseFilterCriteria())
        }
        val filterCriteria by filterCriteriaFlow?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf(ExpenseFilterCriteria()) }
        val activeFilterCount = resolveActiveFilterCount(filterCriteria)
        var showResetFiltersDialog by remember { mutableStateOf(false) }

        DynamicTopAppBar(
            title = stringResource(R.string.expenses_title),
            subtitle = resolveSubtitle(groupName),
            actions = {
                ExpensesFilterActionButton(
                    activeFilterCount = activeFilterCount,
                    onClick = {
                        tabNavController.navigate(Routes.expensesFilterRoute()) {
                            launchSingleTop = true
                        }
                    },
                    onLongClick = {
                        if (canResetFilters(activeFilterCount)) {
                            showResetFiltersDialog = true
                        }
                    }
                )
                val onCategorySpendingClick = debounced {
                    tabNavController.navigate(Routes.CATEGORY_SPENDING) {
                        launchSingleTop = true
                    }
                }
                IconButton(onClick = onCategorySpendingClick) {
                    Icon(
                        imageVector = TablerIcons.Outline.ChartDonut,
                        contentDescription = stringResource(R.string.expenses_category_spending_title)
                    )
                }
                ProfileAvatarButton(
                    avatarUrl = avatarUrl,
                    onClick = { rootNavController.navigate(Routes.PROFILE) }
                )
            }
        )

        if (showResetFiltersDialog) {
            ResetFiltersConfirmationDialog(
                onDismiss = { showResetFiltersDialog = false },
                onConfirm = {
                    expensesBackStackEntry?.savedStateHandle?.set(KEY_RESET_FILTERS_REQUESTED, true)
                    showResetFiltersDialog = false
                }
            )
        }
    }

    override val mainAction: MainAction?
        @Composable
        get() {
            val selectedGroup by observeSelectedGroupUseCase().collectAsStateWithLifecycle(initialValue = null)
            if (!shouldShowMainAction(selectedGroup?.status)) return null

            val tabNavController = LocalTabNavController.current
            return MainAction(
                icon = TablerIcons.Outline.BasketPlus,
                contentDescription = stringResource(R.string.expenses_add),
                onClick = { tabNavController.navigate(Routes.ADD_EXPENSE) },
                sharedTransitionKey = ADD_EXPENSE_SHARED_ELEMENT_KEY
            )
        }

    internal fun resolveSubtitle(groupName: String?): String? =
        groupName?.takeIf { it.isNotBlank() }

    internal fun shouldShowMainAction(status: GroupStatus?): Boolean =
        status != GroupStatus.ARCHIVED

    internal fun resolveActiveFilterCount(criteria: ExpenseFilterCriteria?): Int =
        criteria?.activeFilterCount ?: 0

    internal fun canResetFilters(activeFilterCount: Int): Boolean =
        activeFilterCount > 0
}
