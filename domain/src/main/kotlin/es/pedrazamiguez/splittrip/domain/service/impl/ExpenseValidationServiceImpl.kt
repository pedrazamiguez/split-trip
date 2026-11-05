package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.ExpenseValidationService
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory

class ExpenseValidationServiceImpl(
    private val splitCalculatorFactory: ExpenseSplitCalculatorFactory,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() }
) : ExpenseValidationService {

    override fun validateTitle(title: String): ValidationResult = when {
        title.trim().isBlank() -> ValidationResult.Invalid("Title cannot be empty")
        else -> ValidationResult.Valid
    }

    override fun validateExpenseDate(dateMillis: Long): ValidationResult {
        val now = currentTimeMillisProvider()

        return when {
            dateMillis > (now + GRACE_PERIOD_MILLIS) -> ValidationResult.Invalid(
                "Expense date and time cannot be in the future"
            )
            else -> ValidationResult.Valid
        }
    }

    override fun validateAmount(amountString: String): ValidationResult {
        val result = CurrencyConverter.parseToCents(amountString)
        return if (result.isSuccess) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                result.exceptionOrNull()?.message ?: "Invalid amount"
            )
        }
    }

    override fun validateUserCount(count: Int): ValidationResult = when {
        count <= 0 -> ValidationResult.Invalid("User count must be greater than zero")
        else -> ValidationResult.Valid
    }

    override fun validateSplits(
        splitType: SplitType,
        splits: List<ExpenseSplit>,
        totalAmountCents: Long,
        participantIds: List<String>
    ): ValidationResult = try {
        val calculator = splitCalculatorFactory.create(splitType)
        calculator.calculateShares(totalAmountCents, participantIds, splits)
        ValidationResult.Valid
    } catch (e: Exception) {
        ValidationResult.Invalid(e.message ?: "Invalid split configuration")
    }

    override fun validateAddOn(addOn: AddOn, sourceAmountCents: Long): ValidationResult {
        if (addOn.amountCents <= 0) {
            return ValidationResult.Invalid("Add-on amount must be greater than zero")
        }
        if (addOn.currency.isBlank()) {
            return ValidationResult.Invalid("Add-on currency is required")
        }
        if (addOn.mode == AddOnMode.INCLUDED && addOn.amountCents >= sourceAmountCents) {
            return ValidationResult.Invalid(
                "Included add-on amount must be less than the expense amount"
            )
        }
        return ValidationResult.Valid
    }

    override fun validateAddOns(
        addOns: List<AddOn>,
        sourceAmountCents: Long
    ): ValidationResult {
        for (addOn in addOns) {
            val result = validateAddOn(addOn, sourceAmountCents)
            if (result is ValidationResult.Invalid) return result
        }
        return ValidationResult.Valid
    }

    companion object {
        private const val GRACE_PERIOD_HOURS = 36L
        private const val MILLIS_IN_HOUR = 60L * 60L * 1000L
        const val GRACE_PERIOD_MILLIS = GRACE_PERIOD_HOURS * MILLIS_IN_HOUR
    }
}
