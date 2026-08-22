package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.parseAmountToSmallestUnit
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.collections.immutable.toImmutableList

class SubExpenseEventHandler(
    private val formattingHelper: FormattingHelper,
    private val exchangeRateCalculationService: ExchangeRateCalculationService
) {

    fun handleSubExpensesToggled(state: AddExpenseUiState): AddExpenseUiState {
        val newEnabled = !state.isSubExpensesEnabled
        val newSubExpenses = if (newEnabled && state.subExpenses.isEmpty()) {
            val currency = state.selectedCurrency?.code ?: "EUR"
            val totalCents = parseAmountToSmallestUnit(state.sourceAmount, currency)
            val half1 = totalCents / 2
            val half2 = totalCents - half1
            listOf(
                SubExpenseUiModel(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    amountInput = if (half1 > 0) formattingHelper.formatCentsValue(half1) else "",
                    currency = currency,
                    paymentMethod = state.selectedPaymentMethod?.let {
                        runCatching { PaymentMethod.fromString(it.id) }.getOrDefault(PaymentMethod.OTHER)
                    } ?: PaymentMethod.OTHER,
                    paymentStatus = PaymentStatus.FINISHED,
                    payerType = state.selectedFundingSource?.let {
                        runCatching { PayerType.fromString(it.id) }.getOrDefault(PayerType.GROUP)
                    } ?: PayerType.GROUP,
                    payerId = state.currentUserId
                ),
                SubExpenseUiModel(
                    id = UUID.randomUUID().toString(),
                    title = "",
                    amountInput = if (half2 > 0) formattingHelper.formatCentsValue(half2) else "",
                    currency = currency,
                    paymentMethod = state.selectedPaymentMethod?.let {
                        runCatching { PaymentMethod.fromString(it.id) }.getOrDefault(PaymentMethod.OTHER)
                    } ?: PaymentMethod.OTHER,
                    paymentStatus = PaymentStatus.SCHEDULED,
                    payerType = state.selectedFundingSource?.let {
                        runCatching { PayerType.fromString(it.id) }.getOrDefault(PayerType.GROUP)
                    } ?: PayerType.GROUP,
                    payerId = state.currentUserId
                )
            ).toImmutableList()
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

    fun handleSubExpenseAdded(state: AddExpenseUiState): AddExpenseUiState {
        val currency = state.selectedCurrency?.code ?: "EUR"
        val newSub = SubExpenseUiModel(
            id = UUID.randomUUID().toString(),
            title = "",
            amountInput = "",
            currency = currency,
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
        val updated = state.subExpenses.map { if (it.id == id) it.copy(amountInput = amount) else it }
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

        val currency = state.selectedCurrency?.code ?: "EUR"
        val totalSourceCents = parseAmountToSmallestUnit(state.sourceAmount, currency)
        val rate = state.displayExchangeRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        val isForeign = state.selectedCurrency?.code != state.groupCurrency?.code

        var allocatedCents = 0L
        val updatedSubExpenses = state.subExpenses.map { sub ->
            val subCents = parseAmountToSmallestUnit(sub.amountInput, currency)
            allocatedCents += subCents
            val groupCents = if (isForeign && state.displayExchangeRate.isNotBlank()) {
                exchangeRateCalculationService.convertCentsToGroupCurrencyViaDisplayRate(
                    subCents,
                    state.displayExchangeRate
                )
            } else {
                subCents
            }
            sub.copy(
                currency = currency,
                groupAmountCents = groupCents,
                exchangeRate = rate
            )
        }

        val remainingCents = totalSourceCents - allocatedCents
        val allocatedFormatted = formattingHelper.formatCentsWithCurrency(allocatedCents, currency)
        val remainingFormatted = formattingHelper.formatCentsWithCurrency(remainingCents, currency)
        val totalFormatted = formattingHelper.formatCentsWithCurrency(totalSourceCents, currency)

        val error = when {
            updatedSubExpenses.size < 2 ->
                UiText.StringResource(R.string.expense_error_sub_expenses_minimum)
            allocatedCents != totalSourceCents ->
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
}
