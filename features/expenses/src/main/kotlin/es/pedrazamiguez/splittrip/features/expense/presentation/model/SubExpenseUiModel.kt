package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.runtime.Immutable
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
    val currency: String = "EUR",
    val groupAmountCents: Long = 0L,
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val paymentStatus: PaymentStatus = PaymentStatus.FINISHED,
    val payerType: PayerType = PayerType.GROUP,
    val payerId: String? = null,
    val dueDate: LocalDateTime? = null,
    val operationDate: LocalDateTime? = null,
    val notes: String? = null,
    val addOns: List<AddOn> = emptyList()
)
