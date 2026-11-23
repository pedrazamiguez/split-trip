package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.report.GroupTripReport
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportKeyValueRow
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import es.pedrazamiguez.splittrip.features.balance.R
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

@Suppress("TooManyFunctions")
class TripReportExportUiMapper(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider,
    private val userUiMapper: UserUiMapper
) {

    fun mapToExportPayload(
        report: GroupTripReport,
        referenceYear: Int = LocalDate.now().year
    ): TripReportExportPayload {
        val periodLabel = resolvePeriodLabel(report, referenceYear)
        val generatedAtLabel = resourceProvider.getString(
            R.string.balances_export_generated_at,
            formattingHelper.formatShortDateTime(report.generatedAt, referenceYear)
        )
        return TripReportExportPayload(
            fileBaseName = buildFileBaseName(report.group.name, report.generatedAt.toLocalDate()),
            reportTitle = resourceProvider.getString(R.string.balances_export_report_title),
            groupName = report.group.name,
            groupDescription = report.group.description,
            periodLabel = periodLabel,
            generatedAtLabel = generatedAtLabel,
            baseCurrency = report.baseCurrency,
            summarySectionTitle = resourceProvider.getString(R.string.balances_export_summary_section_title),
            summaryMetrics = buildSummaryDisplayMetrics(report),
            memberBalancesSection = buildMemberBalancesTableSection(report),
            subunitBalancesSection = buildSubunitBalancesTableSection(report),
            settlementsSection = buildSettlementsTableSection(report),
            expensesSection = buildExpensesTableSection(report, referenceYear),
            contributionsSection = buildContributionsTableSection(report, referenceYear),
            withdrawalsSection = buildWithdrawalsTableSection(report, referenceYear),
            emptySectionLabel = resourceProvider.getString(R.string.balances_export_empty_section),
            pageFooterTemplate = resourceProvider.getString(R.string.balances_export_page_footer),
            csvSections = buildTripReportCsvSections(report, resourceProvider, userUiMapper)
        )
    }

    fun formatShortDate(
        date: LocalDate?,
        referenceYear: Int = LocalDate.now().year
    ): String = formattingHelper.formatShortDate(date, referenceYear)

    private fun resolvePeriodLabel(report: GroupTripReport, referenceYear: Int): String =
        when (report.period) {
            SpendingInsightsPeriod.ThisMonth -> resourceProvider.getString(
                R.string.balances_export_period_this_month,
                formattingHelper.formatMediumDate(report.windowStartDate)
            )
            SpendingInsightsPeriod.LastMonth -> resourceProvider.getString(
                R.string.balances_export_period_last_month,
                formattingHelper.formatMediumDate(report.windowStartDate)
            )
            is SpendingInsightsPeriod.CustomRange -> formatRangeLabel(
                start = report.windowStartDate,
                end = report.windowEndDate,
                referenceYear = referenceYear
            )
            SpendingInsightsPeriod.AllTime -> {
                val allTime = resourceProvider.getString(R.string.balances_export_period_all_time)
                val start = report.windowStartDate
                val end = report.windowEndDate
                if (start != null && end != null) {
                    "$allTime (${formatRangeLabel(start, end, referenceYear)})"
                } else {
                    allTime
                }
            }
        }

    private fun formatRangeLabel(
        start: LocalDate?,
        end: LocalDate?,
        referenceYear: Int
    ): String {
        if (start == null || end == null) {
            return resourceProvider.getString(R.string.balances_export_period_all_time)
        }
        if (start == end) {
            return formattingHelper.formatShortDate(start, referenceYear)
        }
        return resourceProvider.getString(
            R.string.balances_insights_period_range,
            formattingHelper.formatShortDate(start, referenceYear),
            formattingHelper.formatShortDate(end, referenceYear)
        )
    }

    private fun buildSummaryDisplayMetrics(report: GroupTripReport): List<TripReportKeyValueRow> = listOf(
        TripReportKeyValueRow(
            label = resourceProvider.getString(R.string.balances_export_summary_base_currency),
            value = report.baseCurrency
        ),
        TripReportKeyValueRow(
            label = resourceProvider.getString(R.string.balances_export_summary_total_contributed),
            value = formattingHelper.formatCentsWithCurrency(
                report.pocketBalance.totalContributions,
                report.baseCurrency
            )
        ),
        TripReportKeyValueRow(
            label = resourceProvider.getString(R.string.balances_export_summary_total_expenses),
            value = formattingHelper.formatCentsWithCurrency(
                report.pocketBalance.totalExpenses,
                report.baseCurrency
            )
        ),
        TripReportKeyValueRow(
            label = resourceProvider.getString(R.string.balances_export_summary_virtual_pocket),
            value = formattingHelper.formatCentsWithCurrency(
                report.pocketBalance.virtualBalance,
                report.baseCurrency
            )
        ),
        TripReportKeyValueRow(
            label = resourceProvider.getString(R.string.balances_export_summary_cash_equivalent),
            value = formattingHelper.formatCentsWithCurrency(
                report.pocketBalance.totalCashEquivalent,
                report.baseCurrency
            )
        ),
        TripReportKeyValueRow(
            label = resourceProvider.getString(R.string.balances_export_summary_total_extras),
            value = formattingHelper.formatCentsWithCurrency(
                report.pocketBalance.totalExtras,
                report.baseCurrency
            )
        )
    )

    private fun buildMemberBalancesTableSection(report: GroupTripReport): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_member_balances),
            headers = listOf(
                resourceProvider.getString(R.string.balances_export_col_member),
                resourceProvider.getString(R.string.balances_export_col_contributed),
                resourceProvider.getString(R.string.balances_export_col_spent),
                resourceProvider.getString(R.string.balances_export_col_pocket_balance),
                resourceProvider.getString(R.string.balances_export_col_cash_in_hand),
                resourceProvider.getString(R.string.balances_export_col_net_balance)
            ),
            rows = report.memberBalances.map { item ->
                val balance = item.memberBalance
                listOf(
                    resolveReportMemberName(report, balance.userId, userUiMapper),
                    formattingHelper.formatCentsWithCurrency(balance.contributed, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(balance.totalSpent, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(balance.pocketBalance, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(balance.cashInHand, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(item.netBalanceCents, report.baseCurrency)
                )
            }
        )

    private fun buildSubunitBalancesTableSection(report: GroupTripReport): TripReportTableSection? {
        if (report.subunitBalances.isEmpty()) return null
        return TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_subunit_balances),
            headers = listOf(
                resourceProvider.getString(R.string.balances_export_col_subunit),
                resourceProvider.getString(R.string.balances_export_col_contributed),
                resourceProvider.getString(R.string.balances_export_col_spent),
                resourceProvider.getString(R.string.balances_export_col_pocket_balance),
                resourceProvider.getString(R.string.balances_export_col_cash_in_hand),
                resourceProvider.getString(R.string.balances_export_col_net_balance)
            ),
            rows = report.subunitBalances.map { item ->
                listOf(
                    item.subunit.name,
                    formattingHelper.formatCentsWithCurrency(item.contributedCents, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(item.totalSpentCents, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(item.pocketBalanceCents, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(item.cashInHandCents, report.baseCurrency),
                    formattingHelper.formatCentsWithCurrency(item.netBalanceCents, report.baseCurrency)
                )
            }
        )
    }

    private fun buildSettlementsTableSection(report: GroupTripReport): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_settlements),
            headers = listOf(
                resourceProvider.getString(R.string.balances_export_col_from),
                resourceProvider.getString(R.string.balances_export_col_to),
                resourceProvider.getString(R.string.balances_export_col_pocket),
                resourceProvider.getString(R.string.balances_export_col_amount),
                resourceProvider.getString(R.string.balances_export_col_status)
            ),
            rows = report.settlements.map { item ->
                val settlement = item.record.settlement
                listOf(
                    resolveReportMemberName(report, settlement.fromUserId, userUiMapper),
                    resolveReportMemberName(report, settlement.toUserId, userUiMapper),
                    resolveReportPocketTypeLabel(settlement.sourcePocket, resourceProvider),
                    formattingHelper.formatCentsWithCurrency(item.amountCents, item.currency),
                    resolveReportSettlementStatusLabel(item.record.status, resourceProvider)
                )
            }
        )

    private fun buildExpensesTableSection(
        report: GroupTripReport,
        referenceYear: Int
    ): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_expenses),
            headers = listOf(
                resourceProvider.getString(R.string.balances_export_col_date),
                resourceProvider.getString(R.string.balances_export_col_title),
                resourceProvider.getString(R.string.balances_export_col_category),
                resourceProvider.getString(R.string.balances_export_col_payer),
                resourceProvider.getString(R.string.balances_export_col_source_amount),
                resourceProvider.getString(R.string.balances_export_col_effective_amount),
                resourceProvider.getString(R.string.balances_export_col_status)
            ),
            rows = report.expenses.map { item ->
                val expense = item.expense
                val date = (expense.effectiveDate ?: expense.dueDate)?.toLocalDate()
                val sourceCurrency = expense.sourceCurrency.ifBlank { report.baseCurrency }
                val baseCurrency = expense.groupCurrency.ifBlank { report.baseCurrency }
                listOf(
                    formattingHelper.formatShortDate(date, referenceYear),
                    expense.title,
                    resourceProvider.getString(expense.category.toStringRes()),
                    resolveReportExpensePayerLabel(
                        report = report,
                        payerType = expense.payerType,
                        payerId = expense.payerId,
                        createdBy = expense.createdBy,
                        resourceProvider = resourceProvider,
                        userUiMapper = userUiMapper
                    ),
                    formattingHelper.formatCentsWithCurrency(expense.sourceAmount, sourceCurrency),
                    formattingHelper.formatCentsWithCurrency(item.effectiveGroupAmountCents, baseCurrency),
                    resolveReportPaymentStatusLabel(expense.paymentStatus, resourceProvider)
                )
            }
        )

    private fun buildContributionsTableSection(
        report: GroupTripReport,
        referenceYear: Int
    ): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_contributions),
            headers = listOf(
                resourceProvider.getString(R.string.balances_export_col_date),
                resourceProvider.getString(R.string.balances_export_col_member),
                resourceProvider.getString(R.string.balances_export_col_scope),
                resourceProvider.getString(R.string.balances_export_col_amount),
                resourceProvider.getString(R.string.balances_export_col_equivalent_base)
            ),
            rows = report.contributions.map { item ->
                val contrib = item.contribution
                val date = (contrib.contributionDate ?: contrib.createdAt)?.toLocalDate()
                val contribCurrency = contrib.currency.ifBlank { report.baseCurrency }
                listOf(
                    formattingHelper.formatShortDate(date, referenceYear),
                    resolveReportContributionActorLabel(
                        report = report,
                        scope = contrib.contributionScope,
                        subunitId = contrib.subunitId,
                        userId = contrib.userId,
                        userUiMapper = userUiMapper
                    ),
                    resolveReportScopeLabel(contrib.contributionScope, resourceProvider),
                    formattingHelper.formatCentsWithCurrency(contrib.amount, contribCurrency),
                    formattingHelper.formatCentsWithCurrency(item.equivalentBaseAmountCents, report.baseCurrency)
                )
            }
        )

    private fun buildWithdrawalsTableSection(
        report: GroupTripReport,
        referenceYear: Int
    ): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_withdrawals),
            headers = listOf(
                resourceProvider.getString(R.string.balances_export_col_date),
                resourceProvider.getString(R.string.balances_export_col_member),
                resourceProvider.getString(R.string.balances_export_col_amount),
                resourceProvider.getString(R.string.balances_export_col_remaining),
                resourceProvider.getString(R.string.balances_export_col_effective_deducted)
            ),
            rows = report.withdrawals.map { item ->
                val withdrawal = item.withdrawal
                val date = withdrawal.createdAt?.toLocalDate()
                val withdrawalCurrency = withdrawal.currency.ifBlank { report.baseCurrency }
                listOf(
                    formattingHelper.formatShortDate(date, referenceYear),
                    resolveReportMemberName(report, withdrawal.withdrawnBy, userUiMapper),
                    formattingHelper.formatCentsWithCurrency(withdrawal.amountWithdrawn, withdrawalCurrency),
                    formattingHelper.formatCentsWithCurrency(withdrawal.remainingAmount, withdrawalCurrency),
                    formattingHelper.formatCentsWithCurrency(item.effectiveDeductedBaseCents, report.baseCurrency)
                )
            }
        )

    private fun buildFileBaseName(groupName: String, generatedDate: LocalDate): String {
        val normalized = Normalizer.normalize(groupName.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .replace(NON_ALNUM_REGEX, "-")
            .trim('-')
            .ifBlank { DEFAULT_GROUP_SLUG }
        return "$FILE_PREFIX-$normalized-$generatedDate"
    }

    private companion object {
        const val FILE_PREFIX = "splittrip-statement"
        const val DEFAULT_GROUP_SLUG = "group"
        val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        val NON_ALNUM_REGEX = Regex("[^a-z0-9]+")
    }
}
