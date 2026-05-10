package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashTranchePreview
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.WithdrawalPoolOption
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toFundingSourceStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddExpenseOptionsUiMapperTest {

    private lateinit var mapper: AddExpenseOptionsUiMapper
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var formattingHelper: FormattingHelper

    private val eurDomain = Currency(
        code = "EUR",
        symbol = "€",
        defaultName = "Euro",
        decimalDigits = 2
    )

    private val usdDomain = Currency(
        code = "USD",
        symbol = "$",
        defaultName = "US Dollar",
        decimalDigits = 2
    )

    private val jpyDomain = Currency(
        code = "JPY",
        symbol = "¥",
        defaultName = "Japanese Yen",
        decimalDigits = 0
    )

    private val tndDomain = Currency(
        code = "TND",
        symbol = "د.ت",
        defaultName = "Tunisian Dinar",
        decimalDigits = 3
    )

    // UI Models for test state construction
    private val eurUi = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val usdUi = CurrencyUiModel(code = "USD", displayText = "USD ($)", decimalDigits = 2)

    @BeforeEach
    fun setup() {
        resourceProvider = mockk(relaxed = true)
        formattingHelper = mockk(relaxed = true)

        // Stub all expense category string resources
        ExpenseCategory.entries.forEach { category ->
            every { resourceProvider.getString(category.toStringRes()) } returns category.name
        }

        // Stub all payment status string resources
        PaymentStatus.entries.forEach { status ->
            every { resourceProvider.getString(status.toStringRes()) } returns status.name
        }

        mapper = AddExpenseOptionsUiMapper(resourceProvider, formattingHelper)
    }

    @Nested
    inner class MapCurrency {

        @Test
        fun `maps EUR domain currency to UI model`() {
            val result = mapper.mapCurrency(eurDomain)
            assertEquals("EUR", result.code)
            assertEquals(2, result.decimalDigits)
            assertTrue(result.displayText.contains("EUR"))
        }

        @Test
        fun `maps JPY domain currency to UI model`() {
            val result = mapper.mapCurrency(jpyDomain)
            assertEquals("JPY", result.code)
            assertEquals(0, result.decimalDigits)
            assertTrue(result.displayText.contains("JPY"))
        }

        @Test
        fun `maps TND domain currency to UI model with 3 decimal places`() {
            val result = mapper.mapCurrency(tndDomain)
            assertEquals("TND", result.code)
            assertEquals(3, result.decimalDigits)
            assertTrue(result.displayText.contains("TND"))
        }

        @Test
        fun `maps known EUR currency with localized name from resourceProvider`() {
            every { resourceProvider.getString(DesignR.string.currency_name_eur) } returns "Euro (localized)"

            val result = mapper.mapCurrency(eurDomain)

            assertEquals("Euro", result.defaultName)
            assertEquals("Euro (localized)", result.localizedName)
            verify { resourceProvider.getString(DesignR.string.currency_name_eur) }
        }

        @Test
        fun `maps known USD currency with Spanish localized name`() {
            every { resourceProvider.getString(DesignR.string.currency_name_usd) } returns "Dólar estadounidense"

            val result = mapper.mapCurrency(usdDomain)

            assertEquals("US Dollar", result.defaultName)
            assertEquals("Dólar estadounidense", result.localizedName)
            verify { resourceProvider.getString(DesignR.string.currency_name_usd) }
        }

        @Test
        fun `maps unknown TND currency falling back to defaultName`() {
            val result = mapper.mapCurrency(tndDomain)

            assertEquals("Tunisian Dinar", result.defaultName)
            assertEquals("Tunisian Dinar", result.localizedName)
        }

        @Test
        fun `maps currency list preserving localized names`() {
            every { resourceProvider.getString(DesignR.string.currency_name_eur) } returns "Euro (localized)"

            val result = mapper.mapCurrencies(listOf(eurDomain, tndDomain))

            assertEquals("Euro (localized)", result[0].localizedName)
            assertEquals("Tunisian Dinar", result[1].localizedName)
        }
    }

    @Nested
    inner class MapCurrencies {

        @Test
        fun `maps list of domain currencies to UI models`() {
            val result = mapper.mapCurrencies(listOf(eurDomain, usdDomain, jpyDomain))
            assertEquals(3, result.size)
            assertEquals("EUR", result[0].code)
            assertEquals("USD", result[1].code)
            assertEquals("JPY", result[2].code)
        }

        @Test
        fun `maps empty list`() {
            val result = mapper.mapCurrencies(emptyList())
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class MapPaymentMethods {

        @Test
        fun `maps payment methods to UI models`() {
            every { resourceProvider.getString(any()) } answers {
                when (firstArg<Int>()) {
                    else -> "Mocked"
                }
            }
            val result = mapper.mapPaymentMethods(PaymentMethod.entries)
            assertEquals(PaymentMethod.entries.size, result.size)
            assertEquals("CASH", result[0].id)
            assertEquals("PIX", result[2].id)
            assertEquals("CREDIT_CARD", result[3].id)
        }
    }

    @Nested
    inner class BuildLabels {

        @Test
        fun `builds exchange rate label`() {
            every { resourceProvider.getString(any(), any(), any()) } returns "1 EUR (€) = ? USD ($)"
            val result = mapper.buildExchangeRateLabel(eurUi, usdUi)
            assertEquals("1 EUR (€) = ? USD ($)", result)
        }

        @Test
        fun `builds group amount label`() {
            every { resourceProvider.getString(any(), any()) } returns "in EUR (€)"
            val result = mapper.buildGroupAmountLabel(eurUi)
            assertEquals("in EUR (€)", result)
        }
    }

    @Nested
    inner class MapCategories {

        @Test
        fun `filters out CONTRIBUTION and REFUND`() {
            val result = mapper.mapCategories(ExpenseCategory.entries)

            val ids = result.map { it.id }
            assertTrue("CONTRIBUTION" !in ids)
            assertTrue("REFUND" !in ids)
        }

        @Test
        fun `includes user-selectable categories`() {
            val result = mapper.mapCategories(ExpenseCategory.entries)

            val ids = result.map { it.id }
            assertTrue("TRANSPORT" in ids)
            assertTrue("FOOD" in ids)
            assertTrue("LODGING" in ids)
            assertTrue("ACTIVITIES" in ids)
            assertTrue("INSURANCE" in ids)
            assertTrue("ENTERTAINMENT" in ids)
            assertTrue("SHOPPING" in ids)
            assertTrue("OTHER" in ids)
        }

        @Test
        fun `maps display text from resource provider`() {
            every { resourceProvider.getString(ExpenseCategory.FOOD.toStringRes()) } returns "Food & Restaurants"

            val result = mapper.mapCategories(listOf(ExpenseCategory.FOOD))

            assertEquals(1, result.size)
            assertEquals("FOOD", result[0].id)
            assertEquals("Food & Restaurants", result[0].displayText)
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.mapCategories(emptyList())
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class MapPaymentStatuses {

        @Test
        fun `only includes FINISHED and SCHEDULED`() {
            val result = mapper.mapPaymentStatuses(PaymentStatus.entries)

            assertEquals(2, result.size)
            val ids = result.map { it.id }
            assertTrue("FINISHED" in ids)
            assertTrue("SCHEDULED" in ids)
        }

        @Test
        fun `excludes RECEIVED, PENDING, and CANCELLED`() {
            val result = mapper.mapPaymentStatuses(PaymentStatus.entries)

            val ids = result.map { it.id }
            assertTrue("RECEIVED" !in ids)
            assertTrue("PENDING" !in ids)
            assertTrue("CANCELLED" !in ids)
        }

        @Test
        fun `maps display text from resource provider`() {
            every { resourceProvider.getString(PaymentStatus.SCHEDULED.toStringRes()) } returns "Scheduled"

            val result = mapper.mapPaymentStatuses(listOf(PaymentStatus.SCHEDULED))

            assertEquals(1, result.size)
            assertEquals("SCHEDULED", result[0].id)
            assertEquals("Scheduled", result[0].displayText)
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.mapPaymentStatuses(emptyList())
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class MapFundingSources {

        @BeforeEach
        fun stubFundingSourceStrings() {
            PayerType.entries
                .filter { it != PayerType.SUBUNIT }
                .forEach { payerType ->
                    every {
                        resourceProvider.getString(payerType.toFundingSourceStringRes())
                    } returns payerType.name
                }
        }

        @Test
        fun `excludes SUBUNIT from user-selectable funding sources`() {
            val result = mapper.mapFundingSources(PayerType.entries)

            val ids = result.map { it.id }
            assertTrue("SUBUNIT" !in ids)
        }

        @Test
        fun `includes GROUP and USER`() {
            val result = mapper.mapFundingSources(PayerType.entries)

            val ids = result.map { it.id }
            assertTrue("GROUP" in ids)
            assertTrue("USER" in ids)
        }

        @Test
        fun `maps display text from resource provider`() {
            every { resourceProvider.getString(PayerType.GROUP.toFundingSourceStringRes()) } returns "Group Pocket"
            every { resourceProvider.getString(PayerType.USER.toFundingSourceStringRes()) } returns "My Money"

            val result = mapper.mapFundingSources(listOf(PayerType.GROUP, PayerType.USER))

            assertEquals(2, result.size)
            assertEquals("GROUP", result[0].id)
            assertEquals("Group Pocket", result[0].displayText)
            assertEquals("USER", result[1].id)
            assertEquals("My Money", result[1].displayText)
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.mapFundingSources(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns only GROUP when only GROUP and SUBUNIT provided`() {
            val result = mapper.mapFundingSources(listOf(PayerType.GROUP, PayerType.SUBUNIT))

            assertEquals(1, result.size)
            assertEquals("GROUP", result[0].id)
        }
    }

    // ── MapCashTranchePreviews ────────────────────────────────────────────────

    @Nested
    inner class MapCashTranchePreviews {

        private val sourceCurrency = "THB"
        private val date = LocalDateTime.of(2026, 1, 10, 12, 0)

        private val trancheWithTitle = CashTranchePreview(
            withdrawalId = "w-1",
            withdrawalTitle = "Airport ATM",
            withdrawalDate = date,
            amountConsumedCents = 50000L,
            remainingAfterCents = 950000L,
            withdrawalRate = BigDecimal("37.037037")
        )

        private val trancheNoTitle = CashTranchePreview(
            withdrawalId = "w-2",
            withdrawalTitle = null,
            withdrawalDate = date,
            amountConsumedCents = 30000L,
            remainingAfterCents = 470000L,
            withdrawalRate = BigDecimal("36.496350")
        )

        private val trancheBlankTitle = CashTranchePreview(
            withdrawalId = "w-3",
            withdrawalTitle = "  ",
            withdrawalDate = date,
            amountConsumedCents = 20000L,
            remainingAfterCents = 0L,
            withdrawalRate = BigDecimal("37.000000")
        )

        private val trancheNullDate = CashTranchePreview(
            withdrawalId = "w-4",
            withdrawalTitle = null,
            withdrawalDate = null,
            amountConsumedCents = 10000L,
            remainingAfterCents = 10000L,
            withdrawalRate = BigDecimal("36.000000")
        )

        @BeforeEach
        fun stubFormattingHelper() {
            every { formattingHelper.formatShortDate(date) } returns "10 Jan"
            every { formattingHelper.formatShortDate(null) } returns ""
            every { formattingHelper.formatCentsWithCurrency(any(), sourceCurrency) } answers {
                "THB ${firstArg<Long>() / 100}"
            }
            every { formattingHelper.formatRateForDisplay(any()) } answers { firstArg() }
            every {
                resourceProvider.getString(R.string.add_expense_cash_tranche_atm_label, "10 Jan")
            } returns "ATM — 10 Jan"
            every {
                resourceProvider.getString(R.string.add_expense_cash_tranche_atm_label_no_date)
            } returns "ATM"
        }

        @Test
        fun `uses withdrawal title as label when present and non-blank`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheWithTitle), sourceCurrency)

            assertEquals(1, result.size)
            assertEquals("Airport ATM", result[0].withdrawalLabel)
        }

        @Test
        fun `falls back to ATM date label when title is null`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheNoTitle), sourceCurrency)

            assertEquals("ATM — 10 Jan", result[0].withdrawalLabel)
        }

        @Test
        fun `falls back to ATM date label when title is blank`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheBlankTitle), sourceCurrency)

            assertEquals("ATM — 10 Jan", result[0].withdrawalLabel)
        }

        @Test
        fun `falls back to ATM no-date label when title is null and date is null`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheNullDate), sourceCurrency)

            assertEquals("ATM", result[0].withdrawalLabel)
        }

        @Test
        fun `delegates formattedAmountConsumed to formattingHelper formatCentsWithCurrency`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheWithTitle), sourceCurrency)

            verify { formattingHelper.formatCentsWithCurrency(50000L, sourceCurrency) }
            assertEquals("THB 500", result[0].formattedAmountConsumed)
        }

        @Test
        fun `delegates formattedRemainingAfter to formattingHelper formatCentsWithCurrency`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheWithTitle), sourceCurrency)

            verify { formattingHelper.formatCentsWithCurrency(950000L, sourceCurrency) }
            assertEquals("THB 9500", result[0].formattedRemainingAfter)
        }

        @Test
        fun `delegates formattedRate to formattingHelper formatRateForDisplay`() {
            val result = mapper.mapCashTranchePreviews(listOf(trancheWithTitle), sourceCurrency)

            verify { formattingHelper.formatRateForDisplay("37.037037") }
            assertEquals("37.037037", result[0].formattedRate)
        }

        @Test
        fun `maps multiple tranches preserving FIFO order`() {
            val result = mapper.mapCashTranchePreviews(
                listOf(trancheWithTitle, trancheNoTitle),
                sourceCurrency
            )

            assertEquals(2, result.size)
            assertEquals("Airport ATM", result[0].withdrawalLabel)
            assertEquals("ATM — 10 Jan", result[1].withdrawalLabel)
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.mapCashTranchePreviews(emptyList(), sourceCurrency)

            assertTrue(result.isEmpty())
        }
    }

    // ── MapWithdrawalPoolOptions ─────────────────────────────────────────────

    @Nested
    inner class MapWithdrawalPoolOptions {

        @BeforeEach
        fun setupPoolStrings() {
            every {
                resourceProvider.getString(R.string.add_expense_cash_pool_personal)
            } returns "My personal cash"

            every {
                resourceProvider.getString(R.string.add_expense_cash_pool_group)
            } returns "Group cash"

            every { resourceProvider.getString(R.string.add_expense_cash_pool_subunit, "Alpha") } returns "Alpha cash"
            every { resourceProvider.getString(R.string.add_expense_cash_pool_subunit, "Beta") } returns "Beta cash"
            every {
                resourceProvider.getString(R.string.add_expense_cash_pool_subunit_generic)
            } returns "Subunit cash"
        }

        @Test
        fun `returns empty list when pools is empty`() {
            val result = mapper.mapWithdrawalPoolOptions(emptyList())

            assertTrue(result.isEmpty())
        }

        @Test
        fun `maps USER pool to personal cash label`() {
            val pool = WithdrawalPoolOption(scope = PayerType.USER, ownerId = "user-1")

            val result = mapper.mapWithdrawalPoolOptions(listOf(pool))

            assertEquals(1, result.size)
            assertEquals(PayerType.USER, result[0].scope)
            assertEquals("user-1", result[0].ownerId)
            assertEquals("My personal cash", result[0].displayLabel)
        }

        @Test
        fun `maps GROUP pool to group cash label with null ownerId`() {
            val pool = WithdrawalPoolOption(scope = PayerType.GROUP, ownerId = null)

            val result = mapper.mapWithdrawalPoolOptions(listOf(pool))

            assertEquals(1, result.size)
            assertEquals(PayerType.GROUP, result[0].scope)
            assertNull(result[0].ownerId)
            assertEquals("Group cash", result[0].displayLabel)
        }

        @Test
        fun `maps SUBUNIT pool with known name from lookup`() {
            val pool = WithdrawalPoolOption(scope = PayerType.SUBUNIT, ownerId = "sub-1")
            val lookup = mapOf("sub-1" to "Alpha")

            val result = mapper.mapWithdrawalPoolOptions(listOf(pool), lookup)

            assertEquals(1, result.size)
            assertEquals(PayerType.SUBUNIT, result[0].scope)
            assertEquals("sub-1", result[0].ownerId)
            assertEquals("Alpha cash", result[0].displayLabel)
        }

        @Test
        fun `maps SUBUNIT pool with unknown ownerId to generic label`() {
            val pool = WithdrawalPoolOption(scope = PayerType.SUBUNIT, ownerId = "sub-99")
            val lookup = mapOf("sub-1" to "Alpha")

            val result = mapper.mapWithdrawalPoolOptions(listOf(pool), lookup)

            assertEquals("Subunit cash", result[0].displayLabel)
        }

        @Test
        fun `maps SUBUNIT pool with null ownerId to generic label`() {
            val pool = WithdrawalPoolOption(scope = PayerType.SUBUNIT, ownerId = null)

            val result = mapper.mapWithdrawalPoolOptions(listOf(pool))

            assertEquals("Subunit cash", result[0].displayLabel)
        }

        @Test
        fun `maps SUBUNIT pool with blank name in lookup to generic label`() {
            val pool = WithdrawalPoolOption(scope = PayerType.SUBUNIT, ownerId = "sub-1")
            val lookup = mapOf("sub-1" to "   ")

            val result = mapper.mapWithdrawalPoolOptions(listOf(pool), lookup)

            assertEquals("Subunit cash", result[0].displayLabel)
        }

        @Test
        fun `maps multiple pools preserving order`() {
            val pools = listOf(
                WithdrawalPoolOption(scope = PayerType.GROUP, ownerId = null),
                WithdrawalPoolOption(scope = PayerType.USER, ownerId = "user-1"),
                WithdrawalPoolOption(scope = PayerType.SUBUNIT, ownerId = "sub-1")
            )
            val lookup = mapOf("sub-1" to "Alpha")

            val result = mapper.mapWithdrawalPoolOptions(pools, lookup)

            assertEquals(3, result.size)
            assertEquals(PayerType.GROUP, result[0].scope)
            assertEquals(PayerType.USER, result[1].scope)
            assertEquals(PayerType.SUBUNIT, result[2].scope)
        }
    }
}
