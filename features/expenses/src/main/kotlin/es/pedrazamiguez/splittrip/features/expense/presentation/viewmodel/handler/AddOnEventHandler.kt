package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles add-on CRUD and recalculation events within the Add Expense form.
 *
 * Manages the `addOns` list in [AddExpenseUiState], resolves amounts
 * (including percentage → cents), and recalculates the effective total.
 *
 * Exchange-rate concerns (API fetch, CASH/FIFO preview, forward/reverse
 * recalculation, debouncing) are delegated to [AddOnExchangeRateDelegate].
 *
 * Exposes [recalculateEffectiveTotal] for cross-handler calls
 * (e.g., when source amount or currency changes).
 */
// 13 public event methods (one per UI event) + 7 private helpers — function count
// is proportional to add-on event surface, not avoidable without artificial merging
@Suppress("TooManyFunctions")
class AddOnEventHandler(
    private val addOnCalculationService: AddOnCalculationService,
    private val exchangeRateCalculationService: ExchangeRateCalculationService,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val splitPreviewService: SplitPreviewService,
    private val formattingHelper: FormattingHelper,
    private val addExpenseOptionsMapper: AddExpenseOptionsUiMapper,
    private val exchangeRateDelegate: AddOnExchangeRateDelegate,
    private val addOnCrudDelegate: AddOnCrudDelegate
) : AddExpenseEventHandler {

    private lateinit var _uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var _actions: MutableSharedFlow<AddExpenseUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    // ── CRUD Operations ─────────────────────────────────────────────────

    fun handleAddOnAdded(type: AddOnType) {
        val state = _uiState.value
        val newAddOn = addOnCrudDelegate.buildNewAddOn(type, state)
        val (isForeign, isCash, _) = addOnCrudDelegate.resolveAddOnCurrencyContext(state)

        _uiState.update {
            it.copy(
                addOns = (it.addOns + newAddOn).toImmutableList(),
                isAddOnsSectionExpanded = true,
                addOnError = null
            )
        }

        // Auto-fetch rate if foreign currency
        if (isForeign) {
            if (isCash) {
                exchangeRateDelegate.fetchCashRate(
                    newAddOn.id,
                    scope,
                    _uiState,
                    ::updateAddOn,
                    ::recalculateEffectiveTotal,
                    payerType = currentPayerType(),
                    payerId = currentPayerId()
                )
            } else {
                exchangeRateDelegate.fetchRate(
                    newAddOn.id,
                    state.groupCurrency!!.code,
                    state.selectedCurrency!!.code,
                    scope,
                    _uiState,
                    ::updateAddOn,
                    ::recalculateEffectiveTotal
                )
            }
        }
    }

    fun handleAddOnRemoved(addOnId: String) {
        exchangeRateDelegate.cancelPendingJobs(addOnId)
        _uiState.update {
            it.copy(
                addOns = it.addOns.filter { a -> a.id != addOnId }.toImmutableList(),
                addOnError = null
            )
        }
        recalculateEffectiveTotal()
    }

    fun handleTypeChanged(addOnId: String, type: AddOnType) {
        updateAddOn(addOnId) { it.copy(type = type) }
    }

    fun handleModeChanged(addOnId: String, mode: AddOnMode) {
        updateAddOn(addOnId) { it.copy(mode = mode) }
        recalculateEffectiveTotal()
    }

    fun handleValueTypeChanged(addOnId: String, valueType: AddOnValueType) {
        updateAddOn(addOnId) { it.copy(valueType = valueType, amountInput = "") }
        val state = _uiState.value
        state.addOns.find { it.id == addOnId }?.let { addOn ->
            updateAddOn(addOnId) { resolveAddOnAmounts(addOn, state) }
        }
        recalculateEffectiveTotal()
    }

    fun handleAmountChanged(addOnId: String, amount: String) {
        updateAddOn(addOnId) {
            it.copy(amountInput = amount, isAmountValid = true)
        }
        _uiState.update { it.copy(addOnError = null) }
        val state = _uiState.value
        state.addOns.find { it.id == addOnId }?.let { addOn ->
            updateAddOn(addOnId) { resolveAddOnAmounts(addOn, state) }
        }
        recalculateEffectiveTotal()

        // Re-fetch CASH rate when amount changes (FIFO may use different tranches)
        val updatedAddOn = _uiState.value.addOns.find { it.id == addOnId }
        if (updatedAddOn?.isExchangeRateLocked == true) {
            exchangeRateDelegate.recalculateCashForward(
                addOnId,
                scope,
                _uiState,
                ::updateAddOn,
                ::recalculateEffectiveTotal
            )
        }
    }

    // Barely over threshold (17/15);
    // branching is inherent to currency-change side effects
    @Suppress("CognitiveComplexMethod")
    fun handleCurrencySelected(addOnId: String, currencyCode: String) {
        val state = _uiState.value
        val currency = state.availableCurrencies
            .find { it.code == currencyCode } ?: return
        val groupCurrency = state.groupCurrency ?: return
        val isForeign = currency.code != groupCurrency.code
        val currentAddOn = state.addOns.find { it.id == addOnId } ?: return
        val isCash = exchangeRateDelegate.isCashMethod(currentAddOn.paymentMethod?.id)

        val exchangeRateLabel = exchangeRateLabelOrEmpty(isForeign, groupCurrency, currency)
        val groupAmountLabel = groupAmountLabelOrEmpty(isForeign, groupCurrency)

        val shouldLockRate = isForeign && isCash

        updateAddOn(addOnId) {
            it.copy(
                currency = currency,
                showExchangeRateSection = isForeign,
                exchangeRateLabel = exchangeRateLabel,
                groupAmountLabel = groupAmountLabel,
                displayExchangeRate = if (isForeign) it.displayExchangeRate else "1.0",
                calculatedGroupAmount = if (isForeign) it.calculatedGroupAmount else "",
                isExchangeRateLocked = shouldLockRate,
                isInsufficientCash = false,
                exchangeRateLockedHint = if (shouldLockRate) {
                    UiText.StringResource(R.string.add_expense_cash_rate_locked_hint)
                } else {
                    null
                },
                // Clear pre-cash rate when currency changes while on CASH
                preCashExchangeRate = if (isCash) null else it.preCashExchangeRate
            )
        }

        // Re-resolve amounts with the new currency
        val updatedState = _uiState.value
        updatedState.addOns.find { it.id == addOnId }?.let { addOn ->
            updateAddOn(addOnId) { resolveAddOnAmounts(addOn, updatedState) }
        }
        recalculateEffectiveTotal()

        // Auto-fetch rate if foreign
        if (isForeign) {
            if (isCash) {
                exchangeRateDelegate.fetchCashRate(
                    addOnId,
                    scope,
                    _uiState,
                    ::updateAddOn,
                    ::recalculateEffectiveTotal,
                    payerType = currentPayerType(),
                    payerId = currentPayerId()
                )
            } else {
                exchangeRateDelegate.fetchRate(
                    addOnId,
                    groupCurrency.code,
                    currency.code,
                    scope,
                    _uiState,
                    ::updateAddOn,
                    ::recalculateEffectiveTotal
                )
            }
        }
    }

    /**
     * Reacts to the payment method changing on a specific add-on.
     *
     * When switching TO CASH + foreign currency:
     * - Saves the current display exchange rate so it can be restored later
     * - Locks the exchange rate fields (not user-editable)
     * - Shows a hint explaining the rate source
     * - Computes a preview blended rate from available ATM withdrawals
     *
     * When switching FROM CASH to non-CASH + foreign currency:
     * - Unlocks the exchange rate fields
     * - Restores the previously saved exchange rate (if available)
     * - Falls back to fetching the API rate only when no saved rate exists
     *
     * When switching between non-CASH methods + foreign currency:
     * - Does nothing with the exchange rate — the user's custom rate is preserved
     */
    fun handlePaymentMethodSelected(addOnId: String, methodId: String) {
        val method = _uiState.value.paymentMethods.find { it.id == methodId } ?: return
        val state = _uiState.value
        val addOn = state.addOns.find { it.id == addOnId } ?: return
        val groupCurrency = state.groupCurrency
        val isForeign = addOn.currency != null &&
            groupCurrency != null &&
            addOn.currency.code != groupCurrency.code
        val isCash = exchangeRateDelegate.isCashMethod(method.id)
        val wasCashLocked = addOn.isExchangeRateLocked

        updateAddOn(addOnId) { it.copy(paymentMethod = method) }

        val updatedAddOn = addOnCrudDelegate.applyPaymentMethodSwitch(addOn, isCash, isForeign, wasCashLocked)
        updateAddOn(addOnId) { updatedAddOn.copy(paymentMethod = method) }

        // Trigger rate fetch/recalculation based on the switch
        when {
            isCash && isForeign -> {
                exchangeRateDelegate.fetchCashRate(
                    addOnId,
                    scope,
                    _uiState,
                    ::updateAddOn,
                    ::recalculateEffectiveTotal,
                    payerType = currentPayerType(),
                    payerId = currentPayerId()
                )
            }
            !isCash && isForeign && wasCashLocked -> {
                exchangeRateDelegate.cancelPendingJobs(addOnId)
                if (addOnCrudDelegate.hasSavedPreCashRate(addOn)) {
                    exchangeRateDelegate.recalculateForward(addOnId, _uiState, ::updateAddOn)
                    recalculateEffectiveTotal()
                } else {
                    exchangeRateDelegate.fetchRate(
                        addOnId,
                        groupCurrency.code,
                        addOn.currency.code,
                        scope,
                        _uiState,
                        ::updateAddOn,
                        ::recalculateEffectiveTotal
                    )
                }
            }
            !isCash && wasCashLocked -> {
                exchangeRateDelegate.cancelPendingJobs(addOnId)
            }
        }
    }

    fun handleDescriptionChanged(addOnId: String, description: String) {
        updateAddOn(addOnId) { it.copy(description = description) }
    }

    fun handleSectionToggled() {
        _uiState.update {
            it.copy(isAddOnsSectionExpanded = !it.isAddOnsSectionExpanded)
        }
    }

    // ── Per-add-on Exchange Rate Events ──────────────────────────────────

    fun handleExchangeRateChanged(addOnId: String, rate: String) {
        updateAddOn(addOnId) { it.copy(displayExchangeRate = rate) }
        exchangeRateDelegate.recalculateForward(addOnId, _uiState, ::updateAddOn)
        recalculateEffectiveTotal()
    }

    fun handleGroupAmountChanged(addOnId: String, amount: String) {
        updateAddOn(addOnId) { it.copy(calculatedGroupAmount = amount) }
        exchangeRateDelegate.recalculateReverse(addOnId, _uiState, ::updateAddOn)
        recalculateEffectiveTotal()
    }

    // ── Cross-handler: Recalculate ──────────────────────────────────────

    /**
     * Recalculates the effective total and the INCLUDED base cost, then updates the display.
     * Called by other handlers when source amount or currency changes.
     */
    fun recalculateEffectiveTotal() {
        val state = _uiState.value
        val groupCurrency = state.groupCurrency ?: return

        // Re-resolve all add-ons with current source amount
        val resolvedAddOns = state.addOns.map { addOn ->
            resolveAddOnAmounts(addOn, state)
        }.toImmutableList()

        // Parse source amount to group amount cents for effective total
        val groupDecimalDigits = state.groupCurrency.decimalDigits
        val groupAmountStr = state.calculatedGroupAmount.ifBlank { state.sourceAmount }
        val groupAmountCents = splitPreviewService.parseAmountToCents(groupAmountStr, groupDecimalDigits)

        // Build domain-level add-on list for calculation
        val domainAddOns = resolvedAddOns.map { uiModel ->
            es.pedrazamiguez.splittrip.domain.model.AddOn(
                groupAmountCents = uiModel.groupAmountCents
                    .takeIf { uiModel.mode != AddOnMode.INCLUDED } ?: 0L,
                mode = uiModel.mode,
                type = uiModel.type
            )
        }

        val effectiveCents = addOnCalculationService.calculateEffectiveGroupAmount(
            groupAmountCents,
            domainAddOns
        )

        val effectiveDisplay = if (effectiveCents != groupAmountCents && effectiveCents > 0) {
            formattingHelper.formatCentsWithCurrency(effectiveCents, groupCurrency.code)
        } else {
            ""
        }

        // Compute base cost for INCLUDED add-ons
        val baseCostDisplay = computeIncludedBaseCostDisplay(
            resolvedAddOns,
            groupAmountCents,
            groupCurrency
        )

        _uiState.update {
            it.copy(
                addOns = resolvedAddOns,
                effectiveTotal = effectiveDisplay,
                includedBaseCost = baseCostDisplay
            )
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────────

    /**
     * Derives the formatted base cost when INCLUDED add-ons are present.
     *
     * Separates EXACT and PERCENTAGE included add-ons — and within those,
     * discount vs non-discount — so the domain service can apply the correct
     * extraction formula for each type.
     *
     * @return A formatted currency string, or empty when there are no INCLUDED add-ons.
     */
    private fun computeIncludedBaseCostDisplay(
        addOns: ImmutableList<AddOnUiModel>,
        groupAmountCents: Long,
        groupCurrency: CurrencyUiModel
    ): String {
        val includedAddOns = addOns.filter { it.mode == AddOnMode.INCLUDED }
        if (includedAddOns.isEmpty() || groupAmountCents <= 0) return ""

        val (discountAddOns, nonDiscountAddOns) = includedAddOns.partition {
            it.type == AddOnType.DISCOUNT
        }

        val includedExactCents = nonDiscountAddOns
            .filter { it.valueType == AddOnValueType.EXACT }
            .sumOf { it.groupAmountCents }

        val includedExactDiscountCents = discountAddOns
            .filter { it.valueType == AddOnValueType.EXACT }
            .sumOf { it.groupAmountCents }

        val totalIncludedPercentage = addOnCalculationService.sumPercentagesFromInputs(
            nonDiscountAddOns
                .filter { it.valueType == AddOnValueType.PERCENTAGE }
                .map { it.amountInput }
        )

        val totalIncludedDiscountPercentage = addOnCalculationService.sumPercentagesFromInputs(
            discountAddOns
                .filter { it.valueType == AddOnValueType.PERCENTAGE }
                .map { it.amountInput }
        )

        val baseCostCents = addOnCalculationService.calculateIncludedBaseCost(
            totalAmountCents = groupAmountCents,
            includedExactCents = includedExactCents,
            totalIncludedPercentage = totalIncludedPercentage,
            includedExactDiscountCents = includedExactDiscountCents,
            totalIncludedDiscountPercentage = totalIncludedDiscountPercentage
        )

        // Base can be < total (surcharges/tips) or > total (discounts)
        return if (baseCostCents > 0 && baseCostCents != groupAmountCents) {
            formattingHelper.formatCentsWithCurrency(baseCostCents, groupCurrency.code)
        } else {
            ""
        }
    }

    private fun updateAddOn(
        addOnId: String,
        transform: (AddOnUiModel) -> AddOnUiModel
    ) {
        _uiState.update { state ->
            state.copy(
                addOns = state.addOns.map { addOn ->
                    if (addOn.id == addOnId) transform(addOn) else addOn
                }.toImmutableList()
            )
        }
    }

    /**
     * Resolves an add-on's amount input into [AddOnUiModel.resolvedAmountCents]
     * and [AddOnUiModel.groupAmountCents].
     *
     * For PERCENTAGE value type, computes the percentage of the source amount.
     * For EXACT, parses the input directly.
     * Then converts to group currency using the add-on's own exchange rate.
     *
     * Also called from [recalculateEffectiveTotal] for batch re-resolution.
     */
    private fun resolveAddOnAmounts(addOn: AddOnUiModel, state: AddExpenseUiState): AddOnUiModel {
        if (addOn.amountInput.isBlank()) {
            return addOn.copy(resolvedAmountCents = 0L, groupAmountCents = 0L)
        }

        val decimalDigits = addOn.currency?.decimalDigits ?: 2
        val normalizedInput = CurrencyConverter.normalizeAmountString(
            addOn.amountInput.trim()
        )
        val inputBd = normalizedInput.toBigDecimalOrNull()
            ?: return addOn.copy(
                resolvedAmountCents = 0L,
                groupAmountCents = 0L,
                isAmountValid = false
            )

        val sourceDecimalDigits = state.selectedCurrency?.decimalDigits ?: 2
        val sourceAmountCents = splitPreviewService.parseAmountToCents(
            state.sourceAmount,
            sourceDecimalDigits
        )
        if (addOn.valueType == AddOnValueType.PERCENTAGE && sourceAmountCents <= 0) return addOn

        val resolvedCents = resolveAddOnAmountCentsForType(addOn, inputBd, decimalDigits, sourceAmountCents)

        // Convert to group currency using the add-on's own rate
        val groupAmountCents = convertAddOnToGroupCurrency(resolvedCents, addOn)

        // Update the display string for the exchange rate card
        val calculatedGroupAmount = if (addOn.showExchangeRateSection && groupAmountCents > 0) {
            val targetDecimalPlaces = state.groupCurrency?.decimalDigits ?: 2
            val displayValue = expenseCalculatorService.centsToBigDecimalString(
                groupAmountCents,
                targetDecimalPlaces
            )
            formattingHelper.formatForDisplay(
                internalValue = displayValue,
                maxDecimalPlaces = targetDecimalPlaces,
                minDecimalPlaces = targetDecimalPlaces
            )
        } else {
            addOn.calculatedGroupAmount
        }

        return addOn.copy(
            resolvedAmountCents = resolvedCents,
            groupAmountCents = groupAmountCents,
            calculatedGroupAmount = calculatedGroupAmount
        )
    }

    private fun convertAddOnToGroupCurrency(
        amountCents: Long,
        addOn: AddOnUiModel
    ): Long {
        if (addOn.currency == null) return amountCents
        if (!addOn.showExchangeRateSection) return amountCents
        return exchangeRateCalculationService.convertCentsToGroupCurrencyViaDisplayRate(
            amountCents,
            addOn.displayExchangeRate
        )
    }

    /**
     * Routes percentage-based INCLUDED DISCOUNTs through the corrected formula
     * `source × pct / (100 − pct)`. The generic resolver returns `source × pct / 100`,
     * which under-reports the embedded discount because [sourceAmountCents] is the
     * already-discounted price, not the original base cost.
     */
    private fun resolveAddOnAmountCentsForType(
        addOn: AddOnUiModel,
        inputBd: BigDecimal,
        decimalDigits: Int,
        sourceAmountCents: Long
    ): Long {
        val isIncludedDiscountPercentage = addOn.mode == AddOnMode.INCLUDED &&
            addOn.type == AddOnType.DISCOUNT &&
            addOn.valueType == AddOnValueType.PERCENTAGE
        return if (isIncludedDiscountPercentage) {
            addOnCalculationService.calculateIncludedDiscountPercentageCents(
                sourceAmountCents = sourceAmountCents,
                discountPercentage = inputBd
            )
        } else {
            addOnCalculationService.resolveAddOnAmountCents(
                normalizedInput = inputBd,
                valueType = addOn.valueType,
                decimalDigits = decimalDigits,
                sourceAmountCents = sourceAmountCents
            )
        }
    }

    private fun exchangeRateLabelOrEmpty(
        isForeign: Boolean,
        groupCurrency: CurrencyUiModel?,
        addOnCurrency: CurrencyUiModel?
    ): String {
        if (!isForeign || groupCurrency == null || addOnCurrency == null) return ""
        return addExpenseOptionsMapper.buildExchangeRateLabel(groupCurrency, addOnCurrency)
    }

    private fun groupAmountLabelOrEmpty(
        isForeign: Boolean,
        groupCurrency: CurrencyUiModel?
    ): String {
        if (!isForeign || groupCurrency == null) return ""
        return addExpenseOptionsMapper.buildGroupAmountLabel(groupCurrency)
    }

    /**
     * Returns the [PayerType] derived from the currently selected funding source.
     * Defaults to [PayerType.GROUP] when no funding source is selected.
     */
    private fun currentPayerType(): PayerType {
        val sourceId = _uiState.value.selectedFundingSource?.id ?: return PayerType.GROUP
        return try {
            PayerType.fromString(sourceId)
        } catch (_: IllegalArgumentException) {
            PayerType.GROUP
        }
    }

    /**
     * Returns the payer ID for USER scope; null for GROUP and SUBUNIT scopes.
     */
    private fun currentPayerId(): String? = when (currentPayerType()) {
        PayerType.USER -> _uiState.value.currentUserId
        PayerType.GROUP, PayerType.SUBUNIT -> null
    }
}
