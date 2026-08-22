package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.parseAmountToSmallestUnit
import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Suppress("TooManyFunctions")
class SubExpenseEventHandler(
    private val formattingHelper: FormattingHelper,
    private val exchangeRateCalculationService: ExchangeRateCalculationService
) {

    fun handleSubExpensesToggled(state: AddExpenseUiState): AddExpenseUiState {
        val newEnabled = !state.isSubExpensesEnabled
        val newSubExpenses = if (newEnabled && state.subExpenses.isEmpty()) {
            createInitialTranches(state)
        } else {
            state.subExpenses
        }

        return recalculateSubExpenses(
            state.copy(
                isSubExpensesEnabled = newEnabled,
                subExpenses = newSubExpenses
            )
        )
    }

    private fun createInitialTranches(state: AddExpenseUiState): ImmutableList<SubExpenseUiModel> {
        val currencyModel = state.selectedCurrency
        val currencyCode = currencyModel?.code ?: "EUR"
        val totalCents = parseAmountToSmallestUnit(state.sourceAmount, currencyCode)
        val half1 = totalCents / 2
        val half2 = totalCents - half1
        val isForeign = currencyCode != state.groupCurrency?.code
        val initialRate = if (isForeign && state.displayExchangeRate.isNotBlank()) {
            state.displayExchangeRate
        } else {
            "1.0"
        }
        val defaultPaymentMethod = state.selectedPaymentMethod?.let {
            runCatching { PaymentMethod.fromString(it.id) }.getOrDefault(PaymentMethod.OTHER)
        } ?: PaymentMethod.OTHER
        val defaultPayerType = state.selectedFundingSource?.let {
            runCatching { PayerType.fromString(it.id) }.getOrDefault(PayerType.GROUP)
        } ?: PayerType.GROUP

        return listOf(
            SubExpenseUiModel(
                id = UUID.randomUUID().toString(),
                title = "",
                amountInput = if (half1 > 0) formattingHelper.formatCentsValue(half1) else "",
                currency = currencyModel,
                currencyCode = currencyCode,
                displayExchangeRate = initialRate,
                showExchangeRateSection = isForeign,
                paymentMethod = defaultPaymentMethod,
                paymentStatus = PaymentStatus.FINISHED,
                payerType = defaultPayerType,
                payerId = state.currentUserId
            ),
            SubExpenseUiModel(
                id = UUID.randomUUID().toString(),
                title = "",
                amountInput = if (half2 > 0) formattingHelper.formatCentsValue(half2) else "",
                currency = currencyModel,
                currencyCode = currencyCode,
                displayExchangeRate = initialRate,
                showExchangeRateSection = isForeign,
                paymentMethod = defaultPaymentMethod,
                paymentStatus = PaymentStatus.SCHEDULED,
                payerType = defaultPayerType,
                payerId = state.currentUserId
            )
        ).toImmutableList()
    }

    fun handleSubExpenseAdded(state: AddExpenseUiState): AddExpenseUiState {
        val currencyModel = state.selectedCurrency
        val currencyCode = currencyModel?.code ?: "EUR"
        val isForeign = currencyCode != state.groupCurrency?.code
        val initialRate = if (isForeign && state.displayExchangeRate.isNotBlank()) {
            state.displayExchangeRate
        } else {
            "1.0"
        }

        val newSub = SubExpenseUiModel(
            id = UUID.randomUUID().toString(),
            title = "",
            amountInput = "",
            currency = currencyModel,
            currencyCode = currencyCode,
            displayExchangeRate = initialRate,
            showExchangeRateSection = isForeign,
            paymentMethod = state.selectedPaymentMethod?.let {
                runCatching { PaymentMethod.fromString(it.id) }.getOrDefault(PaymentMethod.OTHER)
            } ?: PaymentMethod.OTHER,
            paymentStatus = PaymentStatus.FINISHED,
            payerType = state.selectedFundingSource?.let {
                runCatching { PayerType.fromString(it.id) }.getOrDefault(PayerType.GROUP)
            } ?: PayerType.GROUP,
            payerId = state.currentUserId
        )
        return recalculateSubExpenses(
            state.copy(subExpenses = (state.subExpenses + newSub).toImmutableList())
        )
    }

    fun handleSubExpenseRemoved(state: AddExpenseUiState, id: String): AddExpenseUiState {
        return recalculateSubExpenses(
            state.copy(subExpenses = state.subExpenses.filterNot { it.id == id }.toImmutableList())
        )
    }

    fun handleSubExpenseTitleChanged(state: AddExpenseUiState, id: String, title: String): AddExpenseUiState {
        val updated = state.subExpenses.map { if (it.id == id) it.copy(title = title) else it }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun handleSubExpenseAmountChanged(
        state: AddExpenseUiState,
        id: String,
        amount: String
    ): AddExpenseUiState {
        val totalSourceCents = parseAmountToSmallestUnit(
            state.sourceAmount,
            state.selectedCurrency?.code ?: "EUR"
        )

        // Smart rebalancing: if there are 2 tranches and user edits the first, auto-fill the second
        val updated = if (state.subExpenses.size == 2 && state.subExpenses[0].id == id) {
            val editedCents = parseAmountToSmallestUnit(amount, state.subExpenses[0].resolvedCurrencyCode)
            val counterpartCents = (totalSourceCents - editedCents).coerceAtLeast(0L)
            val counterpartFormatted = if (counterpartCents > 0) {
                formattingHelper.formatCentsValue(counterpartCents)
            } else {
                ""
            }
            listOf(
                state.subExpenses[0].copy(amountInput = amount),
                state.subExpenses[1].copy(amountInput = counterpartFormatted)
            )
        } else {
            state.subExpenses.map { if (it.id == id) it.copy(amountInput = amount) else it }
        }

        return recalculateSubExpenses(state.copy(subExpenses = updated.toImmutableList()))
    }

    fun handleSubExpenseCurrencySelected(
        state: AddExpenseUiState,
        id: String,
        currencyCode: String
    ): AddExpenseUiState {
        val currencyModel = state.availableCurrencies.find { it.code == currencyCode }
        val isForeign = currencyCode != state.groupCurrency?.code
        val defaultRate = if (currencyCode == state.selectedCurrency?.code && state.displayExchangeRate.isNotBlank()) {
            state.displayExchangeRate
        } else {
            "1.0"
        }

        val updated = state.subExpenses.map { sub ->
            if (sub.id == id) {
                sub.copy(
                    currency = currencyModel,
                    currencyCode = currencyCode,
                    showExchangeRateSection = isForeign,
                    displayExchangeRate = defaultRate
                )
            } else {
                sub
            }
        }
        return recalculateSubExpenses(state.copy(subExpenses = updated.toImmutableList()))
    }

    fun handleSubExpenseExchangeRateChanged(
        state: AddExpenseUiState,
        id: String,
        rate: String
    ): AddExpenseUiState {
        val updated = state.subExpenses.map { sub ->
            if (sub.id == id) sub.copy(displayExchangeRate = rate) else sub
        }
        return recalculateSubExpenses(state.copy(subExpenses = updated.toImmutableList()))
    }

    fun handleSubExpenseGroupAmountChanged(
        state: AddExpenseUiState,
        id: String,
        groupAmount: String
    ): AddExpenseUiState {
        val targetSub = state.subExpenses.find { it.id == id } ?: return state
        val targetCurrency = targetSub.resolvedCurrencyCode
        val sourceCents = parseAmountToSmallestUnit(targetSub.amountInput, targetCurrency)
        val groupCents = parseAmountToSmallestUnit(groupAmount, state.groupCurrency?.code ?: "EUR")

        val newRate = if (sourceCents > 0 && groupCents > 0) {
            BigDecimal(sourceCents).divide(
                BigDecimal(groupCents),
                DomainConstants.RATE_PRECISION,
                RoundingMode.HALF_UP
            ).stripTrailingZeros().toPlainString()
        } else {
            targetSub.displayExchangeRate
        }

        val updated = state.subExpenses.map { sub ->
            if (sub.id == id) sub.copy(displayExchangeRate = newRate) else sub
        }
        return recalculateSubExpenses(state.copy(subExpenses = updated.toImmutableList()))
    }

    fun handleSubExpenseAutoFillRemaining(
        state: AddExpenseUiState,
        id: String
    ): AddExpenseUiState {
        val baseCurrency = state.selectedCurrency?.code ?: "EUR"
        val totalSourceCents = parseAmountToSmallestUnit(state.sourceAmount, baseCurrency)
        val otherCents = state.subExpenses
            .filterNot { it.id == id }
            .sumOf { parseAmountToSmallestUnit(it.amountInput, it.resolvedCurrencyCode) }

        val remainingCents = (totalSourceCents - otherCents).coerceAtLeast(0L)
        val formatted = if (remainingCents > 0) formattingHelper.formatCentsValue(remainingCents) else ""

        val updated = state.subExpenses.map { sub ->
            if (sub.id == id) sub.copy(amountInput = formatted) else sub
        }
        return recalculateSubExpenses(state.copy(subExpenses = updated.toImmutableList()))
    }

    fun handleSubExpensePaymentMethodSelected(
        state: AddExpenseUiState,
        id: String,
        methodId: String
    ): AddExpenseUiState {
        val method = runCatching { PaymentMethod.fromString(methodId) }.getOrDefault(PaymentMethod.OTHER)
        val updated = state.subExpenses.map { if (it.id == id) it.copy(paymentMethod = method) else it }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun handleSubExpensePaymentStatusSelected(
        state: AddExpenseUiState,
        id: String,
        statusId: String
    ): AddExpenseUiState {
        val status = runCatching { PaymentStatus.fromString(statusId) }.getOrDefault(PaymentStatus.FINISHED)
        val updated = state.subExpenses.map { if (it.id == id) it.copy(paymentStatus = status) else it }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun handleSubExpenseDueDateSelected(
        state: AddExpenseUiState,
        id: String,
        dateMillis: Long
    ): AddExpenseUiState {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateMillis), ZoneId.systemDefault())
        val updated = state.subExpenses.map { if (it.id == id) it.copy(dueDate = date) else it }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun handleSubExpenseOperationDateSelected(
        state: AddExpenseUiState,
        id: String,
        dateMillis: Long
    ): AddExpenseUiState {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateMillis), ZoneId.systemDefault())
        val updated = state.subExpenses.map { if (it.id == id) it.copy(operationDate = date) else it }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun handleSubExpensePayerSelected(
        state: AddExpenseUiState,
        id: String,
        payerType: PayerType,
        payerId: String?
    ): AddExpenseUiState {
        val updated = state.subExpenses.map {
            if (it.id == id) it.copy(payerType = payerType, payerId = payerId) else it
        }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun handleSubExpenseNotesChanged(state: AddExpenseUiState, id: String, notes: String): AddExpenseUiState {
        val updated = state.subExpenses.map { if (it.id == id) it.copy(notes = notes) else it }
        return state.copy(subExpenses = updated.toImmutableList())
    }

    fun recalculateSubExpenses(state: AddExpenseUiState): AddExpenseUiState {
        if (!state.isSubExpensesEnabled) {
            return state.copy(
                subExpensesError = null,
                subExpensesAllocatedFormatted = "",
                subExpensesRemainingFormatted = ""
            )
        }

        val baseCurrency = state.selectedCurrency?.code ?: "EUR"
        val groupCurrencyCode = state.groupCurrency?.code ?: "EUR"
        val totalSourceCents = parseAmountToSmallestUnit(state.sourceAmount, baseCurrency)

        var allocatedSourceCents = 0L
        val updatedSubExpenses = state.subExpenses.map { sub ->
            val (updatedSub, subCents) = resolveSubExpenseCalculation(sub, groupCurrencyCode, state)
            allocatedSourceCents += subCents
            updatedSub
        }

        val remainingCents = totalSourceCents - allocatedSourceCents
        val allocatedFormatted = formattingHelper.formatCentsWithCurrency(allocatedSourceCents, baseCurrency)
        val remainingFormatted = formattingHelper.formatCentsWithCurrency(remainingCents, baseCurrency)
        val totalFormatted = formattingHelper.formatCentsWithCurrency(totalSourceCents, baseCurrency)

        val error = when {
            updatedSubExpenses.size < 2 ->
                UiText.StringResource(R.string.expense_error_sub_expenses_minimum)
            allocatedSourceCents != totalSourceCents ->
                UiText.StringResource(R.string.expense_error_sub_expenses_sum, allocatedFormatted, totalFormatted)
            else -> null
        }

        return state.copy(
            subExpenses = updatedSubExpenses.toImmutableList(),
            subExpensesAllocatedFormatted = allocatedFormatted,
            subExpensesRemainingFormatted = remainingFormatted,
            subExpensesError = error
        )
    }

    private fun resolveSubExpenseCalculation(
        sub: SubExpenseUiModel,
        groupCurrencyCode: String,
        state: AddExpenseUiState
    ): Pair<SubExpenseUiModel, Long> {
        val currencyCode = sub.resolvedCurrencyCode
        val isForeign = currencyCode != groupCurrencyCode
        val subCents = parseAmountToSmallestUnit(sub.amountInput, currencyCode)

        val normalizedRateStr = CurrencyConverter.normalizeAmountString(sub.displayExchangeRate.trim())
        val displayRate = normalizedRateStr.toBigDecimalOrNull() ?: BigDecimal.ONE

        val groupCents = if (isForeign && displayRate.compareTo(BigDecimal.ZERO) != 0) {
            val converted = exchangeRateCalculationService.convertCentsToGroupCurrencyViaDisplayRate(
                subCents,
                sub.displayExchangeRate
            )
            if (converted == 0L && subCents > 0L) {
                BigDecimal(subCents).divide(displayRate, 0, RoundingMode.HALF_UP).toLong()
            } else {
                converted
            }
        } else {
            subCents
        }

        val internalRate = if (displayRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal.ONE.divide(displayRate, DomainConstants.RATE_PRECISION, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ONE
        }

        val calcGroupAmount = formattingHelper.formatCentsValue(groupCents)
        val updatedSub = sub.copy(
            currencyCode = currencyCode,
            currency = sub.currency ?: state.availableCurrencies.find { it.code == currencyCode },
            groupAmountCents = groupCents,
            calculatedGroupAmount = calcGroupAmount,
            exchangeRate = internalRate,
            isAmountValid = subCents > 0
        )
        return updatedSub to subCents
    }
}
