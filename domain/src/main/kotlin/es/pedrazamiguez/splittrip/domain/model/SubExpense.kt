package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class SubExpense(
    val id: String = "",
    val title: String = "",
    val amountCents: Long = 0L,
    val currency: String = "EUR",
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    val groupAmountCents: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val paymentStatus: PaymentStatus = PaymentStatus.FINISHED,
    val payerType: PayerType = PayerType.GROUP,
    val payerId: String? = null,
    val operationDate: LocalDateTime? = null,
    val dueDate: LocalDateTime? = null,
    val addOns: List<AddOn> = emptyList(),
    val cashTranches: List<CashTranche> = emptyList(),
    val notes: String? = null
)
