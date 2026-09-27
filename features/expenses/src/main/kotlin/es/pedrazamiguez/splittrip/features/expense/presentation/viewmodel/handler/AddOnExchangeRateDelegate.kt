package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Encapsulates all per-add-on exchange-rate logic: API rate fetching, CASH/FIFO
 * blended rate previews, forward/reverse recalculation, and debounced updates.
 *
 * This is a plain delegate class (NOT an [AddExpenseEventHandler]) — it does not
 * participate in `bind()`. The owning [AddOnEventHandler] passes the required
 * context (scope, state reader, state mutator) on each call.
 */
class AddOnExchangeRateDelegate(
    private val exchangeRateCalculationService: ExchangeRateCalculationService,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val splitPreviewService: SplitPreviewService,
    private val getExchangeRateUseCase: GetExchangeRateUseCase,
    private val previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase
) {

    /** Tracked API rate-fetch jobs per add-on to prevent stale results. */
    private val rateFetchJobs = ConcurrentHashMap<String, Job>()

    /** Tracked cash rate-fetch jobs per add-on to prevent stale/duplicate results. */
    private val cashRateFetchJobs = ConcurrentHashMap<String, Job>()

    /** Tracked cash rate debounce jobs per add-on (amount changes while CASH). */
    private val cashPreviewJobs = ConcurrentHashMap<String, Job>()

    // ── API Rate Fetch ──────────────────────────────────────────────────

    /**
     * Fetches the exchange rate for an add-on's currency pair and updates
     * the add-on's [AddOnUiModel.displayExchangeRate] when the result arrives.
     *
     * Cancels any in-flight fetch for the same add-on to prevent stale results.
     */
    fun fetchRate(
        addOnId: String,
        baseCurrencyCode: String,
        targetCurrencyCode: String,
        scope: CoroutineScope,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        updateAddOn: (String, (AddOnUiModel) -> AddOnUiModel) -> Unit,
        onRateApplied: () -> Unit
    ) {
        rateFetchJobs[addOnId]?.cancel()
        rateFetchJobs[addOnId] = scope.launch {
            updateAddOn(addOnId) { it.copy(isLoadingRate = true) }
            try {
                val rateResult = getExchangeRateUseCase(
                    baseCurrencyCode = baseCurrencyCode,
                    targetCurrencyCode = targetCurrencyCode
                )

                updateAddOn(addOnId) { current ->
                    val state = stateFlow.value
                    val addOn = state.addOns.find { it.id == addOnId }
                    if (addOn?.currency?.code != targetCurrencyCode ||
                        state.groupCurrency?.code != baseCurrencyCode
                    ) {
                        current.copy(isLoadingRate = false)
                    } else {
                        val rawRate = rateResult?.rate?.stripTrailingZeros()?.toPlainString()
                            ?: current.displayExchangeRate
                        current.copy(
                            isLoadingRate = false,
                            displayExchangeRate = rawRate,
                            isExchangeRateStale = rateResult?.isStale
                                ?: current.isExchangeRateStale
                        )
                    }
                }

                if (rateResult != null) {
                    recalculateForward(addOnId, stateFlow, updateAddOn)
                    onRateApplied()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch add-on rate for $baseCurrencyCode -> $targetCurrencyCode")
                updateAddOn(addOnId) { it.copy(isLoadingRate = false) }
            }
        }
    }

    // ── Forward / Reverse Recalculation ──────────────────────────────────

    /**
     * Forward calculation for a single add-on: source amount + display rate → group amount.
     */
    fun recalculateForward(
        addOnId: String,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        updateAddOn: (String, (AddOnUiModel) -> AddOnUiModel) -> Unit
    ) {
        val state = stateFlow.value
        val addOn = state.addOns.find { it.id == addOnId } ?: return
        if (!addOn.showExchangeRateSection) return

        val sourceDecimalPlaces = addOn.currency?.decimalDigits ?: 2
        val targetDecimalPlaces = state.groupCurrency?.decimalDigits ?: 2

        val sourceAmountStr = if (addOn.resolvedAmountCents > 0) {
            expenseCalculatorService.centsToBigDecimalString(
                addOn.resolvedAmountCents,
                sourceDecimalPlaces
            )
        } else {
            addOn.amountInput
        }

        val calculatedAmount = exchangeRateCalculationService.calculateGroupAmountFromDisplayRate(
            sourceAmountString = sourceAmountStr,
            displayRateString = addOn.displayExchangeRate,
            sourceDecimalPlaces = sourceDecimalPlaces,
            targetDecimalPlaces = targetDecimalPlaces
        )

        updateAddOn(addOnId) { it.copy(calculatedGroupAmount = calculatedAmount) }
    }

    /**
     * Reverse calculation for a single add-on: group amount → implied display rate.
     */
    fun recalculateReverse(
        addOnId: String,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        updateAddOn: (String, (AddOnUiModel) -> AddOnUiModel) -> Unit
    ) {
        val state = stateFlow.value
        val addOn = state.addOns.find { it.id == addOnId } ?: return
        if (!addOn.showExchangeRateSection) return

        val sourceDecimalPlaces = addOn.currency?.decimalDigits ?: 2

        val sourceAmountStr = if (addOn.resolvedAmountCents > 0) {
            expenseCalculatorService.centsToBigDecimalString(
                addOn.resolvedAmountCents,
                sourceDecimalPlaces
            )
        } else {
            addOn.amountInput
        }

        val impliedDisplayRate = exchangeRateCalculationService.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = sourceAmountStr,
            groupAmountString = addOn.calculatedGroupAmount,
            sourceDecimalPlaces = sourceDecimalPlaces
        )

        updateAddOn(addOnId) { it.copy(displayExchangeRate = impliedDisplayRate) }
    }

    // ── CASH Rate ───────────────────────────────────────────────────────

    /**
     * Fetches the blended exchange rate from ATM withdrawals for a specific add-on.
     * The pool queried is scoped by [payerType] and [payerId]:
     * - **GROUP** (default): all group-pocket withdrawals.
     * - **USER**: only the current user's personal withdrawals.
     * - **SUBUNIT**: not yet narrowed further (treated as GROUP pool until SUBUNIT
     *   funding-source selection is implemented).
     *
     * Cancels any previous in-flight cash rate request for this add-on and verifies
     * the result is still relevant before applying state changes.
     */
    fun fetchCashRate(
        addOnId: String,
        scope: CoroutineScope,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        updateAddOn: (String, (AddOnUiModel) -> AddOnUiModel) -> Unit,
        onRateApplied: () -> Unit,
        payerType: PayerType = PayerType.GROUP,
        payerId: String? = null
    ) {
        val state = stateFlow.value
        val addOn = state.addOns.find { it.id == addOnId } ?: return
        val groupId = state.loadedGroupId ?: return
        val sourceCurrency = addOn.currency?.code ?: return
        val groupCurrency = state.groupCurrency?.code
        if (groupCurrency == null || sourceCurrency == groupCurrency) return

        val targetDecimalDigits = state.groupCurrency.decimalDigits
        val sourceDecimalDigits = addOn.currency.decimalDigits
        val sourceAmountCents = parseAddOnAmountToCents(addOn, sourceDecimalDigits)

        cashRateFetchJobs[addOnId]?.cancel()
        cashRateFetchJobs[addOnId] = scope.launch {
            updateAddOn(addOnId) { it.copy(isLoadingRate = true) }
            try {
                val result = previewCashExchangeRateUseCase(
                    groupId = groupId,
                    sourceCurrency = sourceCurrency,
                    sourceAmountCents = sourceAmountCents,
                    payerType = payerType,
                    payerId = payerId
                )
                applyCashRateResult(
                    addOnId,
                    result,
                    groupId,
                    sourceCurrency,
                    targetDecimalDigits,
                    stateFlow,
                    updateAddOn
                )
                onRateApplied()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to preview cash exchange rate for add-on $addOnId")
                updateAddOn(addOnId) { it.copy(isLoadingRate = false) }
            }
        }
    }

    /**
     * Debounced recalculation for CASH add-ons when the amount changes.
     * Calls [fetchCashRate] after a short delay to avoid hitting Room on every keystroke.
     */
    fun recalculateCashForward(
        addOnId: String,
        scope: CoroutineScope,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        updateAddOn: (String, (AddOnUiModel) -> AddOnUiModel) -> Unit,
        onRateApplied: () -> Unit,
        payerType: PayerType = PayerType.GROUP,
        payerId: String? = null
    ) {
        cashPreviewJobs[addOnId]?.cancel()
        cashPreviewJobs[addOnId] = scope.launch {
            delay(CASH_PREVIEW_DEBOUNCE_MS)
            fetchCashRate(addOnId, scope, stateFlow, updateAddOn, onRateApplied, payerType, payerId)
        }
    }

    /**
     * Cancels any in-flight or debounced CASH rate jobs for a specific add-on.
     * Also cancels any pending API rate fetch.
     */
    fun cancelPendingJobs(addOnId: String) {
        rateFetchJobs.remove(addOnId)?.cancel()
        cashRateFetchJobs.remove(addOnId)?.cancel()
        cashPreviewJobs.remove(addOnId)?.cancel()
    }

    // ── Utility ─────────────────────────────────────────────────────────

    /**
     * Returns true if the given payment method ID corresponds to CASH.
     */
    fun isCashMethod(methodId: String?): Boolean {
        if (methodId == null) return false
        return try {
            PaymentMethod.fromString(methodId) == PaymentMethod.CASH
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────────

    /**
     * Applies the [CashRatePreviewResult] to the add-on's UI state.
     * Ignores the result if the group or currency has changed since the request was made.
     */
    private fun applyCashRateResult(
        addOnId: String,
        result: CashRatePreviewResult,
        requestedGroupId: String,
        requestedSourceCurrency: String,
        targetDecimalDigits: Int,
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        updateAddOn: (String, (AddOnUiModel) -> AddOnUiModel) -> Unit
    ) {
        updateAddOn(addOnId) { current ->
            val currentState = stateFlow.value
            val currentAddOn = currentState.addOns.find { it.id == addOnId }
            if (currentState.loadedGroupId != requestedGroupId ||
                currentAddOn?.currency?.code != requestedSourceCurrency
            ) {
                return@updateAddOn current.copy(isLoadingRate = false)
            }

            when (result) {
                is CashRatePreviewResult.Available ->
                    buildAvailableState(current, result, targetDecimalDigits)

                is CashRatePreviewResult.InsufficientCash ->
                    buildUnavailableState(current, isInsufficientCash = true)

                is CashRatePreviewResult.NoWithdrawals ->
                    buildUnavailableState(current, isInsufficientCash = false)
            }
        }
    }

    private fun buildAvailableState(
        current: AddOnUiModel,
        result: CashRatePreviewResult.Available,
        targetDecimalDigits: Int
    ): AddOnUiModel {
        val preview = result.preview
        val rawRate = preview.displayRate.stripTrailingZeros().toPlainString()
        val rawAmount = if (preview.groupAmountCents > 0) {
            expenseCalculatorService.centsToBigDecimalString(
                preview.groupAmountCents,
                targetDecimalDigits
            )
        } else {
            ""
        }
        return current.copy(
            isLoadingRate = false,
            displayExchangeRate = rawRate,
            calculatedGroupAmount = rawAmount,
            isExchangeRateLocked = true,
            isInsufficientCash = false,
            exchangeRateLockedHint = UiText.StringResource(
                R.string.add_expense_cash_rate_locked_hint
            )
        )
    }

    private fun buildUnavailableState(
        current: AddOnUiModel,
        isInsufficientCash: Boolean
    ): AddOnUiModel {
        val hintRes = if (isInsufficientCash) {
            R.string.add_expense_cash_insufficient_hint
        } else {
            R.string.add_expense_cash_rate_locked_hint
        }
        return current.copy(
            isLoadingRate = false,
            displayExchangeRate = EMPTY_FIELD_PLACEHOLDER,
            calculatedGroupAmount = EMPTY_FIELD_PLACEHOLDER,
            isExchangeRateLocked = true,
            isInsufficientCash = isInsufficientCash,
            exchangeRateLockedHint = UiText.StringResource(hintRes)
        )
    }

    private fun parseAddOnAmountToCents(addOn: AddOnUiModel, decimalPlaces: Int): Long {
        if (addOn.resolvedAmountCents > 0) return addOn.resolvedAmountCents
        return splitPreviewService.parseAmountToCents(addOn.amountInput, decimalPlaces)
    }

    companion object {
        private const val CASH_PREVIEW_DEBOUNCE_MS = 300L
        private const val EMPTY_FIELD_PLACEHOLDER = "—"
    }
}
