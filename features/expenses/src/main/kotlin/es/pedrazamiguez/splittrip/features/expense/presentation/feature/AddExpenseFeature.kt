package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.AddExpenseScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.AddExpenseViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddExpenseFeature(
    addExpenseViewModel: AddExpenseViewModel = koinViewModel<AddExpenseViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    onAddExpenseSuccess: () -> Unit = {}
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val navController = LocalTabNavController.current

    val state by addExpenseViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId = sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    // Tracks the pending conflict-resolution action; non-null while the sheet is visible.
    var conflictResolution by remember {
        mutableStateOf<AddExpenseUiAction.ShowCashConflictResolution?>(null)
    }

    // Intercept system back — delegate to wizard navigation
    BackHandler {
        addExpenseViewModel.onEvent(AddExpenseUiEvent.PreviousStep)
    }

    // Collect side-effect actions and route them appropriately.
    LaunchedEffect(Unit) {
        addExpenseViewModel.actions.collectLatest { action ->
            when (action) {
                is AddExpenseUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                is AddExpenseUiAction.ShowCashConflictResolution -> {
                    // Refresh the tranche preview with the latest Room data immediately,
                    // then surface the guided resolution sheet to the user.
                    addExpenseViewModel.refreshCashPreview()
                    conflictResolution = action
                }

                AddExpenseUiAction.NavigateBack -> {
                    navController.popBackStack()
                }

                AddExpenseUiAction.None -> Unit
            }
        }
    }

    // Conflict-resolution bottom sheet — shown when a cash conflict is detected.
    conflictResolution?.let { resolution ->
        val availableAmountDisplay = resolution.availableAmountDisplay

        ActionBottomSheet(
            title = stringResource(R.string.add_expense_cash_conflict_resolution_title),
            icon = TablerIcons.Outline.AlertTriangle,
            actions = buildList {
                // "Use remaining cash" — only when we know the available amount.
                if (availableAmountDisplay != null && resolution.availableAmountForInput != null) {
                    add(
                        SheetAction(
                            text = stringResource(
                                R.string.add_expense_cash_conflict_use_remaining,
                                availableAmountDisplay
                            ),
                            icon = TablerIcons.Outline.Cash,
                            onClick = {
                                addExpenseViewModel.onEvent(
                                    AddExpenseUiEvent.ResolutionAmountSelected(
                                        resolution.availableAmountForInput
                                    )
                                )
                                conflictResolution = null
                            }
                        )
                    )
                }
                // "Switch payment method" — jump back to the Payment Method step.
                add(
                    SheetAction(
                        text = stringResource(R.string.add_expense_cash_conflict_switch_payment),
                        icon = TablerIcons.Outline.CreditCard,
                        onClick = {
                            val idx = state.applicableSteps.indexOf(AddExpenseStep.PAYMENT_METHOD)
                            if (idx >= 0) {
                                addExpenseViewModel.onEvent(AddExpenseUiEvent.JumpToStep(idx))
                            }
                            conflictResolution = null
                        }
                    )
                )
                // "Dismiss and review" — close the sheet; preview was already refreshed.
                add(
                    SheetAction(
                        text = stringResource(R.string.add_expense_cash_conflict_dismiss),
                        icon = TablerIcons.Outline.X,
                        onClick = { conflictResolution = null }
                    )
                )
            },
            onDismiss = { conflictResolution = null }
        )
    }

    AddExpenseScreen(
        groupId = selectedGroupId.value,
        uiState = state,
        onEvent = { event ->
            addExpenseViewModel.onEvent(
                event,
                onAddExpenseSuccess
            )
        }
    )
}
