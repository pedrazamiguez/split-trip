package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseValidationService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy.ExpenseFlowStrategy
import java.math.BigDecimal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles expense form submission.
 *
 * Validates the form, maps to a domain object, applies INCLUDED add-on base-cost
 * decomposition via [ExpenseCalculatorService], and delegates to the use case.
 */
class SubmitEventHandler(
    private val expenseValidationService: ExpenseValidationService,
    private val addOnCalculationService: AddOnCalculationService,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val remainderDistributionService: RemainderDistributionService,
    private val addExpenseUiMapper: AddExpenseUiMapper,
    private val submitResultDelegate: SubmitResultDelegate
) : AddExpenseEventHandler {

    private lateinit var strategy: ExpenseFlowStrategy

    fun setStrategy(strategy: ExpenseFlowStrategy) {
        this.strategy = strategy
    }

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

    // Sequential validation → map → submit → error-handling pipeline;
    // each branch is a distinct validation/error case
    @Suppress("CognitiveComplexMethod", "LongMethod", "ReturnCount")
    fun submitExpense(groupId: String?, onSuccess: () -> Unit) {
        Timber.d(
            "submitExpense: entry groupId=%s isEditMode=%s currentStep=%s",
            groupId,
            _uiState.value.isEditMode,
            _uiState.value.currentStep
        )
        val effectiveGroupId = groupId ?: _uiState.value.loadedGroupId
        if (effectiveGroupId == null) {
            Timber.w("submitExpense: both groupId parameter and loadedGroupId are null, aborting submission")
            return
        }

        val currentState = _uiState.value

        // Validate title using domain service
        val titleValidation = expenseValidationService.validateTitle(currentState.expenseTitle)
        if (titleValidation is ValidationResult.Invalid) {
            Timber.w("submitExpense: title validation failed — length=%d", currentState.expenseTitle.length)
            val errorText = UiText.StringResource(R.string.expense_error_title_empty)
            _uiState.update {
                it.copy(
                    isTitleValid = false,
                    error = errorText
                )
            }
            scope.launch {
                _actions.emit(AddExpenseUiAction.ShowError(errorText))
            }
            return
        }

        // Validate amount using domain service
        val amountValidation = expenseValidationService.validateAmount(currentState.sourceAmount)
        if (amountValidation is ValidationResult.Invalid) {
            Timber.w(
                "submitExpense: amount validation failed — reason=%s",
                amountValidation.message
            )
            val errorText = UiText.DynamicString(amountValidation.message)
            _uiState.update {
                it.copy(
                    isAmountValid = false,
                    error = errorText
                )
            }
            scope.launch {
                _actions.emit(AddExpenseUiAction.ShowError(errorText))
            }
            return
        }

        // Validate due date when payment status is SCHEDULED
        if (currentState.selectedPaymentStatus?.id == PaymentStatus.SCHEDULED.name &&
            currentState.dueDateMillis == null
        ) {
            Timber.w(
                "submitExpense: due-date required but null — paymentStatus=%s",
                currentState.selectedPaymentStatus?.id
            )
            val errorText = UiText.StringResource(R.string.expense_error_due_date_required)
            _uiState.update {
                it.copy(
                    isDueDateValid = false,
                    error = errorText
                )
            }
            scope.launch {
                _actions.emit(AddExpenseUiAction.ShowError(errorText))
            }
            return
        }

        // Validate expense date/time when not scheduled
        if (!currentState.showDueDateSection) {
            val dateValidation = expenseValidationService.validateExpenseDate(
                currentState.expenseDateMillis ?: System.currentTimeMillis()
            )
            if (dateValidation is ValidationResult.Invalid) {
                Timber.w(
                    "submitExpense: expense-date validation failed — expenseDateMillis=%d reason=%s",
                    currentState.expenseDateMillis,
                    dateValidation.message
                )
                // Future date validation is demoted to a soft warning.
                // We show a top pill warning and mark the field invalid visually, but do not block submission.
                val warningText = UiText.StringResource(R.string.expense_error_date_future)
                _uiState.update {
                    it.copy(
                        isExpenseDateValid = false
                    )
                }
                scope.launch {
                    _actions.emit(AddExpenseUiAction.ShowPill(warningText))
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExpenseDateValid = true
                    )
                }
            }
        }

        // Validate add-ons (only those with non-empty input)
        val addOnsWithInput = currentState.addOns.filter { it.amountInput.isNotBlank() }
        if (addOnsWithInput.any { it.resolvedAmountCents <= 0 }) {
            Timber.w(
                "submitExpense: add-on validation failed — invalidAddOns=%d",
                addOnsWithInput.count { it.resolvedAmountCents <= 0 }
            )
            val errorText = UiText.StringResource(
                R.string.add_expense_add_on_error_amount
            )
            _uiState.update {
                it.copy(
                    addOnError = errorText
                )
            }
            scope.launch {
                _actions.emit(AddExpenseUiAction.ShowError(errorText))
            }
            return
        }

        Timber.d("submitExpense: validations passed, mapping to domain")
        _uiState.update { it.copy(isLoading = true, error = null) }

        addExpenseUiMapper.mapToDomain(_uiState.value, effectiveGroupId).onSuccess { expense ->
            val withIncludedAdj = adjustForIncludedAddOns(expense, _uiState.value.addOns)
            val adjustedExpense = adjustForOnTopDiscounts(withIncludedAdj)
            Timber.d("submitExpense: domain mapping ok, delegating to strategy.saveExpense")
            scope.launch {
                strategy.saveExpense(
                    groupId = effectiveGroupId,
                    expense = adjustedExpense,
                    uiState = currentState
                ).onSuccess {
                    Timber.d("submitExpense: strategy.saveExpense succeeded")
                    submitResultDelegate.handleSuccess(_uiState, effectiveGroupId, onSuccess)
                }.onFailure { e ->
                    Timber.e(e, "submitExpense: strategy.saveExpense failed")
                    submitResultDelegate.handleFailure(e, _uiState, _actions, currentState)
                }
            }
        }.onFailure { e ->
            Timber.e(e, "submitExpense: failed to map expense to domain")
            val errorText = UiText.DynamicString(e.message ?: "Unknown error")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = errorText
                )
            }
            scope.launch {
                _actions.emit(AddExpenseUiAction.ShowError(errorText))
            }
        }
    }

    // ── INCLUDED Add-On Base Cost Extraction ──────────────────────────────

    /**
     * When INCLUDED non-discount add-ons are present, adjusts the mapped [expense]
     * to store the extracted **base cost** instead of the full user-entered total.
     *
     * - Computes the base cost via [computeBaseCosts].
     * - Redistributes PERCENTAGE INCLUDED add-on amounts via [adjustIncludedPercentageAddOns]
     *   using a residual approach to guarantee `base + includedExact + sum(includedPct) == total`.
     * - Rescales splits proportionally to the new base source amount.
     *
     * INCLUDED discounts are **informational only** — the user already entered the
     * discounted price — and are therefore excluded from base-cost extraction.
     *
     * When no INCLUDED non-discount add-ons exist, returns the expense unchanged.
     */
    internal fun adjustForIncludedAddOns(
        expense: Expense,
        uiAddOns: ImmutableList<AddOnUiModel>
    ): Expense {
        val includedNonDiscount = expense.addOns.filter {
            it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
        }
        if (includedNonDiscount.isEmpty()) return expense

        val (baseCostGroup, baseCostSource) = computeBaseCosts(expense, uiAddOns)
        val adjustedAddOns = adjustIncludedPercentageAddOns(expense, uiAddOns, baseCostGroup)
        val adjustedSplits = rescaleSplits(expense.splits, expense.sourceAmount, baseCostSource)

        return expense.copy(
            sourceAmount = baseCostSource,
            groupAmount = baseCostGroup,
            addOns = adjustedAddOns,
            splits = adjustedSplits
        )
    }

    /**
     * Derives base costs in both group and source currencies from the user-entered total.
     *
     * Returns `(baseCostGroup, baseCostSource)`.
     */
    private fun computeBaseCosts(
        expense: Expense,
        uiAddOns: ImmutableList<AddOnUiModel>
    ): Pair<Long, Long> {
        val includedNonDiscount = expense.addOns.filter {
            it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT
        }
        val includedExactGroupCents = includedNonDiscount
            .filter { it.valueType == AddOnValueType.EXACT }
            .sumOf { it.groupAmountCents }

        val totalIncludedPercentage = addOnCalculationService.sumPercentagesFromInputs(
            uiAddOns
                .filter {
                    it.mode == AddOnMode.INCLUDED &&
                        it.type != AddOnType.DISCOUNT &&
                        it.valueType == AddOnValueType.PERCENTAGE &&
                        it.resolvedAmountCents > 0
                }
                .map { it.amountInput }
        )

        val baseCostGroup = addOnCalculationService.calculateIncludedBaseCost(
            totalAmountCents = expense.groupAmount,
            includedExactCents = includedExactGroupCents,
            totalIncludedPercentage = totalIncludedPercentage
        )
        val baseCostSource = expenseCalculatorService.computeProportionalAmount(
            amount = expense.sourceAmount,
            targetAmount = baseCostGroup,
            totalAmount = expense.groupAmount
        )
        return baseCostGroup to baseCostSource
    }

    /**
     * Recomputes INCLUDED PERCENTAGE add-on amounts using a **residual approach**:
     *
     *   `residual = originalGroupAmount − includedExactCents − baseCostGroup`
     *
     * The residual is distributed proportionally across all INCLUDED PERCENTAGE add-ons
     * (floor rounding + one-cent remainder redistribution). This guarantees that
     * `base + includedExact + sum(includedPct) == originalGroupAmount` exactly, with
     * no rounding drift from independent `base × pct / 100` recomputation.
     */
    private fun adjustIncludedPercentageAddOns(
        expense: Expense,
        uiAddOns: ImmutableList<AddOnUiModel>,
        baseCostGroup: Long
    ): List<AddOn> {
        val includedExactCents = expense.addOns
            .filter {
                it.mode == AddOnMode.INCLUDED &&
                    it.type != AddOnType.DISCOUNT &&
                    it.valueType == AddOnValueType.EXACT
            }
            .sumOf { it.groupAmountCents }
        val percentageResidual = (expense.groupAmount - includedExactCents - baseCostGroup)
            .coerceAtLeast(0L)

        val pctAddOns = expense.addOns.filter {
            it.mode == AddOnMode.INCLUDED &&
                it.type != AddOnType.DISCOUNT &&
                it.valueType == AddOnValueType.PERCENTAGE
        }
        if (pctAddOns.isEmpty()) return expense.addOns

        val weights = pctAddOns.map { addOn ->
            val ui = uiAddOns.find { it.id == addOn.id }
            val inputStr = ui?.amountInput?.trim() ?: ""
            val normalized = CurrencyConverter.normalizeAmountString(inputStr)
            normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }

        val newGroupCents = remainderDistributionService.distributeByWeights(
            percentageResidual,
            weights
        )

        val allocationsById = pctAddOns.mapIndexed { i, addOn ->
            addOn.id to newGroupCents[i]
        }.toMap()
        return expense.addOns.map { addOn ->
            val newGroupAmountCents = allocationsById[addOn.id] ?: return@map addOn
            val newAmountCents = addOnCalculationService.convertGroupToSourceCents(
                groupAmountCents = newGroupAmountCents,
                exchangeRate = addOn.exchangeRate
            )
            addOn.copy(amountCents = newAmountCents, groupAmountCents = newGroupAmountCents)
        }
    }

    // ── ON_TOP Discount Baking ──────────────────────────────────────────

    /**
     * Bakes ON_TOP discount amounts into [Expense.groupAmount] and [Expense.sourceAmount].
     *
     * Unlike tips/fees/surcharges that are stored as separate add-ons and
     * reconstructed via [AddOnCalculationService.calculateEffectiveGroupAmount],
     * discounts reduce the stored expense amount directly so the expense list
     * and balance calculations reflect the discounted price without a separate
     * "discount" line item.
     *
     * After baking, each ON_TOP discount add-on's [AddOn.groupAmountCents] is
     * set to 0 to prevent [AddOnCalculationService.calculateEffectiveGroupAmount] from subtracting
     * the discount a second time. The original amount is still recoverable
     * from [AddOn.amountCents] and [AddOn.exchangeRate].
     *
     * When no ON_TOP discounts exist, returns the expense unchanged.
     */
    internal fun adjustForOnTopDiscounts(expense: Expense): Expense {
        val onTopDiscountCents = expense.addOns
            .filter { it.type == AddOnType.DISCOUNT && it.mode == AddOnMode.ON_TOP }
            .sumOf { it.groupAmountCents }
        if (onTopDiscountCents <= 0) return expense

        val newGroupAmount = (expense.groupAmount - onTopDiscountCents).coerceAtLeast(0L)
        val newSourceAmount = expenseCalculatorService.computeProportionalAmount(
            amount = expense.sourceAmount,
            targetAmount = newGroupAmount,
            totalAmount = expense.groupAmount
        )
        val adjustedSplits = rescaleSplits(expense.splits, expense.sourceAmount, newSourceAmount)
        val adjustedAddOns = expense.addOns.map { addOn ->
            if (addOn.type == AddOnType.DISCOUNT && addOn.mode == AddOnMode.ON_TOP) {
                addOn.copy(groupAmountCents = 0L)
            } else {
                addOn
            }
        }

        return expense.copy(
            sourceAmount = newSourceAmount,
            groupAmount = newGroupAmount,
            addOns = adjustedAddOns,
            splits = adjustedSplits
        )
    }

    /**
     * Rescales split amounts proportionally from [originalTotal] to [newTotal].
     *
     * Both [originalTotal] and [newTotal] must be in **source currency** because
     * [ExpenseSplit.amountCents] is stored in source currency. The balance layer
     * converts splits to group currency at read time.
     *
     * Uses floor rounding for each split, then distributes the remainder
     * (one unit at a time) to the first splits to ensure the sum equals
     * [newTotal] exactly (conservation of currency).
     */
    private fun rescaleSplits(
        splits: List<ExpenseSplit>,
        originalTotal: Long,
        newTotal: Long
    ): List<ExpenseSplit> {
        if (originalTotal == newTotal || originalTotal <= 0 || splits.isEmpty()) return splits

        val amounts = splits.map { it.amountCents }
        val isExcluded = splits.map { it.isExcluded }
        val rescaled = remainderDistributionService.rescaleAmounts(
            originalTotal = originalTotal,
            newTotal = newTotal,
            amounts = amounts,
            isExcluded = isExcluded
        )

        return splits.mapIndexed { index, split ->
            split.copy(amountCents = rescaled[index])
        }
    }
}
