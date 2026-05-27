package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Strategy implementation for editing an existing expense.
 *
 * Configures the screen with edit-related titles and submit labels,
 * initiates config loading followed by fetching the target expense & contribution,
 * and delegates save calls to the UpdateExpenseUseCase.
 */
class EditExpenseFlowStrategy(
    private val expenseId: String,
    private val configEventHandler: ConfigEventHandler,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getContributionByExpenseIdUseCase: GetContributionByExpenseIdUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val addExpenseUiMapper: AddExpenseUiMapper,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val getGroupSubunitsUseCase: GetGroupSubunitsUseCase
) : ExpenseFlowStrategy {

    override val screenTitleRes: Int = R.string.edit_expense_title
    override val submitLabelRes: Int = R.string.edit_expense_submit_button
    override val isEditMode: Boolean = true
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
            try {
                uiState.update { it.copy(isLoading = true, configLoadFailed = false) }

                // 1. Load group config via ConfigEventHandler
                configEventHandler.suspendLoadGroupConfig(groupId, forceRefresh)

                // 2. Fetch the target expense
                val expense = getExpenseByIdUseCase(expenseId)
                if (expense == null) {
                    Timber.e("Expense with ID %s not found in group %s", expenseId, groupId)
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            configLoadFailed = true
                        )
                    }
                    return@launch
                }

                // 3. Fetch paired contribution if it exists
                val contribution = getContributionByExpenseIdUseCase(groupId, expenseId)

                // 4. Fetch additional details needed for mapper (member profiles and subunits)
                val memberProfiles = getMemberProfilesUseCase(expense.splits.map { it.userId })
                val subunits = getGroupSubunitsUseCase(groupId)

                // 5. Map everything back to UI State
                uiState.update { state ->
                    val mappedState = addExpenseUiMapper.mapExpenseToState(
                        expense = expense,
                        contribution = contribution,
                        currentState = state,
                        memberProfiles = memberProfiles,
                        subunits = subunits
                    )
                    // Skip AI scan step completely: force start step to TITLE
                    mappedState.copy(
                        currentStep = AddExpenseStep.TITLE,
                        isConfigLoaded = true,
                        isLoading = false
                    )
                }

                onConfigLoaded()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load initial data for editing expense: $expenseId")
                uiState.update {
                    it.copy(
                        isLoading = false,
                        configLoadFailed = true
                    )
                }
            }
        }
    }

    override suspend fun saveExpense(
        groupId: String?,
        expense: Expense,
        uiState: AddExpenseUiState
    ): Result<Unit> {
        val expenseWithId = expense.copy(id = expenseId)

        val pairedSubunitId = if (uiState.contributionScope == PayerType.SUBUNIT) {
            uiState.selectedContributionSubunitId
        } else {
            null
        }
        return updateExpenseUseCase(
            groupId = groupId,
            expense = expenseWithId,
            pairedContributionScope = uiState.contributionScope,
            pairedSubunitId = pairedSubunitId,
            preferredWithdrawalScope = uiState.selectedWithdrawalPool?.scope,
            preferredWithdrawalOwnerId = uiState.selectedWithdrawalPool?.ownerId
        )
    }
}
