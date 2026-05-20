package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",

    // 1. Source (Wallet)
    val sourceAmount: Long = 0,
    val sourceCurrency: String = "EUR",

    // 2. Target (Group Debt)
    val groupAmount: Long = 0,
    val groupCurrency: String = "EUR",

    // 3. Bridge (Rate)
    val exchangeRate: BigDecimal = BigDecimal.ONE,

    // 4. Add-Ons (structured fees, tips, surcharges, discounts)
    val addOns: List<AddOn> = emptyList(),

    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val vendor: String? = null,
    val notes: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val paymentStatus: PaymentStatus = PaymentStatus.FINISHED,
    val dueDate: LocalDateTime? = null,
    val receiptAttachment: ReceiptAttachment? = null,
    val cashTranches: List<CashTranche> = emptyList(),
    val splitType: SplitType = SplitType.EQUAL,
    val splits: List<ExpenseSplit> = emptyList(),
    val createdBy: String = "",
    val payerType: PayerType = PayerType.GROUP,
    val payerId: String? = null,
    val createdAt: LocalDateTime? = null,
    val lastUpdatedAt: LocalDateTime? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
