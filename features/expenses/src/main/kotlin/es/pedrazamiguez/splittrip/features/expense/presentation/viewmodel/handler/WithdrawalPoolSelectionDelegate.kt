package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetAvailableWithdrawalPoolsUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Delegate that handles withdrawal-pool discovery and user pool selection for CASH expenses.
 *
 * Extracted from [CurrencyEventHandler] to stay within the 600-line production-file limit
 * enforced by the Konsist architecture test. This is a plain class (NOT an [AddExpenseEventHandler])
 * — it does NOT implement `bind()` and is NOT wired into the `init` block of [AddExpenseViewModel].
 * Instead, it is composed directly into [CurrencyEventHandler] following the same lambda-based
 * state-access pattern as [AddOnExchangeRateDelegate].
 *
 * **Responsibilities:**
 * - Probing available withdrawal pools via [GetAvailableWithdrawalPoolsUseCase].
 * - Auto-selecting the pool when only one is available (no UI shown).
 * - Updating [AddExpenseUiState.availableWithdrawalPools] and [AddExpenseUiState.selectedWithdrawalPool].
 * - Invoking [onPoolResolved] after pool state changes so [CurrencyEventHandler] can re-fetch
 *   the cash rate preview with the newly selected pool's scope.
 */
class WithdrawalPoolSelectionDelegate(
    private val getAvailableWithdrawalPoolsUseCase: GetAvailableWithdrawalPoolsUseCase,
    private val addExpenseOptionsMapper: AddExpenseOptionsUiMapper
) {

    /**
     * Queries available withdrawal pools for [groupId] / [currency] / [payerType] and updates
     * [stateFlow] accordingly.
     *
     * - When no pools have funds: clears pool state and invokes [onPoolResolved] so the rate
     *   preview refreshes and returns [CashRatePreviewResult.NoWithdrawals] or
     *   [CashRatePreviewResult.InsufficientCash] (preserves existing "—" placeholder UX).
     * - When exactly one pool has funds: auto-populates [AddExpenseUiState.selectedWithdrawalPool]
     *   silently and invokes [onPoolResolved] so the rate preview is refreshed.
     * - When multiple pools have funds: populates [AddExpenseUiState.availableWithdrawalPools]
     *   for the UI to render the pool-selection widget; pre-selects the **first pool in the list**
     *   (which follows the priority order documented in [GetAvailableWithdrawalPoolsUseCase] —
     *   GROUP for GROUP payerType, personal/subunit pool for USER/SUBUNIT payerType) and immediately
     *   invokes [onPoolResolved] so the tranche preview loads without requiring user action.
     *
     * For GROUP payer type, also probes the personal (USER-scoped) pool when a [payerId]
     * is provided, surfacing it as a supplement when the GROUP pool is insufficient.
     */
    fun fetchPools(
        groupId: String,
        currency: String,
        payerType: PayerType,
        payerId: String?,
        scope: CoroutineScope,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        onPoolResolved: () -> Unit
    ) {
        val subunitOptions = stateFlow.value.contributionSubunitOptions
        val subunitNameLookup = subunitOptions.associate { it.id to it.name }
        val subunitIds = subunitOptions.map { it.id }

        scope.launch {
            try {
                val pools = getAvailableWithdrawalPoolsUseCase(
                    groupId = groupId,
                    currency = currency,
                    payerType = payerType,
                    payerId = payerId,
                    subunitIds = subunitIds
                )

                val uiPools = addExpenseOptionsMapper.mapWithdrawalPoolOptions(
                    pools = pools,
                    subunitNameLookup = subunitNameLookup
                )

                when {
                    uiPools.isEmpty() -> {
                        clearPoolState(stateFlow)
                        // Invoke callback even with no pools so the rate preview refreshes
                        // and surfaces NoWithdrawals / InsufficientCash to the user.
                        onPoolResolved()
                    }

                    uiPools.size == 1 -> {
                        stateFlow.update { state ->
                            state.copy(
                                availableWithdrawalPools = persistentListOf(),
                                selectedWithdrawalPool = uiPools.first()
                            )
                        }
                        onPoolResolved()
                    }

                    else -> {
                        // Pre-select the first pool — the use case returns pools in priority order:
                        // GROUP first for GROUP payerType; personal/subunit first for USER/SUBUNIT.
                        // This matches the documented FIFO priority chain while saving the user a tap.
                        val defaultPool = uiPools.first()
                        stateFlow.update { state ->
                            state.copy(
                                availableWithdrawalPools = uiPools,
                                selectedWithdrawalPool = defaultPool
                            )
                        }
                        onPoolResolved()
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch available withdrawal pools")
                clearPoolState(stateFlow)
            }
        }
    }

    /**
     * Applies the user's explicit pool selection and invokes [onPoolResolved] so the caller
     * can re-fetch the cash rate preview for the newly selected pool.
     */
    fun handlePoolSelected(
        poolScope: PayerType,
        scopeOwnerId: String?,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        onPoolResolved: () -> Unit
    ) {
        val currentPools = stateFlow.value.availableWithdrawalPools
        val selectedPool = currentPools.find { pool ->
            pool.scope == poolScope && pool.ownerId == scopeOwnerId
        } ?: return

        stateFlow.update { state -> state.copy(selectedWithdrawalPool = selectedPool) }
        onPoolResolved()
    }

    /**
     * Clears pool discovery and selection state.
     * Called when the expense is not CASH, the funding source switches to GROUP,
     * or no pools have available funds.
     */
    fun clearPoolState(stateFlow: MutableStateFlow<AddExpenseUiState>) {
        stateFlow.update { state ->
            state.copy(
                availableWithdrawalPools = persistentListOf(),
                selectedWithdrawalPool = null
            )
        }
    }
}
