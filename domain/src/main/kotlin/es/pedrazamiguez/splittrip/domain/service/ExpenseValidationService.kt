package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.ValidationResult

interface ExpenseValidationService {
    fun validateTitle(title: String): ValidationResult
    fun validateExpenseDate(dateMillis: Long): ValidationResult
    fun validateAmount(amountString: String): ValidationResult
    fun validateUserCount(count: Int): ValidationResult
    fun validateSplits(
        splitType: SplitType,
        splits: List<ExpenseSplit>,
        totalAmountCents: Long,
        participantIds: List<String>
    ): ValidationResult
    fun validateAddOn(addOn: AddOn, sourceAmountCents: Long): ValidationResult
    fun validateAddOns(
        addOns: List<AddOn>,
        sourceAmountCents: Long
    ): ValidationResult
}
