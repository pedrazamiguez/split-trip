package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.report.GroupTripReport
import es.pedrazamiguez.splittrip.domain.model.report.TripReportBrandFooter
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCell
import es.pedrazamiguez.splittrip.domain.model.report.TripReportCellAlign
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportMetricCard
import es.pedrazamiguez.splittrip.domain.model.report.TripReportProUpsellCallout
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTableSection
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone
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
        referenceYear: Int = LocalDate.now().year,
        includeItemisedDetails: Boolean = true
    ): TripReportExportPayload {
        val periodLabel = resolvePeriodLabel(report, referenceYear)
        val generatedAtLabel = resourceProvider.getString(
            R.string.balances_export_generated_at,
            formattingHelper.formatShortDateTime(report.generatedAt, referenceYear)
        )
        val reportTitleRes = if (includeItemisedDetails) {
            R.string.balances_export_report_title_pro
        } else {
            R.string.balances_export_report_title_free
        }
        return TripReportExportPayload(
            fileBaseName = buildFileBaseName(report.group.name, report.generatedAt.toLocalDate()),
            reportTitle = resourceProvider.getString(reportTitleRes),
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
            expensesSection = if (includeItemisedDetails) {
                buildExpensesTableSection(report, referenceYear)
            } else {
                null
            },
            contributionsSection = if (includeItemisedDetails) {
                buildContributionsTableSection(report, referenceYear)
            } else {
                null
            },
            withdrawalsSection = if (includeItemisedDetails) {
                buildWithdrawalsTableSection(report, referenceYear)
            } else {
                null
            },
            proUpsellCallout = if (includeItemisedDetails) null else buildProUpsellCallout(),
            brandFooter = buildBrandFooter(),
            emptySectionLabel = resourceProvider.getString(R.string.balances_export_empty_section),
            pageFooterTemplate = resourceProvider.getString(R.string.balances_export_page_footer),
            csvSections = buildTripReportCsvSections(report, resourceProvider, userUiMapper)
        )
    }

    fun formatShortDate(
        date: LocalDate?,
        referenceYear: Int = LocalDate.now().year
    ): String = formattingHelper.formatShortDate(date, referenceYear)

    private fun buildProUpsellCallout(): TripReportProUpsellCallout = TripReportProUpsellCallout(
        badge = resourceProvider.getString(R.string.balances_export_pro_callout_badge),
        title = resourceProvider.getString(R.string.balances_export_pro_callout_title),
        body = resourceProvider.getString(R.string.balances_export_pro_callout_body)
    )

    private fun buildBrandFooter(): TripReportBrandFooter = TripReportBrandFooter(
        brandTagline = resourceProvider.getString(R.string.balances_export_brand_tagline),
        ctaHeadline = resourceProvider.getString(R.string.balances_export_brand_cta_headline),
        ctaSubtitle = resourceProvider.getString(R.string.balances_export_brand_cta_subtitle),
        websiteDisplay = SPLITTRIP_WEBSITE_DISPLAY,
        qrUrl = SPLITTRIP_WEBSITE_URL,
        qrCaption = resourceProvider.getString(R.string.balances_export_brand_qr_caption)
    )

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

    private fun buildSummaryDisplayMetrics(report: GroupTripReport): List<TripReportMetricCard> {
        val virtualBalance = report.pocketBalance.virtualBalance
        val pocketTone = if (virtualBalance < 0L) TripReportTone.NEGATIVE else TripReportTone.PRIMARY
        return listOf(
            TripReportMetricCard(
                label = resourceProvider.getString(R.string.balances_export_summary_virtual_pocket),
                value = formattingHelper.formatCentsWithCurrency(virtualBalance, report.baseCurrency),
                tone = pocketTone
            ),
            TripReportMetricCard(
                label = resourceProvider.getString(R.string.balances_export_summary_total_contributed),
                value = formattingHelper.formatCentsWithCurrency(
                    report.pocketBalance.totalContributions,
                    report.baseCurrency
                ),
                tone = TripReportTone.POSITIVE
            ),
            TripReportMetricCard(
                label = resourceProvider.getString(R.string.balances_export_summary_total_expenses),
                value = formattingHelper.formatCentsWithCurrency(
                    report.pocketBalance.totalExpenses,
                    report.baseCurrency
                ),
                tone = TripReportTone.NEGATIVE
            ),
            TripReportMetricCard(
                label = resourceProvider.getString(R.string.balances_export_summary_cash_equivalent),
                value = formattingHelper.formatCentsWithCurrency(
                    report.pocketBalance.totalCashEquivalent,
                    report.baseCurrency
                ),
                tone = TripReportTone.TEAL
            ),
            TripReportMetricCard(
                label = resourceProvider.getString(R.string.balances_export_summary_total_extras),
                value = formattingHelper.formatCentsWithCurrency(
                    report.pocketBalance.totalExtras,
                    report.baseCurrency
                ),
                tone = TripReportTone.AMBER
            )
        )
    }

    private fun buildMemberBalancesTableSection(report: GroupTripReport): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_member_balances),
            accentTone = TripReportTone.PRIMARY,
            columnWeights = BALANCE_COLUMN_WEIGHTS,
            columnAlignments = BALANCE_COLUMN_ALIGNMENTS,
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
                    TripReportCell(resolveReportMemberName(report, balance.userId, userUiMapper)),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(balance.contributed, report.baseCurrency),
                        tone = TripReportTone.POSITIVE,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(balance.totalSpent, report.baseCurrency),
                        tone = TripReportTone.NEGATIVE,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(balance.pocketBalance, report.baseCurrency),
                        tone = balanceTone(balance.pocketBalance),
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(balance.cashInHand, report.baseCurrency),
                        tone = TripReportTone.TEAL,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.netBalanceCents, report.baseCurrency),
                        tone = balanceTone(item.netBalanceCents),
                        align = TripReportCellAlign.END
                    )
                )
            }
        )

    private fun buildSubunitBalancesTableSection(report: GroupTripReport): TripReportTableSection? {
        if (report.subunitBalances.isEmpty()) return null
        return TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_subunit_balances),
            accentTone = TripReportTone.PRIMARY,
            columnWeights = BALANCE_COLUMN_WEIGHTS,
            columnAlignments = BALANCE_COLUMN_ALIGNMENTS,
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
                    TripReportCell(item.subunit.name),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.contributedCents, report.baseCurrency),
                        tone = TripReportTone.POSITIVE,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.totalSpentCents, report.baseCurrency),
                        tone = TripReportTone.NEGATIVE,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.pocketBalanceCents, report.baseCurrency),
                        tone = balanceTone(item.pocketBalanceCents),
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.cashInHandCents, report.baseCurrency),
                        tone = TripReportTone.TEAL,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.netBalanceCents, report.baseCurrency),
                        tone = balanceTone(item.netBalanceCents),
                        align = TripReportCellAlign.END
                    )
                )
            }
        )
    }

    private fun buildSettlementsTableSection(report: GroupTripReport): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_settlements),
            accentTone = TripReportTone.TEAL,
            columnWeights = SETTLEMENT_COLUMN_WEIGHTS,
            columnAlignments = SETTLEMENT_COLUMN_ALIGNMENTS,
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
                    TripReportCell(resolveReportMemberName(report, settlement.fromUserId, userUiMapper)),
                    TripReportCell(resolveReportMemberName(report, settlement.toUserId, userUiMapper)),
                    TripReportCell(
                        text = resolveReportPocketTypeLabel(settlement.sourcePocket, resourceProvider),
                        tone = TripReportTone.MUTED
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.amountCents, item.currency),
                        tone = TripReportTone.PRIMARY,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = resolveReportSettlementStatusLabel(item.record.status, resourceProvider),
                        tone = settlementStatusTone(item.record.status),
                        align = TripReportCellAlign.END
                    )
                )
            }
        )

    private fun buildExpensesTableSection(
        report: GroupTripReport,
        referenceYear: Int
    ): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_expenses),
            accentTone = TripReportTone.NEGATIVE,
            columnWeights = EXPENSE_COLUMN_WEIGHTS,
            columnAlignments = EXPENSE_COLUMN_ALIGNMENTS,
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
                    TripReportCell(
                        text = formattingHelper.formatShortDate(date, referenceYear),
                        tone = TripReportTone.MUTED
                    ),
                    TripReportCell(expense.title),
                    TripReportCell(
                        text = resourceProvider.getString(expense.category.toStringRes()),
                        tone = TripReportTone.MUTED
                    ),
                    TripReportCell(
                        resolveReportExpensePayerLabel(
                            report = report,
                            payerType = expense.payerType,
                            payerId = expense.payerId,
                            createdBy = expense.createdBy,
                            resourceProvider = resourceProvider,
                            userUiMapper = userUiMapper
                        )
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(expense.sourceAmount, sourceCurrency),
                        tone = TripReportTone.MUTED,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(item.effectiveGroupAmountCents, baseCurrency),
                        tone = TripReportTone.NEGATIVE,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = resolveReportPaymentStatusLabel(expense.paymentStatus, resourceProvider),
                        tone = paymentStatusTone(expense.paymentStatus),
                        align = TripReportCellAlign.END
                    )
                )
            }
        )

    private fun buildContributionsTableSection(
        report: GroupTripReport,
        referenceYear: Int
    ): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_contributions),
            accentTone = TripReportTone.POSITIVE,
            columnWeights = CONTRIBUTION_COLUMN_WEIGHTS,
            columnAlignments = CONTRIBUTION_COLUMN_ALIGNMENTS,
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
                    TripReportCell(
                        text = formattingHelper.formatShortDate(date, referenceYear),
                        tone = TripReportTone.MUTED
                    ),
                    TripReportCell(
                        resolveReportContributionActorLabel(
                            report = report,
                            scope = contrib.contributionScope,
                            subunitId = contrib.subunitId,
                            userId = contrib.userId,
                            userUiMapper = userUiMapper
                        )
                    ),
                    TripReportCell(
                        text = resolveReportScopeLabel(contrib.contributionScope, resourceProvider),
                        tone = TripReportTone.MUTED
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(contrib.amount, contribCurrency),
                        tone = TripReportTone.POSITIVE,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(
                            item.equivalentBaseAmountCents,
                            report.baseCurrency
                        ),
                        tone = TripReportTone.POSITIVE,
                        align = TripReportCellAlign.END
                    )
                )
            }
        )

    private fun buildWithdrawalsTableSection(
        report: GroupTripReport,
        referenceYear: Int
    ): TripReportTableSection =
        TripReportTableSection(
            title = resourceProvider.getString(R.string.balances_export_section_withdrawals),
            accentTone = TripReportTone.AMBER,
            columnWeights = WITHDRAWAL_COLUMN_WEIGHTS,
            columnAlignments = WITHDRAWAL_COLUMN_ALIGNMENTS,
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
                    TripReportCell(
                        text = formattingHelper.formatShortDate(date, referenceYear),
                        tone = TripReportTone.MUTED
                    ),
                    TripReportCell(resolveReportMemberName(report, withdrawal.withdrawnBy, userUiMapper)),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(withdrawal.amountWithdrawn, withdrawalCurrency),
                        tone = TripReportTone.AMBER,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(withdrawal.remainingAmount, withdrawalCurrency),
                        tone = TripReportTone.TEAL,
                        align = TripReportCellAlign.END
                    ),
                    TripReportCell(
                        text = formattingHelper.formatCentsWithCurrency(
                            item.effectiveDeductedBaseCents,
                            report.baseCurrency
                        ),
                        tone = TripReportTone.DEFAULT,
                        align = TripReportCellAlign.END
                    )
                )
            }
        )

    private fun balanceTone(cents: Long): TripReportTone = when {
        cents > 0L -> TripReportTone.POSITIVE
        cents < 0L -> TripReportTone.NEGATIVE
        else -> TripReportTone.MUTED
    }

    private fun settlementStatusTone(status: SettlementStatus): TripReportTone = when (status) {
        SettlementStatus.RESOLVED -> TripReportTone.BADGE_SUCCESS
        SettlementStatus.CONFIRMED_BY_PAYER,
        SettlementStatus.SUGGESTED -> TripReportTone.BADGE_WARNING
        SettlementStatus.DISPUTED -> TripReportTone.BADGE_ERROR
    }

    private fun paymentStatusTone(status: PaymentStatus): TripReportTone = when (status) {
        PaymentStatus.FINISHED,
        PaymentStatus.RECEIVED -> TripReportTone.BADGE_SUCCESS
        PaymentStatus.PENDING,
        PaymentStatus.SCHEDULED -> TripReportTone.BADGE_WARNING
        PaymentStatus.REFUNDABLE -> TripReportTone.BADGE_INFO
        PaymentStatus.CANCELLED -> TripReportTone.BADGE_ERROR
    }

    private fun buildFileBaseName(groupName: String, generatedDate: LocalDate): String {
        val normalized = Normalizer.normalize(groupName.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .replace(NON_ALNUM_REGEX, "-")
            .trim('-')
            .ifBlank { DEFAULT_GROUP_SLUG }
        return "$FILE_PREFIX-$normalized-$generatedDate"
    }

    private companion object {
        const val FILE_PREFIX = "splittrip-summary"
        const val DEFAULT_GROUP_SLUG = "group"
        const val SPLITTRIP_WEBSITE_URL = "https://splittrip.eu"
        const val SPLITTRIP_WEBSITE_DISPLAY = "splittrip.eu"
        val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        val NON_ALNUM_REGEX = Regex("[^a-z0-9]+")

        val BALANCE_COLUMN_WEIGHTS = listOf(0.22f, 0.16f, 0.16f, 0.16f, 0.14f, 0.16f)
        val BALANCE_COLUMN_ALIGNMENTS = listOf(
            TripReportCellAlign.START,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END
        )

        val SETTLEMENT_COLUMN_WEIGHTS = listOf(0.20f, 0.20f, 0.16f, 0.18f, 0.26f)
        val SETTLEMENT_COLUMN_ALIGNMENTS = listOf(
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.END,
            TripReportCellAlign.END
        )

        val EXPENSE_COLUMN_WEIGHTS = listOf(0.10f, 0.22f, 0.13f, 0.15f, 0.13f, 0.14f, 0.13f)
        val EXPENSE_COLUMN_ALIGNMENTS = listOf(
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END
        )

        val CONTRIBUTION_COLUMN_WEIGHTS = listOf(0.14f, 0.28f, 0.16f, 0.21f, 0.21f)
        val CONTRIBUTION_COLUMN_ALIGNMENTS = listOf(
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.END,
            TripReportCellAlign.END
        )

        val WITHDRAWAL_COLUMN_WEIGHTS = listOf(0.14f, 0.24f, 0.20f, 0.20f, 0.22f)
        val WITHDRAWAL_COLUMN_ALIGNMENTS = listOf(
            TripReportCellAlign.START,
            TripReportCellAlign.START,
            TripReportCellAlign.END,
            TripReportCellAlign.END,
            TripReportCellAlign.END
        )
    }
}
