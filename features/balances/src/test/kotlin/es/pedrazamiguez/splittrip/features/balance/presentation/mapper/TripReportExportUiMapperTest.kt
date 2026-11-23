package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.R as CommonR
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
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
import es.pedrazamiguez.splittrip.domain.model.report.TripReportTone
import es.pedrazamiguez.splittrip.domain.model.report.TripReportWithdrawalItem
import es.pedrazamiguez.splittrip.features.balance.R
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TripReportExportUiMapperTest {

    private class FakeLocaleProvider(var locale: Locale = Locale.US) : LocaleProvider {
        override fun getCurrentLocale(): Locale = locale
    }

    private lateinit var localeProvider: FakeLocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var mapper: TripReportExportUiMapper

    private val stringMap by lazy {
        mapOf(
            R.string.balances_export_report_title_pro to "Group summary & itemised ledger",
            R.string.balances_export_report_title_free to "Group summary",
            R.string.balances_export_period_all_time to "All history",
            R.string.balances_export_empty_section to "No activity recorded in this period",
            R.string.balances_export_page_footer to "Page %1\$d of %2\$d",
            R.string.balances_export_summary_section_title to "Group overview",
            R.string.balances_export_summary_total_contributed to "Total contributed",
            R.string.balances_export_summary_total_expenses to "Total expenses",
            R.string.balances_export_summary_virtual_pocket to "Virtual pocket",
            R.string.balances_export_summary_cash_equivalent to "Cash in hand",
            R.string.balances_export_summary_total_extras to "Extras & fees",
            R.string.balances_export_section_expenses to "Itemised expenses",
            R.string.balances_export_pro_callout_badge to "PRO",
            R.string.balances_export_pro_callout_title to "Itemised breakdown reserved for SplitTrip Pro",
            R.string.balances_export_pro_callout_body to "Upgrade to SplitTrip Pro.",
            R.string.balances_export_brand_tagline to "Split expenses without borders",
            R.string.balances_export_brand_cta_headline to "Share expenses fairly. Zero stress.",
            R.string.balances_export_brand_cta_subtitle to "Offline-first multi-currency group pockets.",
            R.string.balances_export_brand_qr_caption to "Scan to get SplitTrip",
            R.string.balances_export_payment_status_finished to "Finished",
            CommonR.string.expense_category_food to "Food",
            CommonR.string.expense_subcategory_restaurant to "Restaurant",
            DesignSystemR.string.self_identification_nominative to "You"
        )
    }

    @BeforeEach
    fun setUp() {
        localeProvider = FakeLocaleProvider(Locale.US)
        resourceProvider = mockk()
        every { resourceProvider.getString(any()) } answers {
            stringMap[firstArg<Int>()] ?: "label"
        }
        every { resourceProvider.getString(any(), *anyVararg()) } answers {
            val resId = firstArg<Int>()
            val args = secondArg<Array<Any>>()
            when (resId) {
                R.string.balances_export_generated_at -> "Generated ${args[0]}"
                R.string.balances_insights_period_range -> "${args[0]} - ${args[1]}"
                else -> "label"
            }
        }

        val formattingHelper = FormattingHelper(localeProvider = localeProvider)
        val userUiMapper = UserUiMapper(resourceProvider = resourceProvider, localeProvider = localeProvider)
        mapper = TripReportExportUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider,
            userUiMapper = userUiMapper
        )
    }

    @Test
    fun `mapToExportPayload formats PDF display amounts and CSV BigDecimal plain strings`() {
        localeProvider.locale = Locale("es", "ES")
        val report = createReport(groupName = "Shared Flat")

        val payload = mapper.mapToExportPayload(report = report, referenceYear = 2026)

        val pdfRow = requireNotNull(payload.expensesSection).rows.single()
        assertEquals("Dinner", pdfRow[1].text)
        assertTrue(pdfRow[4].text.contains("125,50"))
        assertTrue(pdfRow[5].text.contains("130,00"))
        assertEquals(TripReportTone.NEGATIVE, pdfRow[5].tone)
        assertEquals("https://splittrip.eu", payload.brandFooter.qrUrl)
        assertNull(payload.proUpsellCallout)

        val expensesCsv = payload.csvSections.first { it.title == "Itemised expenses" }
        val csvRow = expensesCsv.rows.single()
        assertEquals("2026-11-12", csvRow[0])
        assertEquals("125.50", csvRow[5])
        assertEquals("EUR", csvRow[6])
        assertEquals("1.000000", csvRow[7])
        assertEquals("125.50", csvRow[8])
        assertEquals("4.50", csvRow[9])
        assertEquals("130.00", csvRow[10])
    }

    @Test
    fun `mapToExportPayload on Free tier omits itemised sections and includes Pro upsell callout`() {
        val report = createRichReport()

        val payload = mapper.mapToExportPayload(
            report = report,
            referenceYear = 2026,
            includeItemisedDetails = false
        )

        assertEquals("Group summary", payload.reportTitle)
        assertNull(payload.expensesSection)
        assertNull(payload.contributionsSection)
        assertNull(payload.withdrawalsSection)
        assertNotNull(payload.proUpsellCallout)
        assertEquals("PRO", payload.proUpsellCallout?.badge)
        assertEquals("https://splittrip.eu", payload.brandFooter.qrUrl)
    }

    @Test
    fun `mapToExportPayload builds sanitized fileBaseName from group name and date`() {
        val report = createReport(groupName = "Café & Flatmates #1!")

        val payload = mapper.mapToExportPayload(report = report, referenceYear = 2026)

        assertEquals("splittrip-summary-cafe-flatmates-1-2026-11-20", payload.fileBaseName)
    }

    @Test
    fun `mapToExportPayload maps balances subunits settlements contributions withdrawals and periods`() {
        val report = createRichReport()

        val payload = mapper.mapToExportPayload(report = report, referenceYear = 2026)

        assertEquals(1, payload.memberBalancesSection.rows.size)
        assertEquals(6, payload.memberBalancesSection.columnWeights.size)
        assertNotNull(payload.subunitBalancesSection)
        assertEquals(4, payload.settlementsSection.rows.size)
        assertEquals(6, requireNotNull(payload.expensesSection).rows.size)
        assertEquals(3, requireNotNull(payload.contributionsSection).rows.size)
        assertEquals(1, requireNotNull(payload.withdrawalsSection).rows.size)
        assertEquals(7, payload.csvSections.size)

        val thisMonthPayload = mapper.mapToExportPayload(
            report.copy(period = SpendingInsightsPeriod.ThisMonth),
            referenceYear = 2026
        )
        val lastMonthPayload = mapper.mapToExportPayload(
            report.copy(period = SpendingInsightsPeriod.LastMonth),
            referenceYear = 2026
        )
        val singleDayPayload = mapper.mapToExportPayload(
            report.copy(
                period = SpendingInsightsPeriod.CustomRange(
                    LocalDate.of(2026, 11, 5),
                    LocalDate.of(2026, 11, 5)
                ),
                windowStartDate = LocalDate.of(2026, 11, 5),
                windowEndDate = LocalDate.of(2026, 11, 5)
            ),
            referenceYear = 2026
        )
        val noDatesAllTime = mapper.mapToExportPayload(
            report.copy(windowStartDate = null, windowEndDate = null),
            referenceYear = 2026
        )

        assertTrue(thisMonthPayload.periodLabel.isNotBlank())
        assertTrue(lastMonthPayload.periodLabel.isNotBlank())
        assertTrue(singleDayPayload.periodLabel.isNotBlank())
        assertEquals("All history", noDatesAllTime.periodLabel)
        assertTrue(mapper.formatShortDate(LocalDate.of(2026, 11, 5), 2026).isNotBlank())
    }

    private fun createRichReport(): GroupTripReport {
        val subunit = Subunit(id = "sub-1", groupId = "g1", name = "Couple", memberIds = listOf("u1"))
        val memberItem = TripReportMemberBalanceItem(
            memberBalance = MemberBalance(userId = "u1", contributed = 10000L, totalSpent = 5000L),
            contributedAmount = BigDecimal("100.00"),
            withdrawnAmount = BigDecimal("20.00"),
            cashSpentAmount = BigDecimal("10.00"),
            nonCashSpentAmount = BigDecimal("40.00"),
            totalSpentAmount = BigDecimal("50.00"),
            pocketBalanceAmount = BigDecimal("30.00"),
            cashInHandAmount = BigDecimal("10.00"),
            netBalanceCents = 4000L,
            netBalanceAmount = BigDecimal("40.00")
        )
        val subunitItem = TripReportSubunitBalanceItem(
            subunit = subunit,
            contributedCents = 10000L,
            contributedAmount = BigDecimal("100.00"),
            withdrawnCents = 2000L,
            withdrawnAmount = BigDecimal("20.00"),
            totalSpentCents = 5000L,
            totalSpentAmount = BigDecimal("50.00"),
            pocketBalanceCents = 3000L,
            pocketBalanceAmount = BigDecimal("30.00"),
            cashInHandCents = 1000L,
            cashInHandAmount = BigDecimal("10.00"),
            netBalanceCents = 4000L,
            netBalanceAmount = BigDecimal("40.00")
        )
        return createReport("Shared Flat").copy(
            expenses = createAllStatusExpenses(),
            contributions = createAllScopeContributions(),
            withdrawals = listOf(createWithdrawalItem()),
            memberBalances = listOf(memberItem),
            subunitBalances = listOf(subunitItem),
            settlements = createAllSettlementItems(),
            subunits = mapOf("sub-1" to subunit)
        )
    }

    private fun createAllStatusExpenses(): List<TripReportExpenseItem> =
        PaymentStatus.entries.mapIndexed { idx, status ->
            val payerType = PayerType.entries[idx % PayerType.entries.size]
            val payerId = if (payerType == PayerType.SUBUNIT) "sub-1" else "u1"
            TripReportExpenseItem(
                expense = Expense(
                    id = "exp-$idx",
                    groupId = "g1",
                    title = "Item $idx",
                    sourceAmount = 1000L,
                    sourceCurrency = "EUR",
                    groupAmount = 1000L,
                    groupCurrency = "EUR",
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.RESTAURANT,
                    paymentStatus = status,
                    payerType = payerType,
                    payerId = payerId,
                    createdBy = "u1",
                    operationDate = LocalDateTime.of(2026, 11, 10, 12, 0)
                ),
                sourceAmount = BigDecimal("10.00"),
                baseGroupAmount = BigDecimal("10.00"),
                addOnExtrasCents = 0L,
                addOnExtrasAmount = BigDecimal("0.00"),
                effectiveGroupAmountCents = 1000L,
                effectiveGroupAmount = BigDecimal("10.00"),
                exchangeRate = BigDecimal.ONE
            )
        }

    private fun createAllScopeContributions(): List<TripReportContributionItem> =
        PayerType.entries.mapIndexed { idx, scope ->
            TripReportContributionItem(
                contribution = Contribution(
                    id = "c-$idx",
                    groupId = "g1",
                    userId = "u1",
                    contributionScope = scope,
                    subunitId = if (scope == PayerType.SUBUNIT) "sub-1" else null,
                    amount = 5000L,
                    currency = "EUR",
                    contributionDate = LocalDateTime.of(2026, 11, 2, 10, 0)
                ),
                amount = BigDecimal("50.00"),
                equivalentBaseAmountCents = 5000L,
                equivalentBaseAmount = BigDecimal("50.00")
            )
        }

    private fun createWithdrawalItem(): TripReportWithdrawalItem = TripReportWithdrawalItem(
        withdrawal = CashWithdrawal(
            id = "w1",
            groupId = "g1",
            withdrawnBy = "u1",
            withdrawalScope = PayerType.GROUP,
            amountWithdrawn = 2000L,
            remainingAmount = 1000L,
            currency = "EUR",
            deductedBaseAmount = 2000L,
            createdAt = LocalDateTime.of(2026, 11, 3, 12, 0)
        ),
        amountWithdrawn = BigDecimal("20.00"),
        remainingAmount = BigDecimal("10.00"),
        deductedBaseAmount = BigDecimal("20.00"),
        effectiveDeductedBaseCents = 2100L,
        effectiveDeductedBaseAmount = BigDecimal("21.00"),
        addOnFeesCents = 100L,
        addOnFeesAmount = BigDecimal("1.00")
    )

    private fun createAllSettlementItems(): List<TripReportSettlementItem> =
        SettlementStatus.entries.mapIndexed { idx, status ->
            val pocket = SettlementPocketType.entries[idx % SettlementPocketType.entries.size]
            TripReportSettlementItem(
                record = SettlementRecord(
                    id = "s-$idx",
                    groupId = "g1",
                    settlement = Settlement(
                        fromUserId = "u2",
                        toUserId = "u1",
                        amount = 1500L,
                        currency = "EUR",
                        sourcePocket = pocket
                    ),
                    status = status,
                    createdAt = LocalDateTime.of(2026, 11, 15, 10, 0)
                ),
                currency = "EUR",
                amountCents = 1500L,
                amount = BigDecimal("15.00")
            )
        }

    private fun createReport(groupName: String): GroupTripReport = GroupTripReport(
        group = Group(id = "g1", name = groupName, currency = "EUR", members = listOf("u1")),
        period = SpendingInsightsPeriod.AllTime,
        windowStartDate = LocalDate.of(2026, 11, 1),
        windowEndDate = LocalDate.of(2026, 11, 12),
        generatedAt = LocalDateTime.of(2026, 11, 20, 15, 0),
        baseCurrency = "EUR",
        pocketBalance = GroupPocketBalance(
            totalContributions = 20000L,
            totalExpenses = 12550L,
            virtualBalance = 7000L,
            currency = "EUR",
            totalCashEquivalent = 0L,
            totalExtras = 450L
        ),
        totalContributedAmount = BigDecimal("200.00"),
        totalExpensesAmount = BigDecimal("125.50"),
        virtualPocketBalanceAmount = BigDecimal("70.00"),
        totalCashEquivalentAmount = BigDecimal("0.00"),
        totalExtrasAmount = BigDecimal("4.50"),
        expenses = listOf(createExpenseItem()),
        contributions = emptyList(),
        withdrawals = emptyList(),
        memberBalances = emptyList(),
        subunitBalances = emptyList(),
        settlements = emptyList(),
        memberProfiles = mapOf(
            "u1" to User(userId = "u1", email = "alice@test.com", displayName = "Alice")
        ),
        subunits = emptyMap(),
        currentUserId = "u1"
    )

    private fun createExpenseItem(): TripReportExpenseItem = TripReportExpenseItem(
        expense = Expense(
            id = "exp-1",
            groupId = "g1",
            title = "Dinner",
            sourceAmount = 12550L,
            sourceCurrency = "EUR",
            groupAmount = 12550L,
            groupCurrency = "EUR",
            exchangeRate = BigDecimal("1.000000"),
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            paymentStatus = PaymentStatus.FINISHED,
            payerType = PayerType.USER,
            payerId = "u1",
            createdBy = "u1",
            operationDate = LocalDateTime.of(2026, 11, 12, 21, 0)
        ),
        sourceAmount = BigDecimal("125.50"),
        baseGroupAmount = BigDecimal("125.50"),
        addOnExtrasCents = 450L,
        addOnExtrasAmount = BigDecimal("4.50"),
        effectiveGroupAmountCents = 13000L,
        effectiveGroupAmount = BigDecimal("130.00"),
        exchangeRate = BigDecimal("1.000000")
    )
}
