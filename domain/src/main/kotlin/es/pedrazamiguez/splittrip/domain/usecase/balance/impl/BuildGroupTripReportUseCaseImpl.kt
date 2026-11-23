package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.GroupDashboardReadModel
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.model.report.GroupTripReport
import es.pedrazamiguez.splittrip.domain.model.report.TripReportContributionItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExpenseItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMemberBalanceItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportSettlementItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportSubunitBalanceItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportWithdrawalItem
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.usecase.balance.BuildGroupTripReportUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.MemberBalanceCalculationInputs
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Currency
import kotlinx.coroutines.flow.first

@Suppress("LongParameterList", "TooManyFunctions")
class BuildGroupTripReportUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val groupDashboardDataSource: GroupDashboardDataSource,
    private val getGroupPocketBalanceFlowUseCase: GetGroupPocketBalanceFlowUseCase,
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase,
    private val getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val addOnCalculationService: AddOnCalculationService,
    private val expenseCalculatorService: ExpenseCalculatorService,
    private val authenticationService: AuthenticationService,
    private val appConfigService: AppConfigService
) : BuildGroupTripReportUseCase {

    override suspend operator fun invoke(
        groupId: String,
        period: SpendingInsightsPeriod,
        today: LocalDate,
        generatedAt: LocalDateTime
    ): GroupTripReport {
        groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found: $groupId")

        val snapshot = groupDashboardDataSource.getDashboardSnapshotFlow(groupId).first()
        return buildReport(
            groupId = groupId,
            snapshot = snapshot,
            period = period,
            today = today,
            generatedAt = generatedAt
        )
    }

    private data class ReportWindowBounds(
        val startDate: LocalDate?,
        val endDate: LocalDate?
    )

    private data class FilteredPeriodData(
        val windowStartDate: LocalDate?,
        val windowEndDate: LocalDate?,
        val expenses: List<Expense>,
        val contributions: List<Contribution>,
        val withdrawals: List<CashWithdrawal>,
        val cashTransfers: List<CashTransfer>,
        val settlements: List<SettlementRecord>
    )

    private suspend fun buildReport(
        groupId: String,
        snapshot: GroupDashboardReadModel,
        period: SpendingInsightsPeriod,
        today: LocalDate,
        generatedAt: LocalDateTime
    ): GroupTripReport {
        val baseCurrency = snapshot.group.currency.ifBlank { appConfigService.defaultCurrencyCode.value }
        val filtered = filterPeriodData(snapshot, period, today)
        val pocketBalance = getGroupPocketBalanceFlowUseCase.calculate(
            filtered.contributions,
            filtered.expenses,
            filtered.withdrawals,
            baseCurrency
        )
        val memberBalances = getMemberBalancesFlowUseCase.computeMemberBalances(
            MemberBalanceCalculationInputs(
                contributions = filtered.contributions,
                withdrawals = filtered.withdrawals,
                expenses = filtered.expenses,
                cashTransfers = filtered.cashTransfers,
                subunits = snapshot.subunits,
                groupMemberIds = snapshot.group.members,
                groupCurrency = baseCurrency,
                settlements = filtered.settlements
            )
        )
        val settlementRecords = resolveSettlementRecords(groupId, memberBalances, baseCurrency, filtered.settlements)
        val allUserIds = collectAllUserIds(
            snapshot,
            filtered.expenses,
            filtered.contributions,
            filtered.withdrawals,
            memberBalances,
            settlementRecords
        )
        val memberProfiles = if (allUserIds.isEmpty()) emptyMap() else getMemberProfilesUseCase(allUserIds)

        return assembleReport(
            snapshot = snapshot,
            period = period,
            windowStartDate = filtered.windowStartDate,
            windowEndDate = filtered.windowEndDate,
            generatedAt = generatedAt,
            baseCurrency = baseCurrency,
            pocketBalance = pocketBalance,
            expenses = filtered.expenses,
            contributions = filtered.contributions,
            withdrawals = filtered.withdrawals,
            memberBalances = memberBalances,
            settlements = settlementRecords,
            memberProfiles = memberProfiles
        )
    }

    private fun filterPeriodData(
        snapshot: GroupDashboardReadModel,
        period: SpendingInsightsPeriod,
        today: LocalDate
    ): FilteredPeriodData {
        val activeExpenses = snapshot.expenses.filter { it.paymentStatus != PaymentStatus.CANCELLED }
        val bounds = resolveWindowBounds(
            period = period,
            today = today,
            expenses = activeExpenses,
            contributions = snapshot.contributions,
            withdrawals = snapshot.withdrawals
        )
        return FilteredPeriodData(
            windowStartDate = bounds.startDate,
            windowEndDate = bounds.endDate,
            expenses = filterAndSortExpenses(activeExpenses, period, bounds.startDate, bounds.endDate),
            contributions = filterAndSortContributions(
                snapshot.contributions,
                period,
                bounds.startDate,
                bounds.endDate
            ),
            withdrawals = filterAndSortWithdrawals(snapshot.withdrawals, period, bounds.startDate, bounds.endDate),
            cashTransfers = filterCashTransfers(snapshot.cashTransfers, period, bounds.startDate, bounds.endDate),
            settlements = filterSettlements(snapshot.settlements, period, bounds.startDate, bounds.endDate)
        )
    }

    @Suppress("LongParameterList")
    private fun assembleReport(
        snapshot: GroupDashboardReadModel,
        period: SpendingInsightsPeriod,
        windowStartDate: LocalDate?,
        windowEndDate: LocalDate?,
        generatedAt: LocalDateTime,
        baseCurrency: String,
        pocketBalance: GroupPocketBalance,
        expenses: List<Expense>,
        contributions: List<Contribution>,
        withdrawals: List<CashWithdrawal>,
        memberBalances: List<MemberBalance>,
        settlements: List<SettlementRecord>,
        memberProfiles: Map<String, User>
    ): GroupTripReport = GroupTripReport(
        group = snapshot.group,
        period = period,
        windowStartDate = windowStartDate,
        windowEndDate = windowEndDate,
        generatedAt = generatedAt,
        baseCurrency = baseCurrency,
        pocketBalance = pocketBalance,
        totalContributedAmount = centsToBigDecimal(pocketBalance.totalContributions, baseCurrency),
        totalExpensesAmount = centsToBigDecimal(pocketBalance.totalExpenses, baseCurrency),
        virtualPocketBalanceAmount = centsToBigDecimal(pocketBalance.virtualBalance, baseCurrency),
        totalCashEquivalentAmount = centsToBigDecimal(pocketBalance.totalCashEquivalent, baseCurrency),
        totalExtrasAmount = centsToBigDecimal(pocketBalance.totalExtras, baseCurrency),
        expenses = expenses.map { mapExpenseItem(it, baseCurrency) },
        contributions = contributions.map { mapContributionItem(it, baseCurrency) },
        withdrawals = withdrawals.map { mapWithdrawalItem(it, baseCurrency) },
        memberBalances = memberBalances.map { mapMemberBalanceItem(it, baseCurrency) },
        subunitBalances = mapSubunitBalanceItems(snapshot.subunits, memberBalances, baseCurrency),
        settlements = settlements.map { mapSettlementItem(it, baseCurrency) },
        memberProfiles = memberProfiles,
        subunits = snapshot.subunits.associateBy { it.id },
        currentUserId = authenticationService.currentUserId()
    )

    private fun resolveWindowBounds(
        period: SpendingInsightsPeriod,
        today: LocalDate,
        expenses: List<Expense>,
        contributions: List<Contribution>,
        withdrawals: List<CashWithdrawal>
    ): ReportWindowBounds = when (period) {
        SpendingInsightsPeriod.ThisMonth -> ReportWindowBounds(
            startDate = today.withDayOfMonth(1),
            endDate = today.withDayOfMonth(today.lengthOfMonth())
        )

        SpendingInsightsPeriod.LastMonth -> {
            val lastMonth = YearMonth.from(today).minusMonths(1)
            ReportWindowBounds(
                startDate = lastMonth.atDay(1),
                endDate = lastMonth.atEndOfMonth()
            )
        }

        is SpendingInsightsPeriod.CustomRange -> ReportWindowBounds(
            startDate = minOf(period.startDate, period.endDate),
            endDate = maxOf(period.startDate, period.endDate)
        )

        SpendingInsightsPeriod.AllTime -> {
            val allDates = buildList {
                expenses.mapNotNullTo(this) { (it.effectiveDate ?: it.dueDate)?.toLocalDate() }
                contributions.mapNotNullTo(this) { (it.contributionDate ?: it.createdAt)?.toLocalDate() }
                withdrawals.mapNotNullTo(this) { it.createdAt?.toLocalDate() }
            }
            ReportWindowBounds(
                startDate = allDates.minOrNull(),
                endDate = allDates.maxOrNull()
            )
        }
    }

    private fun filterAndSortExpenses(
        expenses: List<Expense>,
        period: SpendingInsightsPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<Expense> = expenses
        .filter { expense ->
            isWithinPeriod(
                date = (expense.effectiveDate ?: expense.dueDate)?.toLocalDate(),
                period = period,
                startDate = startDate,
                endDate = endDate
            )
        }
        .sortedWith(
            compareByDescending<Expense> { it.effectiveDate ?: it.dueDate ?: LocalDateTime.MIN }
                .thenBy { it.id }
        )

    private fun filterAndSortContributions(
        contributions: List<Contribution>,
        period: SpendingInsightsPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<Contribution> = contributions
        .filter { contribution ->
            isWithinPeriod(
                date = (contribution.contributionDate ?: contribution.createdAt)?.toLocalDate(),
                period = period,
                startDate = startDate,
                endDate = endDate
            )
        }
        .sortedWith(
            compareByDescending<Contribution> {
                it.contributionDate ?: it.createdAt ?: LocalDateTime.MIN
            }.thenBy { it.id }
        )

    private fun filterAndSortWithdrawals(
        withdrawals: List<CashWithdrawal>,
        period: SpendingInsightsPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<CashWithdrawal> = withdrawals
        .filter { withdrawal ->
            isWithinPeriod(
                date = withdrawal.createdAt?.toLocalDate(),
                period = period,
                startDate = startDate,
                endDate = endDate
            )
        }
        .sortedWith(
            compareByDescending<CashWithdrawal> { it.createdAt ?: LocalDateTime.MIN }
                .thenBy { it.id }
        )

    private fun filterCashTransfers(
        cashTransfers: List<CashTransfer>,
        period: SpendingInsightsPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<CashTransfer> = cashTransfers.filter { transfer ->
        val date = Instant.ofEpochMilli(transfer.createdAt).atZone(ZoneOffset.UTC).toLocalDate()
        isWithinPeriod(date = date, period = period, startDate = startDate, endDate = endDate)
    }

    private fun filterSettlements(
        settlements: List<SettlementRecord>,
        period: SpendingInsightsPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<SettlementRecord> = settlements
        .filter { record ->
            isWithinPeriod(
                date = record.createdAt.toLocalDate(),
                period = period,
                startDate = startDate,
                endDate = endDate
            )
        }
        .sortedWith(
            compareByDescending<SettlementRecord> { it.createdAt }
                .thenBy { it.id }
        )

    private fun isWithinPeriod(
        date: LocalDate?,
        period: SpendingInsightsPeriod,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Boolean {
        if (period is SpendingInsightsPeriod.AllTime) return true
        val start = startDate ?: return false
        val end = endDate ?: return false
        val target = date ?: return false
        return !target.isBefore(start) && !target.isAfter(end)
    }

    private fun resolveSettlementRecords(
        groupId: String,
        memberBalances: List<MemberBalance>,
        baseCurrency: String,
        periodSettlements: List<SettlementRecord>
    ): List<SettlementRecord> {
        val activeAndSuggested = getSettlementSuggestionsUseCase.getEphemeralSettlements(
            groupId = groupId,
            memberBalances = memberBalances,
            groupCurrency = baseCurrency,
            persistedRecords = periodSettlements
        )
        val resolved = periodSettlements.filter { it.status == SettlementStatus.RESOLVED }
        return (activeAndSuggested + resolved).distinctBy { it.id }
    }

    private fun collectAllUserIds(
        snapshot: GroupDashboardReadModel,
        expenses: List<Expense>,
        contributions: List<Contribution>,
        withdrawals: List<CashWithdrawal>,
        memberBalances: List<MemberBalance>,
        settlements: List<SettlementRecord>
    ): List<String> = buildSet {
        addAll(snapshot.group.members)
        snapshot.subunits.forEach { subunit -> addAll(subunit.memberIds) }
        expenses.forEach { expense ->
            expense.payerId?.let(::add)
            add(expense.createdBy)
            expense.splits.forEach { split -> add(split.userId) }
        }
        contributions.forEach { contribution ->
            add(contribution.userId)
            add(contribution.createdBy)
        }
        withdrawals.forEach { withdrawal ->
            add(withdrawal.withdrawnBy)
            add(withdrawal.createdBy)
        }
        memberBalances.forEach { balance -> add(balance.userId) }
        settlements.forEach { record ->
            add(record.settlement.fromUserId)
            add(record.settlement.toUserId)
            record.disputedBy?.let(::add)
        }
    }.filter { it.isNotBlank() }

    private fun mapExpenseItem(expense: Expense, baseCurrency: String): TripReportExpenseItem {
        val addOnExtrasCents = addOnCalculationService.calculateTotalAddOnExtras(expense.addOns)
        val effectiveGroupAmountCents = addOnCalculationService.calculateEffectiveGroupAmount(
            baseGroupAmount = expense.groupAmount,
            addOns = expense.addOns
        )
        val effectiveBaseCurrency = expense.groupCurrency.ifBlank { baseCurrency }
        val effectiveSourceCurrency = expense.sourceCurrency.ifBlank { effectiveBaseCurrency }
        return TripReportExpenseItem(
            expense = expense,
            sourceAmount = centsToBigDecimal(expense.sourceAmount, effectiveSourceCurrency),
            baseGroupAmount = centsToBigDecimal(expense.groupAmount, effectiveBaseCurrency),
            addOnExtrasCents = addOnExtrasCents,
            addOnExtrasAmount = centsToBigDecimal(addOnExtrasCents, effectiveBaseCurrency),
            effectiveGroupAmountCents = effectiveGroupAmountCents,
            effectiveGroupAmount = centsToBigDecimal(effectiveGroupAmountCents, effectiveBaseCurrency),
            exchangeRate = expense.exchangeRate
        )
    }

    private fun mapContributionItem(
        contribution: Contribution,
        baseCurrency: String
    ): TripReportContributionItem {
        val contribCurrency = contribution.currency.ifBlank { baseCurrency }
        val equivalentBaseCents = contribution.equivalentBaseAmount ?: contribution.amount
        return TripReportContributionItem(
            contribution = contribution,
            amount = centsToBigDecimal(contribution.amount, contribCurrency),
            equivalentBaseAmountCents = equivalentBaseCents,
            equivalentBaseAmount = centsToBigDecimal(equivalentBaseCents, baseCurrency)
        )
    }

    private fun mapWithdrawalItem(
        withdrawal: CashWithdrawal,
        baseCurrency: String
    ): TripReportWithdrawalItem {
        val withdrawalCurrency = withdrawal.currency.ifBlank { baseCurrency }
        val effectiveDeductedCents = addOnCalculationService.calculateEffectiveDeductedAmount(
            baseDeductedAmount = withdrawal.deductedBaseAmount,
            addOns = withdrawal.addOns
        )
        val addOnFeesCents = effectiveDeductedCents - withdrawal.deductedBaseAmount
        return TripReportWithdrawalItem(
            withdrawal = withdrawal,
            amountWithdrawn = centsToBigDecimal(withdrawal.amountWithdrawn, withdrawalCurrency),
            remainingAmount = centsToBigDecimal(withdrawal.remainingAmount, withdrawalCurrency),
            deductedBaseAmount = centsToBigDecimal(withdrawal.deductedBaseAmount, baseCurrency),
            effectiveDeductedBaseCents = effectiveDeductedCents,
            effectiveDeductedBaseAmount = centsToBigDecimal(effectiveDeductedCents, baseCurrency),
            addOnFeesCents = addOnFeesCents,
            addOnFeesAmount = centsToBigDecimal(addOnFeesCents, baseCurrency)
        )
    }

    private fun mapMemberBalanceItem(
        memberBalance: MemberBalance,
        baseCurrency: String
    ): TripReportMemberBalanceItem {
        val netBalanceCents = memberBalance.totalBalance
        return TripReportMemberBalanceItem(
            memberBalance = memberBalance,
            contributedAmount = centsToBigDecimal(memberBalance.contributed, baseCurrency),
            withdrawnAmount = centsToBigDecimal(memberBalance.withdrawn, baseCurrency),
            cashSpentAmount = centsToBigDecimal(memberBalance.cashSpent, baseCurrency),
            nonCashSpentAmount = centsToBigDecimal(memberBalance.nonCashSpent, baseCurrency),
            totalSpentAmount = centsToBigDecimal(memberBalance.totalSpent, baseCurrency),
            pocketBalanceAmount = centsToBigDecimal(memberBalance.pocketBalance, baseCurrency),
            cashInHandAmount = centsToBigDecimal(memberBalance.cashInHand, baseCurrency),
            netBalanceCents = netBalanceCents,
            netBalanceAmount = centsToBigDecimal(netBalanceCents, baseCurrency)
        )
    }

    private fun mapSubunitBalanceItems(
        subunits: List<Subunit>,
        memberBalances: List<MemberBalance>,
        baseCurrency: String
    ): List<TripReportSubunitBalanceItem> {
        val balanceByUser = memberBalances.associateBy { it.userId }
        return subunits.map { subunit ->
            val members = subunit.memberIds.mapNotNull { balanceByUser[it] }
            val contributedCents = members.sumOf { it.contributed }
            val withdrawnCents = members.sumOf { it.withdrawn }
            val totalSpentCents = members.sumOf { it.totalSpent }
            val pocketBalanceCents = members.sumOf { it.pocketBalance }
            val cashInHandCents = members.sumOf { it.cashInHand }
            val netBalanceCents = pocketBalanceCents + cashInHandCents
            TripReportSubunitBalanceItem(
                subunit = subunit,
                contributedCents = contributedCents,
                contributedAmount = centsToBigDecimal(contributedCents, baseCurrency),
                withdrawnCents = withdrawnCents,
                withdrawnAmount = centsToBigDecimal(withdrawnCents, baseCurrency),
                totalSpentCents = totalSpentCents,
                totalSpentAmount = centsToBigDecimal(totalSpentCents, baseCurrency),
                pocketBalanceCents = pocketBalanceCents,
                pocketBalanceAmount = centsToBigDecimal(pocketBalanceCents, baseCurrency),
                cashInHandCents = cashInHandCents,
                cashInHandAmount = centsToBigDecimal(cashInHandCents, baseCurrency),
                netBalanceCents = netBalanceCents,
                netBalanceAmount = centsToBigDecimal(netBalanceCents, baseCurrency)
            )
        }
    }

    private fun mapSettlementItem(
        record: SettlementRecord,
        baseCurrency: String
    ): TripReportSettlementItem {
        val currency = record.settlement.currency.ifBlank { baseCurrency }
        val amountCents = record.settlement.amount
        return TripReportSettlementItem(
            record = record,
            currency = currency,
            amountCents = amountCents,
            amount = centsToBigDecimal(amountCents, currency)
        )
    }

    private fun centsToBigDecimal(cents: Long, currencyCode: String): BigDecimal =
        expenseCalculatorService.centsToBigDecimal(cents, resolveFractionDigits(currencyCode))

    private fun resolveFractionDigits(currencyCode: String): Int =
        runCatching {
            Currency.getInstance(currencyCode).defaultFractionDigits.takeIf { it >= 0 }
        }.getOrNull() ?: DomainConstants.DEFAULT_DECIMAL_PLACES
}
