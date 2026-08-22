package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import java.math.BigDecimal
import java.time.LocalDateTime

@Immutable
data class SubExpenseUiModel(
    val id: String = "",
    val title: String = "",
    val amountInput: String = "",
    val currency: CurrencyUiModel? = null,
    val currencyCode: String = "EUR",
    val displayExchangeRate: String = "1.0",
    val showExchangeRateSection: Boolean = false,
    val isExchangeRateLocked: Boolean = false,
    val calculatedGroupAmount: String = "",
    val exchangeRateLabel: String = "",
    val groupAmountLabel: String = "",
    val groupAmountCents: Long = 0L,
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val paymentStatus: PaymentStatus = PaymentStatus.FINISHED,
    val payerType: PayerType = PayerType.GROUP,
    val payerId: String? = null,
    val dueDate: LocalDateTime? = null,
    val operationDate: LocalDateTime? = null,
    val notes: String? = null,
    val addOns: List<AddOn> = emptyList(),
    val isAmountValid: Boolean = true
) {
    /** Resolved currency code. */
    val resolvedCurrencyCode: String
        get() = currency?.code ?: currencyCode
}
