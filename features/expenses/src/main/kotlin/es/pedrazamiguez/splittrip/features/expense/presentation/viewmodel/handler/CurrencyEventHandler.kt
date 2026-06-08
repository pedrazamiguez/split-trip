package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles currency selection and exchange rate events:
 * [CurrencySelected], [ExchangeRateChanged], [GroupAmountChanged].
 *
 * Also exposes [fetchRate], [fetchCashRate], [recalculateForward], and [recalculateCashForward]
 * for cross-handler calls (e.g., from [ConfigEventHandler] after initial config load).
 *
 * CASH-specific async operations (rate fetching, result mapping, debounce) are delegated
 * to [CashRateDelegate] to stay within the 600-line production-file limit.
 */
// Function count driven by event/action categories (CASH, non-CASH, funding source);
// extracting further would require additional Delegate sub-patterns
@Suppress("TooManyFunctions")
class CurrencyEventHandler(
    private val getExchangeRateUseCase: GetExchangeRateUseCase,
    private val exchangeRateCalculationService: ExchangeRateCalculationService,
    private val formattingHelper: FormattingHelper,
    private val addExpenseOptionsMapper: AddExpenseOptionsUiMapper,
    private val withdrawalPoolSelectionDelegate: WithdrawalPoolSelectionDelegate,
    private val cashRateDelegate: CashRateDelegate
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actions: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    /**
     * Fired after any automatic pool resolution (auto-select single pool, pre-select first of many).
     * Set by the ViewModel during initialization via [setOnPoolResolvedCallback].
     * Explicit user selections are handled directly in the ViewModel's [onEvent] router.
     */
    private var poolResolvedCallback: ((PayerType, String?) -> Unit)? = null

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
        cashRateDelegate.setup(stateFlow, scope)
    }

    fun handleCurrencySelected(currencyCode: String, onRecalculate: () -> Unit) {
        val currentState = _uiState.value
        val selectedUiModel = currentState.availableCurrencies
            .find { it.code == currencyCode } ?: return
        val isForeign = selectedUiModel.code != currentState.groupCurrency?.code

        val exchangeRateLabel = if (isForeign && currentState.groupCurrency != null) {
            addExpenseOptionsMapper.buildExchangeRateLabel(currentState.groupCurrency, selectedUiModel)
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                selectedCurrency = selectedUiModel,
                showExchangeRateSection = isForeign,
                exchangeRateLabel = exchangeRateLabel,
                // Clear the saved pre-CASH rate — it belongs to the previous currency pair
                preCashExchangeRate = null
            ).withStepClamped()
        }
        // If switching to foreign, fetch the appropriate rate; otherwise default to 1.0
        val isCash = isCashPaymentMethod()
        if (isForeign) {
            _uiState.update {
                it.copy(
                    displayExchangeRate = "",
                    isExchangeRateError = false
                )
            }
            if (isCash) {
                // Lock immediately so the fields are non-editable from the start,
                // before the async pool probe + rate fetch completes.
                _uiState.update {
                    it.copy(
                        isExchangeRateLocked = true,
                        isInsufficientCash = false,
                        exchangeRateLockedHint = UiText.StringResource(
                            R.string.add_expense_cash_rate_locked_hint
                        )
                    )
                }
                // Clear stale pool state from the previous currency pair, then re-probe.
                withdrawalPoolSelectionDelegate.clearPoolState(_uiState)
                fetchPoolsIfNeeded()
            } else {
                fetchRate()
            }
        } else {
            _uiState.update {
                it.copy(
                    displayExchangeRate = "1.0",
                    isExchangeRateLocked = false,
                    isInsufficientCash = false,
                    exchangeRateLockedHint = null,
                    isExchangeRateError = false
                )
            }
            // Same currency + CASH: probe pools and fetch tranche preview via callback.
            if (isCash) fetchPoolsIfNeeded()
        }
        recalculateForward()
        onRecalculate()
    }

    fun handleExchangeRateChanged(rate: String) {
        _uiState.update { it.copy(displayExchangeRate = rate, isExchangeRateError = false) }
        recalculateForward()
    }

    fun handleGroupAmountChanged(amount: String) {
        _uiState.update { it.copy(calculatedGroupAmount = amount, isExchangeRateError = false) }
        recalculateReverse()
    }

    /**
     * Calculates the group amount from source amount and display exchange rate.
     * Uses the user-friendly display rate (1 GroupCurrency = X SourceCurrency).
     * All BigDecimal operations are delegated to ExpenseCalculatorService.
     */
    fun recalculateForward() {
        val state = _uiState.value
        if (state.displayExchangeRate.isBlank()) {
            _uiState.update { it.copy(calculatedGroupAmount = "") }
            return
        }
        val sourceDecimalPlaces = state.selectedCurrency?.decimalDigits ?: 2
        val targetDecimalPlaces = state.groupCurrency?.decimalDigits ?: 2
        val calculatedAmount = exchangeRateCalculationService.calculateGroupAmountFromDisplayRate(
            sourceAmountString = state.sourceAmount,
            displayRateString = state.displayExchangeRate,
            sourceDecimalPlaces = sourceDecimalPlaces,
            targetDecimalPlaces = targetDecimalPlaces
        )
        // Format the amount for display using locale-aware formatting
        // Use currency's decimal digits as minimum to ensure proper display (e.g., "1,10" for EUR instead of "1,1")
        val formattedAmount = formattingHelper.formatForDisplay(
            internalValue = calculatedAmount,
            maxDecimalPlaces = targetDecimalPlaces,
            minDecimalPlaces = targetDecimalPlaces
        )
        _uiState.update { it.copy(calculatedGroupAmount = formattedAmount) }
    }

    /**
     * Calculates the implied display exchange rate from source and group amounts.
     * Returns the rate in user-friendly format (1 GroupCurrency = X SourceCurrency).
     * All BigDecimal operations are delegated to ExpenseCalculatorService.
     */
    private fun recalculateReverse() {
        val state = _uiState.value
        val sourceDecimalPlaces = state.selectedCurrency?.decimalDigits ?: 2
        val impliedDisplayRate = exchangeRateCalculationService.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = state.sourceAmount,
            groupAmountString = state.calculatedGroupAmount,
            sourceDecimalPlaces = sourceDecimalPlaces
        )
        // Format the rate for display using locale-aware formatting
        val formattedRate = formattingHelper.formatRateForDisplay(impliedDisplayRate)
        _uiState.update { it.copy(displayExchangeRate = formattedRate) }
    }

    fun fetchRate() {
        val state = _uiState.value
        val groupCurrency = state.groupCurrency
        val selectedCurrency = state.selectedCurrency

        if (groupCurrency == null || selectedCurrency == null || groupCurrency.code == selectedCurrency.code) {
            return
        }

        // Capture the requested pair so we can verify it before applying the result
        val requestedBaseCode = groupCurrency.code
        val requestedTargetCode = selectedCurrency.code

        scope.launch {
            _uiState.update { it.copy(isLoadingRate = true) }

            try {
                val rateResult = getExchangeRateUseCase(
                    baseCurrencyCode = requestedBaseCode,
                    targetCurrencyCode = requestedTargetCode
                )

                _uiState.update { current ->
                    current.updateRateResult(
                        requestedBaseCode = requestedBaseCode,
                        requestedTargetCode = requestedTargetCode,
                        rateResult = rateResult,
                        isError = rateResult == null
                    )
                }

                if (rateResult != null) {
                    recalculateForward()
                }
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Failed to fetch exchange rate for $requestedBaseCode -> $requestedTargetCode"
                )
                _uiState.update { current ->
                    current.updateRateResult(
                        requestedBaseCode = requestedBaseCode,
                        requestedTargetCode = requestedTargetCode,
                        rateResult = null,
                        isError = true
                    )
                }
            }
        }
    }

    /**
     * Reacts to the payment method changing between CASH and non-CASH.
     *
     * - CASH + foreign + GROUP pocket: saves current rate, locks fields, fetches ATM preview.
     * - CASH + same currency + GROUP pocket: no lock needed, fetches tranche preview only.
     * - CASH + foreign + USER/SUBUNIT pocket: rate stays unlocked (user enters manually).
     * - non-CASH or leaving CASH: cancels jobs, unlocks rate, restores pre-CASH rate if available.
     *
     * @param isCash true when the selected payment method is CASH.
     */
    fun handlePaymentMethodChanged(isCash: Boolean, isGroupPocket: Boolean = true) {
        val state = _uiState.value
        val isForeign = state.selectedCurrency?.code != state.groupCurrency?.code
        val wasCashLocked = state.isExchangeRateLocked
        if (isCash) {
            switchToCashPayment(isForeign, isGroupPocket)
        } else {
            switchFromCashPayment(isForeign, wasCashLocked, state.preCashExchangeRate)
        }
    }

    private fun switchToCashPayment(isForeign: Boolean, isGroupPocket: Boolean) {
        if (isForeign && isGroupPocket) {
            // GROUP pocket CASH: save current rate, lock fields, then probe pools.
            _uiState.update {
                it.copy(
                    preCashExchangeRate = it.displayExchangeRate,
                    isExchangeRateLocked = true,
                    isInsufficientCash = false,
                    exchangeRateLockedHint = UiText.StringResource(R.string.add_expense_cash_rate_locked_hint)
                )
            }
            withdrawalPoolSelectionDelegate.clearPoolState(_uiState)
            fetchPoolsIfNeeded()
        } else if (isForeign) {
            // USER/SUBUNIT cash: rate stays unlocked, user enters manually.
            fetchPoolsIfNeeded()
        } else if (isGroupPocket) {
            // Same-currency CASH + GROUP pocket: no lock, but probe pools for tranche preview.
            withdrawalPoolSelectionDelegate.clearPoolState(_uiState)
            fetchPoolsIfNeeded()
        }
    }

    private fun switchFromCashPayment(isForeign: Boolean, wasCashLocked: Boolean, savedRate: String?) {
        cashRateDelegate.cancelPendingCashJobs()
        withdrawalPoolSelectionDelegate.clearPoolState(_uiState)
        _uiState.update {
            it.copy(
                isExchangeRateLocked = false,
                isInsufficientCash = false,
                exchangeRateLockedHint = null,
                cashTranchePreviews = persistentListOf()
            )
        }
        if (isForeign && wasCashLocked) {
            // Transitioning OUT of locked CASH rate — restore the rate the user had before lock.
            if (savedRate != null) {
                _uiState.update { it.copy(displayExchangeRate = savedRate, preCashExchangeRate = null) }
                recalculateForward()
            } else {
                fetchRate()
            }
        }
    }

    /**
     * Reacts to the funding source changing between GROUP, USER, and SUBUNIT.
     * CASH + foreign + GROUP: locks the rate and fetches from the ATM pool.
     * CASH + foreign + USER/SUBUNIT: cancels the fetch, unlocks, restores pre-CASH rate.
     * CASH + same-currency + GROUP: fetches tranche preview (no rate lock needed).
     * CASH + same-currency + USER/SUBUNIT: clears tranche previews and insufficient state.
     * Non-CASH: no effect on the exchange rate.
     */
    fun handleFundingSourceChanged(isGroupPocket: Boolean) {
        if (!isCashPaymentMethod()) return
        val state = _uiState.value
        val isForeign = state.selectedCurrency?.code != state.groupCurrency?.code
        if (isGroupPocket) {
            switchToGroupPocketCash(isForeign, state)
        } else {
            switchToPersonalCash(isForeign, state)
        }
    }

    private fun switchToGroupPocketCash(isForeign: Boolean, state: AddExpenseUiState) {
        if (isForeign && !state.isExchangeRateLocked) {
            _uiState.update {
                it.copy(
                    preCashExchangeRate = it.displayExchangeRate,
                    isExchangeRateLocked = true,
                    isInsufficientCash = false,
                    exchangeRateLockedHint = UiText.StringResource(R.string.add_expense_cash_rate_locked_hint)
                )
            }
        }
        withdrawalPoolSelectionDelegate.clearPoolState(_uiState)
        fetchPoolsIfNeeded()
    }

    private fun switchToPersonalCash(isForeign: Boolean, state: AddExpenseUiState) {
        if (isForeign) {
            cashRateDelegate.cancelPendingCashJobs()
            val savedRate = state.preCashExchangeRate
            _uiState.update {
                it.copy(
                    isExchangeRateLocked = false,
                    isInsufficientCash = false,
                    exchangeRateLockedHint = null,
                    displayExchangeRate = savedRate ?: it.displayExchangeRate,
                    preCashExchangeRate = null,
                    cashTranchePreviews = persistentListOf()
                )
            }
            if (savedRate != null) recalculateForward()
            fetchPoolsIfNeeded()
        } else {
            // Same-currency USER/SUBUNIT pocket: clear tranche state — ATM pool is not used.
            cashRateDelegate.cancelPendingCashJobs()
            withdrawalPoolSelectionDelegate.clearPoolState(_uiState)
            _uiState.update { it.copy(isInsufficientCash = false, cashTranchePreviews = persistentListOf()) }
        }
    }

    /**
     * Registers a callback that fires after any automatic pool resolution (auto-select single pool
     * or pre-select first of many). The ViewModel wires this to call
     * [SplitEventHandler.applyPersonalPoolSplitDefault] with the resolved pool's scope and ownerId.
     */
    fun setOnPoolResolvedCallback(callback: (PayerType, String?) -> Unit) {
        poolResolvedCallback = callback
    }

    /** Thin wrapper — delegates to [CashRateDelegate.fetchCashRate]. */
    fun fetchCashRate() = cashRateDelegate.fetchCashRate()

    /** Thin wrapper — delegates to [CashRateDelegate.recalculateCashForward]. */
    fun recalculateCashForward() = cashRateDelegate.recalculateCashForward()

    /**
     * Handles the user's explicit pool selection from the pool-selection widget.
     * Delegates to [WithdrawalPoolSelectionDelegate], which updates [AddExpenseUiState.selectedWithdrawalPool]
     * and triggers a [fetchCashRate] via the delegate's [onPoolResolved] callback.
     */
    fun handleWithdrawalPoolSelected(scope: PayerType, scopeOwnerId: String?) {
        withdrawalPoolSelectionDelegate.handlePoolSelected(scope, scopeOwnerId, _uiState) {
            cashRateDelegate.fetchCashRate()
        }
    }

    internal fun fetchPoolsIfNeeded() {
        val state = _uiState.value
        val groupId = state.loadedGroupId ?: return
        val currency = state.selectedCurrency?.code ?: return
        val payerType = currentPayerType()
        val payerId = currentPayerId()
        withdrawalPoolSelectionDelegate.fetchPools(
            groupId = groupId,
            currency = currency,
            payerType = payerType,
            payerId = payerId,
            scope = scope,
            stateFlow = _uiState,
            onPoolResolved = {
                cashRateDelegate.fetchCashRate()
                // Notify the ViewModel so it can apply the smart split default if the resolved
                // pool is USER- or SUBUNIT-scoped. Reading pool from state here is safe because
                // WithdrawalPoolSelectionDelegate always updates selectedWithdrawalPool first.
                val pool = _uiState.value.selectedWithdrawalPool
                if (pool != null) {
                    poolResolvedCallback?.invoke(pool.scope, pool.ownerId)
                }
            }
        )
    }

    /**
     * Returns true if the currently selected payment method is CASH.
     */
    private fun isCashPaymentMethod(): Boolean {
        val methodId = _uiState.value.selectedPaymentMethod?.id ?: return false
        return try {
            PaymentMethod.fromString(methodId) == PaymentMethod.CASH
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Returns the [PayerType] derived from the currently selected funding source.
     * Defaults to [PayerType.GROUP] when no funding source is selected.
     */
    internal fun currentPayerType(): PayerType {
        val sourceId = _uiState.value.selectedFundingSource?.id ?: return PayerType.GROUP
        return try {
            PayerType.fromString(sourceId)
        } catch (_: IllegalArgumentException) {
            PayerType.GROUP
        }
    }

    /**
     * Returns the payer ID relevant to the current funding source scope:
     * - **GROUP:** the current user's ID ([AddExpenseUiState.currentUserId]), forwarded to
     *   [GetAvailableWithdrawalPoolsUseCase] so it can probe the user's personal (USER-scoped)
     *   pool as a supplement to the GROUP pool.
     * - **USER:** the current user's ID ([AddExpenseUiState.currentUserId]).
     * - **SUBUNIT:** the selected contribution subunit ID ([AddExpenseUiState.selectedContributionSubunitId])
     *   so the use case can look up cash pools owned by that specific subunit.
     */
    internal fun currentPayerId(): String? = when (currentPayerType()) {
        PayerType.USER, PayerType.GROUP -> _uiState.value.currentUserId
        PayerType.SUBUNIT -> _uiState.value.selectedContributionSubunitId
    }

    private fun AddExpenseUiState.updateRateResult(
        requestedBaseCode: String,
        requestedTargetCode: String,
        rateResult: ExchangeRateWithStaleness?,
        isError: Boolean
    ): AddExpenseUiState {
        if (groupCurrency?.code != requestedBaseCode ||
            selectedCurrency?.code != requestedTargetCode
        ) {
            return copy(isLoadingRate = false)
        }
        return copy(
            isLoadingRate = false,
            isExchangeRateError = isError,
            displayExchangeRate = rateResult?.rate?.let { exchangeRate ->
                formattingHelper.formatRateForDisplay(exchangeRate.toPlainString())
            } ?: displayExchangeRate,
            isExchangeRateStale = rateResult?.isStale ?: isExchangeRateStale
        )
    }
}
