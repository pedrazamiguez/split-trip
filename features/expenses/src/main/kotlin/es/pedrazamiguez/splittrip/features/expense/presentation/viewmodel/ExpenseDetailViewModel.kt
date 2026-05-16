package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseDetailUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpenseDetailUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpenseDetailUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Expense Detail screen.
 *
 * Mirrors the `_groupId`-gated `flatMapLatest + stateIn` pattern used in
 * [GroupDetailViewModel]: the expense ID is pushed via [setExpenseId], which
 * gates the reactive load. Deletion is handled by [DeleteExpenseUseCase] and
 * reported as a [ExpenseDetailUiAction.DeleteSuccess] side-effect so the
 * Feature can navigate back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModel(
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase,
    private val getGroupSubunitsUseCase: GetGroupSubunitsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val authenticationService: AuthenticationService,
    private val expenseDetailUiMapper: ExpenseDetailUiMapper
) : ViewModel() {

    private val _expenseId = MutableStateFlow("")

    private val _actions = Channel<ExpenseDetailUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<ExpenseDetailUiState> = _expenseId
        .filter { it.isNotBlank() }
        .flatMapLatest { expenseId ->
            val expense = try {
                getExpenseByIdUseCase(expenseId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load expense: $expenseId")
                null
            }

            if (expense == null) {
                return@flatMapLatest flowOf(
                    ExpenseDetailUiState(isLoading = false, hasError = true)
                )
            }

            val allUserIds = buildSet {
                add(expense.createdBy)
                expense.payerId?.let { add(it) }
                expense.splits.forEach { add(it.userId) }
            }.toList()

            val memberProfiles = try {
                getMemberProfilesUseCase(allUserIds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch member profiles for expense $expenseId")
                emptyMap()
            }

            val currentUserId = authenticationService.currentUserId()

            val withdrawalLookup = if (expense.cashTranches.isNotEmpty()) {
                val withdrawalIds = expense.cashTranches.map { it.withdrawalId }.toSet()
                try {
                    getCashWithdrawalsFlowUseCase(expense.groupId)
                        .first()
                        .filter { it.id in withdrawalIds }
                        .associateBy { it.id }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch withdrawals for expense $expenseId")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val subunitNameLookup = if (withdrawalLookup.values.any {
                    it.withdrawalScope == PayerType.SUBUNIT && !it.subunitId.isNullOrBlank()
                }
            ) {
                try {
                    getGroupSubunitsUseCase(expense.groupId)
                        .associate { it.id to it.name }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch subunits for expense $expenseId")
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val uiModel = expenseDetailUiMapper.map(
                expense = expense,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                withdrawalLookup = withdrawalLookup,
                subunitNameLookup = subunitNameLookup
            )

            flowOf(ExpenseDetailUiState(expense = uiModel, isLoading = false))
        }
        .catch { e ->
            Timber.e(e, "Fatal error in ExpenseDetailViewModel flow")
            _actions.send(
                ExpenseDetailUiAction.ShowError(
                    UiText.StringResource(R.string.expense_detail_error_loading)
                )
            )
            emit(ExpenseDetailUiState(isLoading = false, hasError = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = ExpenseDetailUiState()
        )

    fun setExpenseId(expenseId: String) {
        if (expenseId != _expenseId.value) {
            _expenseId.value = expenseId
        }
    }

    fun onEvent(event: ExpenseDetailUiEvent) {
        when (event) {
            ExpenseDetailUiEvent.DeleteConfirmed -> handleDelete()
        }
    }

    private fun handleDelete() {
        val expense = uiState.value.expense ?: return
        viewModelScope.launch {
            try {
                deleteExpenseUseCase(expense.groupId, expense.id)
                _actions.send(
                    ExpenseDetailUiAction.DeleteSuccess(
                        UiText.StringResource(R.string.expense_deleted_successfully)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete expense: ${expense.id}")
                _actions.send(
                    ExpenseDetailUiAction.ShowError(
                        UiText.StringResource(R.string.error_deleting_expense)
                    )
                )
            }
        }
    }
}
