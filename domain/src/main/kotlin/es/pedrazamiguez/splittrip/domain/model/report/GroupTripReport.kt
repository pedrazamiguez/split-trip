package es.pedrazamiguez.splittrip.domain.model.report

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class GroupTripReport(
    val group: Group,
    val period: SpendingInsightsPeriod,
    val windowStartDate: LocalDate?,
    val windowEndDate: LocalDate?,
    val generatedAt: LocalDateTime,
    val baseCurrency: String,
    val pocketBalance: GroupPocketBalance,
    val totalContributedAmount: BigDecimal,
    val totalExpensesAmount: BigDecimal,
    val virtualPocketBalanceAmount: BigDecimal,
    val totalCashEquivalentAmount: BigDecimal,
    val totalExtrasAmount: BigDecimal,
    val expenses: List<TripReportExpenseItem>,
    val contributions: List<TripReportContributionItem>,
    val withdrawals: List<TripReportWithdrawalItem>,
    val memberBalances: List<TripReportMemberBalanceItem>,
    val subunitBalances: List<TripReportSubunitBalanceItem>,
    val settlements: List<TripReportSettlementItem>,
    val memberProfiles: Map<String, User>,
    val subunits: Map<String, Subunit>,
    val currentUserId: String?
)

data class TripReportExpenseItem(
    val expense: Expense,
    val sourceAmount: BigDecimal,
    val baseGroupAmount: BigDecimal,
    val addOnExtrasCents: Long,
    val addOnExtrasAmount: BigDecimal,
    val effectiveGroupAmountCents: Long,
    val effectiveGroupAmount: BigDecimal,
    val exchangeRate: BigDecimal
)

data class TripReportContributionItem(
    val contribution: Contribution,
    val amount: BigDecimal,
    val equivalentBaseAmountCents: Long,
    val equivalentBaseAmount: BigDecimal
)

data class TripReportWithdrawalItem(
    val withdrawal: CashWithdrawal,
    val amountWithdrawn: BigDecimal,
    val remainingAmount: BigDecimal,
    val deductedBaseAmount: BigDecimal,
    val effectiveDeductedBaseCents: Long,
    val effectiveDeductedBaseAmount: BigDecimal,
    val addOnFeesCents: Long,
    val addOnFeesAmount: BigDecimal
)

data class TripReportMemberBalanceItem(
    val memberBalance: MemberBalance,
    val contributedAmount: BigDecimal,
    val withdrawnAmount: BigDecimal,
    val cashSpentAmount: BigDecimal,
    val nonCashSpentAmount: BigDecimal,
    val totalSpentAmount: BigDecimal,
    val pocketBalanceAmount: BigDecimal,
    val cashInHandAmount: BigDecimal,
    val netBalanceCents: Long,
    val netBalanceAmount: BigDecimal
)

data class TripReportSubunitBalanceItem(
    val subunit: Subunit,
    val contributedCents: Long,
    val contributedAmount: BigDecimal,
    val withdrawnCents: Long,
    val withdrawnAmount: BigDecimal,
    val totalSpentCents: Long,
    val totalSpentAmount: BigDecimal,
    val pocketBalanceCents: Long,
    val pocketBalanceAmount: BigDecimal,
    val cashInHandCents: Long,
    val cashInHandAmount: BigDecimal,
    val netBalanceCents: Long,
    val netBalanceAmount: BigDecimal
)

data class TripReportSettlementItem(
    val record: SettlementRecord,
    val currency: String,
    val amountCents: Long,
    val amount: BigDecimal
)
