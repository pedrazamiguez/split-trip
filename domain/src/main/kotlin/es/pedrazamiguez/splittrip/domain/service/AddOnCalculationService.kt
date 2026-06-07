package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import java.math.BigDecimal

interface AddOnCalculationService {
    fun resolveAddOnAmountCents(
        normalizedInput: BigDecimal,
        valueType: AddOnValueType,
        decimalDigits: Int,
        sourceAmountCents: Long
    ): Long
    fun calculateIncludedDiscountPercentageCents(
        sourceAmountCents: Long,
        discountPercentage: BigDecimal
    ): Long
    fun calculateTotalOnTopAddOns(addOns: List<AddOn>): Long
    fun calculateTotalAddOnExtras(addOns: List<AddOn>): Long
    fun calculateEffectiveGroupAmount(baseGroupAmount: Long, addOns: List<AddOn>): Long
    fun calculateIncludedBaseCost(
        totalAmountCents: Long,
        includedExactCents: Long,
        totalIncludedPercentage: BigDecimal,
        includedExactDiscountCents: Long = 0L,
        totalIncludedDiscountPercentage: BigDecimal = BigDecimal.ZERO
    ): Long
    fun calculateEffectiveDeductedAmount(baseDeductedAmount: Long, addOns: List<AddOn>): Long
    fun convertGroupToSourceCents(groupAmountCents: Long, exchangeRate: BigDecimal): Long
    fun sumPercentagesFromInputs(amountInputs: List<String>): BigDecimal
}
