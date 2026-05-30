package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

class ExpenseValidationService(private val splitCalculatorFactory: ExpenseSplitCalculatorFactory) {

    fun validateTitle(title: String): ValidationResult = when {
        title.isBlank() -> ValidationResult.Invalid("Title cannot be empty")
        else -> ValidationResult.Valid
    }

    fun validateExpenseDate(dateMillis: Long): ValidationResult {
        val currentLocalAsUtcMillis = LocalDateTime.now()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
        val gracePeriodMillis = 36 * 60 * 60 * 1000L // 36 hours grace period

        return when {
            dateMillis > (currentLocalAsUtcMillis + gracePeriodMillis) -> ValidationResult.Invalid(
                "Expense date and time cannot be in the future"
            )
            else -> ValidationResult.Valid
        }
    }

    fun validateAmount(amountString: String): ValidationResult {
        val result = CurrencyConverter.parseToCents(amountString)
        return if (result.isSuccess) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                result.exceptionOrNull()?.message ?: "Invalid amount"
            )
        }
    }

    /**
     * Validates that the number of users is positive before performing
     * division-based operations (e.g., equal split).
     *
     * @param count The number of users to split an expense among.
     * @return [ValidationResult.Valid] if count > 0, otherwise [ValidationResult.Invalid].
     */
    fun validateUserCount(count: Int): ValidationResult = when {
        count <= 0 -> ValidationResult.Invalid("User count must be greater than zero")
        else -> ValidationResult.Valid
    }

    /**
     * Validates expense splits by delegating to the appropriate strategy's validation.
     *
     * @param splitType       The split strategy being used.
     * @param splits          The user-provided split data.
     * @param totalAmountCents The total expense amount in cents.
     * @param participantIds  The user IDs of all active (non-excluded) participants.
     * @return [ValidationResult.Valid] if the splits are valid, otherwise [ValidationResult.Invalid].
     */
    fun validateSplits(
        splitType: SplitType,
        splits: List<ExpenseSplit>,
        totalAmountCents: Long,
        participantIds: List<String>
    ): ValidationResult = try {
        val calculator = splitCalculatorFactory.create(splitType)
        // calculateShares calls validate() internally via Template Method
        calculator.calculateShares(totalAmountCents, participantIds, splits)
        ValidationResult.Valid
    } catch (e: Exception) {
        ValidationResult.Invalid(e.message ?: "Invalid split configuration")
    }

    /**
     * Validates a single add-on.
     *
     * Rules:
     * - [AddOn.amountCents] must be > 0.
     * - [AddOn.currency] must not be blank.
     * - For [AddOnMode.INCLUDED] add-ons, [AddOn.amountCents] must be < [sourceAmountCents]
     *   (can't extract more than the total).
     */
    fun validateAddOn(addOn: AddOn, sourceAmountCents: Long): ValidationResult {
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

    /**
     * Validates all add-ons in the list.
     * Returns the first invalid result, or [ValidationResult.Valid] if all pass.
     */
    fun validateAddOns(
        addOns: List<AddOn>,
        sourceAmountCents: Long
    ): ValidationResult {
        for (addOn in addOns) {
            val result = validateAddOn(addOn, sourceAmountCents)
            if (result is ValidationResult.Invalid) return result
        }
        return ValidationResult.Valid
    }
}
