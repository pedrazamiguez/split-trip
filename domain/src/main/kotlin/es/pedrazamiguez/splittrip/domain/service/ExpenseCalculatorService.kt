package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import java.math.BigDecimal

interface ExpenseCalculatorService {
    companion object {
        const val DEFAULT_DECIMAL_PLACES = 2
    }
    fun centsToBigDecimal(cents: Long, decimalPlaces: Int = DEFAULT_DECIMAL_PLACES): BigDecimal
    fun centsToBigDecimalString(cents: Long, decimalPlaces: Int = DEFAULT_DECIMAL_PLACES): String
    fun computeProportionalAmount(amount: Long, targetAmount: Long, totalAmount: Long): Long
    fun distributeAmount(
        totalAmount: BigDecimal,
        numberOfUsers: Int,
        decimalPlaces: Int = DEFAULT_DECIMAL_PLACES
    ): List<BigDecimal>
    fun hasInsufficientCash(amountToCover: Long, availableWithdrawals: List<CashWithdrawal>): Boolean
    fun calculateFifoCashAmount(
        amountToCover: Long,
        availableWithdrawals: List<CashWithdrawal>
    ): FifoCashResult

    data class FifoCashResult(val groupAmountCents: Long, val tranches: List<CashTranche>)
}
