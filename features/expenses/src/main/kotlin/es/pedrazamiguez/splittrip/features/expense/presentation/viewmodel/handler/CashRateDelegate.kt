package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Delegate responsible for CASH-specific exchange rate fetching and result mapping.
 *
 * Extracted from [CurrencyEventHandler] to keep the handler within the 600-line production-file
 * limit enforced by the Konsist architecture test. This is a plain class (NOT an
 * [AddExpenseEventHandler]) — it does NOT implement `bind()` and is NOT wired into the `init`
 * block of [AddExpenseViewModel].
 *
 * Initialised via [setup] inside [CurrencyEventHandler.bind] so it shares the same
 * [MutableStateFlow] and [CoroutineScope] as the owning handler.
 *
 * **Responsibilities:**
 * - Fetching the blended ATM rate preview via [PreviewCashExchangeRateUseCase].
 * - Mapping [CashRatePreviewResult] variants into updated [AddExpenseUiState] snapshots.
 * - Debounced recalculation on source-amount keystrokes.
 * - Cancelling in-flight or debounced CASH rate jobs when leaving CASH payment.
 */
class CashRateDelegate(
    private val previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val splitPreviewService: SplitPreviewService,
    private val formattingHelper: FormattingHelper,
    private val addExpenseOptionsMapper: AddExpenseOptionsUiMapper
) {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var scope: CoroutineScope

    /** Debounce job for cash rate preview recalculations on amount changes. */
    private var cashPreviewJob: Job? = null

    /** Tracked coroutine for in-flight cash rate fetch (prevents stale/duplicate results). */
    private var cashRateJob: Job? = null

    companion object {
        private const val CASH_PREVIEW_DEBOUNCE_MS = 300L

        /**
         * Placeholder shown in locked exchange-rate fields when no value is available
         * (e.g. insufficient cash, no withdrawals). Keeps the OutlinedTextField label
         * floating above the field instead of collapsing into the field body.
         */
        internal const val EMPTY_FIELD_PLACEHOLDER = "—"
    }

    /**
     * Wires this delegate to the state flow and coroutine scope owned by the parent handler.
     * Must be called from [CurrencyEventHandler.bind] before any other method.
     */
    fun setup(stateFlow: MutableStateFlow<AddExpenseUiState>, coroutineScope: CoroutineScope) {
        _uiState = stateFlow
        scope = coroutineScope
    }

    /**
     * Fetches the blended ATM exchange rate preview for the current source currency and amount.
     * Shows a weighted-average when no amount is entered yet.
     * Cancels any previous in-flight request and applies a stale-result guard.
     *
     * When the user has explicitly selected a pool via the pool-selection widget,
     * uses that pool's exact scope by passing [preferredWithdrawalScope] and
     * [preferredWithdrawalOwnerId] to [PreviewCashExchangeRateUseCase]. Otherwise,
     * uses the automatic scope-priority logic (personal pool first, GROUP fallback).
     */
    fun fetchCashRate() {
        val state = _uiState.value
        val groupId = state.loadedGroupId ?: return
        val sourceCurrency = state.selectedCurrency?.code ?: return
        val groupCurrency = state.groupCurrency?.code ?: return

        val isSameCurrency = sourceCurrency == groupCurrency
        val sourceDecimalDigits = state.selectedCurrency.decimalDigits
        val targetDecimalDigits = state.groupCurrency.decimalDigits

        // Parse current source amount to cents (0 if blank/invalid)
        val sourceAmountCents = splitPreviewService.parseAmountToCents(
            state.sourceAmount,
            sourceDecimalDigits
        )

        // Resolve scope context from the current funding source selection
        val payerType = try {
            state.selectedFundingSource?.id?.let { PayerType.fromString(it) } ?: PayerType.GROUP
        } catch (_: IllegalArgumentException) {
            PayerType.GROUP
        }
        val payerId = when (payerType) {
            PayerType.USER -> state.currentUserId
            PayerType.GROUP, PayerType.SUBUNIT -> null
        }

        val selectedPool = state.selectedWithdrawalPool
        val preferredScope = selectedPool?.scope
        val preferredOwnerId = selectedPool?.ownerId

        // Capture request context for stale-result check
        val requestedGroupId = groupId
        val requestedSourceCurrency = sourceCurrency

        // Cancel any previous in-flight cash rate request
        cashRateJob?.cancel()
        cashRateJob = scope.launch {
            _uiState.update { it.copy(isLoadingRate = true) }
            try {
                val result = previewCashExchangeRateUseCase(
                    groupId = requestedGroupId,
                    sourceCurrency = requestedSourceCurrency,
                    sourceAmountCents = sourceAmountCents,
                    payerType = payerType,
                    payerId = payerId,
                    preferredWithdrawalScope = preferredScope,
                    preferredWithdrawalOwnerId = preferredOwnerId
                )

                _uiState.update { current ->
                    // Stale-result check: ignore if the user changed group or currency
                    // while the request was in-flight.
                    if (current.loadedGroupId != requestedGroupId ||
                        current.selectedCurrency?.code != requestedSourceCurrency
                    ) {
                        return@update current.copy(isLoadingRate = false)
                    }

                    mapCashRateResult(
                        current,
                        result,
                        targetDecimalDigits,
                        requestedSourceCurrency,
                        !isSameCurrency
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to preview cash exchange rate")
                _uiState.update { it.copy(isLoadingRate = false) }
            }
        }
    }

    /**
     * Maps a [CashRatePreviewResult] into an updated [AddExpenseUiState].
     *
     * Extracted from [fetchCashRate] to reduce method length and complexity.
     * Each sealed variant is delegated to a private mapping method.
     */
    internal fun mapCashRateResult(
        current: AddExpenseUiState,
        result: CashRatePreviewResult,
        targetDecimalDigits: Int,
        sourceCurrencyCode: String = "",
        updateRateFields: Boolean = true
    ): AddExpenseUiState = when (result) {
        is CashRatePreviewResult.Available -> mapAvailableResult(
            current,
            result,
            targetDecimalDigits,
            sourceCurrencyCode,
            updateRateFields
        )
        is CashRatePreviewResult.InsufficientCash -> mapInsufficientCashResult(current, updateRateFields)
        is CashRatePreviewResult.NoWithdrawals -> mapNoWithdrawalsResult(current, updateRateFields)
    }

    private fun mapAvailableResult(
        current: AddExpenseUiState,
        result: CashRatePreviewResult.Available,
        targetDecimalDigits: Int,
        sourceCurrencyCode: String,
        updateRateFields: Boolean
    ): AddExpenseUiState {
        val preview = result.preview
        val tranchePreviews = if (preview.tranches.isNotEmpty() && sourceCurrencyCode.isNotBlank()) {
            addExpenseOptionsMapper.mapCashTranchePreviews(preview.tranches, sourceCurrencyCode)
        } else {
            persistentListOf()
        }

        // Same-currency CASH: only update tranche preview and clear insufficient flag.
        if (!updateRateFields) {
            return current.copy(
                isLoadingRate = false,
                isInsufficientCash = false,
                cashTranchePreviews = tranchePreviews
            )
        }

        val formattedRate = formattingHelper.formatRateForDisplay(preview.displayRate.toPlainString())

        return if (preview.groupAmountCents > 0) {
            // FIFO-simulated: update both rate and group amount
            val groupAmountStr = expenseCalculatorService.centsToBigDecimalString(
                preview.groupAmountCents,
                targetDecimalDigits
            )
            val formattedAmount = formattingHelper.formatForDisplay(
                internalValue = groupAmountStr,
                maxDecimalPlaces = targetDecimalDigits,
                minDecimalPlaces = targetDecimalDigits
            )
            current.copy(
                isLoadingRate = false,
                displayExchangeRate = formattedRate,
                calculatedGroupAmount = formattedAmount,
                isExchangeRateLocked = true,
                isInsufficientCash = false,
                exchangeRateLockedHint = UiText.StringResource(R.string.add_expense_cash_rate_locked_hint),
                cashTranchePreviews = tranchePreviews
            )
        } else {
            // Weighted-average preview (no amount entered yet).
            current.copy(
                isLoadingRate = false,
                displayExchangeRate = formattedRate,
                calculatedGroupAmount = "",
                isExchangeRateLocked = true,
                isInsufficientCash = false,
                exchangeRateLockedHint = UiText.StringResource(R.string.add_expense_cash_rate_locked_hint),
                cashTranchePreviews = persistentListOf()
            )
        }
    }

    private fun mapInsufficientCashResult(
        current: AddExpenseUiState,
        updateRateFields: Boolean
    ): AddExpenseUiState = current.copy(
        isLoadingRate = false,
        displayExchangeRate = if (updateRateFields) EMPTY_FIELD_PLACEHOLDER else current.displayExchangeRate,
        calculatedGroupAmount = if (updateRateFields) EMPTY_FIELD_PLACEHOLDER else current.calculatedGroupAmount,
        isExchangeRateLocked = updateRateFields || current.isExchangeRateLocked,
        isInsufficientCash = true,
        exchangeRateLockedHint = if (updateRateFields) {
            UiText.StringResource(R.string.add_expense_cash_insufficient_hint)
        } else {
            current.exchangeRateLockedHint
        },
        cashTranchePreviews = persistentListOf()
    )

    private fun mapNoWithdrawalsResult(
        current: AddExpenseUiState,
        updateRateFields: Boolean
    ): AddExpenseUiState = current.copy(
        isLoadingRate = false,
        displayExchangeRate = if (updateRateFields) EMPTY_FIELD_PLACEHOLDER else current.displayExchangeRate,
        calculatedGroupAmount = if (updateRateFields) EMPTY_FIELD_PLACEHOLDER else current.calculatedGroupAmount,
        isExchangeRateLocked = updateRateFields,
        isInsufficientCash = false,
        exchangeRateLockedHint = if (updateRateFields) {
            UiText.StringResource(R.string.add_expense_cash_rate_locked_hint)
        } else {
            current.exchangeRateLockedHint
        },
        cashTranchePreviews = persistentListOf()
    )

    /** Debounced CASH rate recalculation — avoids hitting Room on every keystroke. */
    fun recalculateCashForward() {
        cashPreviewJob?.cancel()
        cashPreviewJob = scope.launch {
            delay(CASH_PREVIEW_DEBOUNCE_MS)
            fetchCashRate()
        }
    }

    /**
     * Cancels any in-flight or debounced CASH rate jobs.
     * Called when leaving CASH payment to prevent a stale result from
     * re-locking the exchange rate after the user has switched away.
     */
    fun cancelPendingCashJobs() {
        cashRateJob?.cancel()
        cashRateJob = null
        cashPreviewJob?.cancel()
        cashPreviewJob = null
    }
}
