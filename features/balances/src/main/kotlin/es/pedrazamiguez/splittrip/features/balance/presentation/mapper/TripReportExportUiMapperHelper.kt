package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.report.GroupTripReport
import es.pedrazamiguez.splittrip.domain.model.report.TripReportContributionItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCsvSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExpenseItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMemberBalanceItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportSettlementItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportSubunitBalanceItem
import es.pedrazamiguez.splittrip.domain.model.report.TripReportWithdrawalItem
import es.pedrazamiguez.splittrip.features.balance.R

internal fun buildTripReportCsvSections(
    report: GroupTripReport,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): List<TripReportCsvSection> = buildList {
    add(buildSummaryCsvSection(report, resourceProvider))
    add(buildMemberBalancesCsvSection(report, resourceProvider, userUiMapper))
    if (report.subunitBalances.isNotEmpty()) {
        add(buildSubunitBalancesCsvSection(report, resourceProvider))
    }
    add(buildSettlementsCsvSection(report, resourceProvider, userUiMapper))
    add(buildExpensesCsvSection(report, resourceProvider, userUiMapper))
    add(buildContributionsCsvSection(report, resourceProvider, userUiMapper))
    add(buildWithdrawalsCsvSection(report, resourceProvider, userUiMapper))
}

private fun buildSummaryCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_summary_section_title),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_title),
        resourceProvider.getString(R.string.balances_export_col_amount),
        resourceProvider.getString(R.string.balances_export_col_currency)
    ),
    rows = listOf(
        listOf(
            resourceProvider.getString(R.string.balances_export_summary_total_contributed),
            report.totalContributedAmount.toPlainString(),
            report.baseCurrency
        ),
        listOf(
            resourceProvider.getString(R.string.balances_export_summary_total_expenses),
            report.totalExpensesAmount.toPlainString(),
            report.baseCurrency
        ),
        listOf(
            resourceProvider.getString(R.string.balances_export_summary_virtual_pocket),
            report.virtualPocketBalanceAmount.toPlainString(),
            report.baseCurrency
        ),
        listOf(
            resourceProvider.getString(R.string.balances_export_summary_cash_equivalent),
            report.totalCashEquivalentAmount.toPlainString(),
            report.baseCurrency
        ),
        listOf(
            resourceProvider.getString(R.string.balances_export_summary_total_extras),
            report.totalExtrasAmount.toPlainString(),
            report.baseCurrency
        )
    )
)

private fun buildMemberBalancesCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_section_member_balances),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_member),
        resourceProvider.getString(R.string.balances_export_col_contributed),
        resourceProvider.getString(R.string.balances_export_col_withdrawn),
        resourceProvider.getString(R.string.balances_export_col_cash_spent),
        resourceProvider.getString(R.string.balances_export_col_non_cash_spent),
        resourceProvider.getString(R.string.balances_export_col_spent),
        resourceProvider.getString(R.string.balances_export_col_pocket_balance),
        resourceProvider.getString(R.string.balances_export_col_cash_in_hand),
        resourceProvider.getString(R.string.balances_export_col_net_balance),
        resourceProvider.getString(R.string.balances_export_col_currency)
    ),
    rows = report.memberBalances.map { item: TripReportMemberBalanceItem ->
        listOf(
            resolveReportMemberName(report, item.memberBalance.userId, userUiMapper),
            item.contributedAmount.toPlainString(),
            item.withdrawnAmount.toPlainString(),
            item.cashSpentAmount.toPlainString(),
            item.nonCashSpentAmount.toPlainString(),
            item.totalSpentAmount.toPlainString(),
            item.pocketBalanceAmount.toPlainString(),
            item.cashInHandAmount.toPlainString(),
            item.netBalanceAmount.toPlainString(),
            report.baseCurrency
        )
    }
)

private fun buildSubunitBalancesCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_section_subunit_balances),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_subunit),
        resourceProvider.getString(R.string.balances_export_col_contributed),
        resourceProvider.getString(R.string.balances_export_col_withdrawn),
        resourceProvider.getString(R.string.balances_export_col_spent),
        resourceProvider.getString(R.string.balances_export_col_pocket_balance),
        resourceProvider.getString(R.string.balances_export_col_cash_in_hand),
        resourceProvider.getString(R.string.balances_export_col_net_balance),
        resourceProvider.getString(R.string.balances_export_col_currency)
    ),
    rows = report.subunitBalances.map { item: TripReportSubunitBalanceItem ->
        listOf(
            item.subunit.name,
            item.contributedAmount.toPlainString(),
            item.withdrawnAmount.toPlainString(),
            item.totalSpentAmount.toPlainString(),
            item.pocketBalanceAmount.toPlainString(),
            item.cashInHandAmount.toPlainString(),
            item.netBalanceAmount.toPlainString(),
            report.baseCurrency
        )
    }
)

private fun buildSettlementsCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_section_settlements),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_from),
        resourceProvider.getString(R.string.balances_export_col_to),
        resourceProvider.getString(R.string.balances_export_col_pocket),
        resourceProvider.getString(R.string.balances_export_col_amount),
        resourceProvider.getString(R.string.balances_export_col_currency),
        resourceProvider.getString(R.string.balances_export_col_status)
    ),
    rows = report.settlements.map { item: TripReportSettlementItem ->
        val settlement = item.record.settlement
        listOf(
            resolveReportMemberName(report, settlement.fromUserId, userUiMapper),
            resolveReportMemberName(report, settlement.toUserId, userUiMapper),
            resolveReportPocketTypeLabel(settlement.sourcePocket, resourceProvider),
            item.amount.toPlainString(),
            item.currency,
            resolveReportSettlementStatusLabel(item.record.status, resourceProvider)
        )
    }
)

private fun buildExpensesCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_section_expenses),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_date),
        resourceProvider.getString(R.string.balances_export_col_title),
        resourceProvider.getString(R.string.balances_export_col_category),
        resourceProvider.getString(R.string.balances_export_col_subcategory),
        resourceProvider.getString(R.string.balances_export_col_payer),
        resourceProvider.getString(R.string.balances_export_col_source_amount),
        resourceProvider.getString(R.string.balances_export_col_source_currency),
        resourceProvider.getString(R.string.balances_export_col_rate),
        resourceProvider.getString(R.string.balances_export_col_base_amount),
        resourceProvider.getString(R.string.balances_export_col_extras),
        resourceProvider.getString(R.string.balances_export_col_effective_amount),
        resourceProvider.getString(R.string.balances_export_col_base_currency),
        resourceProvider.getString(R.string.balances_export_col_status)
    ),
    rows = report.expenses.map { item: TripReportExpenseItem ->
        val expense = item.expense
        val date = (expense.effectiveDate ?: expense.dueDate)?.toLocalDate()?.toString().orEmpty()
        listOf(
            date,
            expense.title,
            resourceProvider.getString(expense.category.toStringRes()),
            resourceProvider.getString(expense.subcategory.toStringRes()),
            resolveReportExpensePayerLabel(
                report = report,
                payerType = expense.payerType,
                payerId = expense.payerId,
                createdBy = expense.createdBy,
                resourceProvider = resourceProvider,
                userUiMapper = userUiMapper
            ),
            item.sourceAmount.toPlainString(),
            expense.sourceCurrency.ifBlank { report.baseCurrency },
            item.exchangeRate.toPlainString(),
            item.baseGroupAmount.toPlainString(),
            item.addOnExtrasAmount.toPlainString(),
            item.effectiveGroupAmount.toPlainString(),
            expense.groupCurrency.ifBlank { report.baseCurrency },
            resolveReportPaymentStatusLabel(expense.paymentStatus, resourceProvider)
        )
    }
)

private fun buildContributionsCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_section_contributions),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_date),
        resourceProvider.getString(R.string.balances_export_col_member),
        resourceProvider.getString(R.string.balances_export_col_scope),
        resourceProvider.getString(R.string.balances_export_col_amount),
        resourceProvider.getString(R.string.balances_export_col_currency),
        resourceProvider.getString(R.string.balances_export_col_equivalent_base),
        resourceProvider.getString(R.string.balances_export_col_base_currency)
    ),
    rows = report.contributions.map { item: TripReportContributionItem ->
        val contrib = item.contribution
        val date = (contrib.contributionDate ?: contrib.createdAt)?.toLocalDate()?.toString().orEmpty()
        listOf(
            date,
            resolveReportContributionActorLabel(
                report = report,
                scope = contrib.contributionScope,
                subunitId = contrib.subunitId,
                userId = contrib.userId,
                userUiMapper = userUiMapper
            ),
            resolveReportScopeLabel(contrib.contributionScope, resourceProvider),
            item.amount.toPlainString(),
            contrib.currency.ifBlank { report.baseCurrency },
            item.equivalentBaseAmount.toPlainString(),
            report.baseCurrency
        )
    }
)

private fun buildWithdrawalsCsvSection(
    report: GroupTripReport,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): TripReportCsvSection = TripReportCsvSection(
    title = resourceProvider.getString(R.string.balances_export_section_withdrawals),
    headers = listOf(
        resourceProvider.getString(R.string.balances_export_col_date),
        resourceProvider.getString(R.string.balances_export_col_member),
        resourceProvider.getString(R.string.balances_export_col_scope),
        resourceProvider.getString(R.string.balances_export_col_amount),
        resourceProvider.getString(R.string.balances_export_col_remaining),
        resourceProvider.getString(R.string.balances_export_col_currency),
        resourceProvider.getString(R.string.balances_export_col_rate),
        resourceProvider.getString(R.string.balances_export_col_deducted_base),
        resourceProvider.getString(R.string.balances_export_col_fees),
        resourceProvider.getString(R.string.balances_export_col_effective_deducted),
        resourceProvider.getString(R.string.balances_export_col_base_currency)
    ),
    rows = report.withdrawals.map { item: TripReportWithdrawalItem ->
        val withdrawal = item.withdrawal
        val date = withdrawal.createdAt?.toLocalDate()?.toString().orEmpty()
        listOf(
            date,
            resolveReportMemberName(report, withdrawal.withdrawnBy, userUiMapper),
            resolveReportScopeLabel(withdrawal.withdrawalScope, resourceProvider),
            item.amountWithdrawn.toPlainString(),
            item.remainingAmount.toPlainString(),
            withdrawal.currency.ifBlank { report.baseCurrency },
            withdrawal.exchangeRate.toPlainString(),
            item.deductedBaseAmount.toPlainString(),
            item.addOnFeesAmount.toPlainString(),
            item.effectiveDeductedBaseAmount.toPlainString(),
            report.baseCurrency
        )
    }
)

internal fun resolveReportMemberName(
    report: GroupTripReport,
    userId: String,
    userUiMapper: UserUiMapper
): String = userUiMapper.mapToDisplayName(
    user = report.memberProfiles[userId],
    fallbackUserId = userId,
    currentUserId = report.currentUserId,
    selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
)

internal fun resolveReportExpensePayerLabel(
    report: GroupTripReport,
    payerType: PayerType,
    payerId: String?,
    createdBy: String,
    resourceProvider: ResourceProvider,
    userUiMapper: UserUiMapper
): String = when (payerType) {
    PayerType.GROUP -> resourceProvider.getString(R.string.balances_export_scope_group)
    PayerType.SUBUNIT -> report.subunits[payerId]?.name
        ?: resourceProvider.getString(R.string.balances_export_scope_subunit)
    PayerType.USER -> resolveReportMemberName(report, payerId ?: createdBy, userUiMapper)
}

internal fun resolveReportContributionActorLabel(
    report: GroupTripReport,
    scope: PayerType,
    subunitId: String?,
    userId: String,
    userUiMapper: UserUiMapper
): String = when (scope) {
    PayerType.SUBUNIT -> report.subunits[subunitId]?.name
        ?: resolveReportMemberName(report, userId, userUiMapper)
    PayerType.GROUP,
    PayerType.USER -> resolveReportMemberName(report, userId, userUiMapper)
}

internal fun resolveReportScopeLabel(
    scope: PayerType,
    resourceProvider: ResourceProvider
): String = when (scope) {
    PayerType.GROUP -> resourceProvider.getString(R.string.balances_export_scope_group)
    PayerType.SUBUNIT -> resourceProvider.getString(R.string.balances_export_scope_subunit)
    PayerType.USER -> resourceProvider.getString(R.string.balances_export_scope_user)
}

internal fun resolveReportPocketTypeLabel(
    pocketType: SettlementPocketType,
    resourceProvider: ResourceProvider
): String = when (pocketType) {
    SettlementPocketType.POCKET -> resourceProvider.getString(DesignSystemR.string.settlement_pocket_type_virtual)
    SettlementPocketType.CASH -> resourceProvider.getString(DesignSystemR.string.settlement_pocket_type_cash)
    SettlementPocketType.NET -> resourceProvider.getString(DesignSystemR.string.settlement_pocket_type_net)
}

internal fun resolveReportSettlementStatusLabel(
    status: SettlementStatus,
    resourceProvider: ResourceProvider
): String = when (status) {
    SettlementStatus.SUGGESTED -> resourceProvider.getString(DesignSystemR.string.settlement_status_pending)
    SettlementStatus.CONFIRMED_BY_PAYER ->
        resourceProvider.getString(DesignSystemR.string.settlement_status_awaiting_confirmation)
    SettlementStatus.DISPUTED -> resourceProvider.getString(DesignSystemR.string.settlement_status_disputed)
    SettlementStatus.RESOLVED -> resourceProvider.getString(DesignSystemR.string.settlement_status_confirmed)
}

internal fun resolveReportPaymentStatusLabel(
    status: PaymentStatus,
    resourceProvider: ResourceProvider
): String = when (status) {
    PaymentStatus.RECEIVED -> resourceProvider.getString(R.string.balances_export_payment_status_received)
    PaymentStatus.PENDING -> resourceProvider.getString(R.string.balances_export_payment_status_pending)
    PaymentStatus.FINISHED -> resourceProvider.getString(R.string.balances_export_payment_status_finished)
    PaymentStatus.SCHEDULED -> resourceProvider.getString(R.string.balances_export_payment_status_scheduled)
    PaymentStatus.CANCELLED -> resourceProvider.getString(R.string.balances_export_payment_status_cancelled)
    PaymentStatus.REFUNDABLE -> resourceProvider.getString(R.string.balances_export_payment_status_refundable)
}
