package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.expense.AddExpenseUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Strategy implementation for creating a new expense.
 *
 * Configures the screen with creation-related titles and submit labels,
 * initiates config loading, and delegates save calls to the AddExpenseUseCase.
 */
class AddExpenseFlowStrategy(
    private val configEventHandler: ConfigEventHandler,
    private val addExpenseUseCase: AddExpenseUseCase
) : ExpenseFlowStrategy {

    override val screenTitleRes: Int = R.string.add_expense_title
    override val submitLabelRes: Int = R.string.add_expense_submit_button
    override val isEditMode: Boolean = false
    override val startStep: AddExpenseStep = AddExpenseStep.TITLE

    override fun loadInitialData(
        groupId: String?,
        uiState: MutableStateFlow<AddExpenseUiState>,
        actions: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope,
        forceRefresh: Boolean,
        onConfigLoaded: suspend () -> Unit
    ) {
        if (groupId == null) return
        scope.launch {
            configEventHandler.suspendLoadGroupConfig(groupId, forceRefresh)
            onConfigLoaded()
        }
    }

    override suspend fun saveExpense(
        groupId: String?,
        expense: Expense,
        uiState: AddExpenseUiState
    ): Result<Unit> {
        val pairedSubunitId = if (uiState.contributionScope ==
            es.pedrazamiguez.splittrip.domain.enums.PayerType.SUBUNIT
        ) {
            uiState.selectedContributionSubunitId
        } else {
            null
        }
        return addExpenseUseCase(
            groupId = groupId,
            expense = expense,
            pairedContributionScope = uiState.contributionScope,
            pairedSubunitId = pairedSubunitId,
            preferredWithdrawalScope = uiState.selectedWithdrawalPool?.scope,
            preferredWithdrawalOwnerId = uiState.selectedWithdrawalPool?.ownerId
        )
    }
}
