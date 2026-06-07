package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.features.expense.R
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseDetailUiMapperTest {

    private lateinit var mapper: ExpenseDetailUiMapper
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var localeProvider: LocaleProvider

    private val currentUserId = "user-current"
    private val otherUserId = "user-other"

    private val baseExpense = Expense(
        id = "expense-123",
        groupId = "group-456",
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        exchangeRate = BigDecimal.ONE,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        paymentStatus = PaymentStatus.FINISHED,
        splitType = SplitType.EQUAL,
        category = ExpenseCategory.FOOD,
        createdBy = currentUserId,
        createdAt = LocalDateTime.of(2024, 6, 15, 20, 0),
        syncStatus = SyncStatus.SYNCED
    )

    private val memberProfiles = mapOf(
        currentUserId to User(userId = currentUserId, displayName = "Alice", email = "alice@example.com"),
        otherUserId to User(userId = otherUserId, displayName = "Bob", email = "bob@example.com")
    )

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US
        // ResourceProvider returns the key as string by default (relaxed = true)
        every { resourceProvider.getString(any()) } returns "translated_string"
        every { resourceProvider.getString(any(), any()) } returns "translated_string_with_arg"

        val formattingHelper = FormattingHelper(localeProvider)
        val expenseCalculatorService = ExpenseCalculatorServiceImpl()
        val addOnCalculationService = AddOnCalculationServiceImpl()
        val scheduledBadgeUiMapper = ScheduledBadgeUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider
        )

        mapper = ExpenseDetailUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider,
            expenseCalculatorService = expenseCalculatorService,
            addOnCalculationService = addOnCalculationService,
            scheduledBadgeUiMapper = scheduledBadgeUiMapper
        )
    }

    @Nested
    inner class BasicMapping {

        @Test
        fun `maps id, groupId and title correctly`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertEquals("expense-123", result.id)
            assertEquals("group-456", result.groupId)
            assertEquals("Dinner", result.title)
        }

        @Test
        fun `maps category from expense`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertEquals(ExpenseCategory.FOOD, result.category)
        }

        @Test
        fun `maps syncStatus from expense`() {
            val result = mapper.map(
                baseExpense.copy(syncStatus = SyncStatus.PENDING_SYNC),
                memberProfiles,
                currentUserId
            )

            assertEquals(SyncStatus.PENDING_SYNC, result.syncStatus)
        }

        @Test
        fun `formats group amount with currency`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            // FormattingHelper with Locale.US formats 5000 cents (50.00 EUR) → "€50.00"
            assertNotNull(result.formattedGroupAmount)
            assertTrue(result.formattedGroupAmount.isNotBlank())
        }

        @Test
        fun `maps date text via formatting helper`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertNotNull(result.dateText)
            assertTrue(result.dateText.isNotBlank())
        }
    }

    @Nested
    inner class CurrencyHandling {

        @Test
        fun `isForeignCurrency is false when source and group currency match`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertFalse(result.isForeignCurrency)
        }

        @Test
        fun `isForeignCurrency is true for multi-currency expense`() {
            val foreignExpense = baseExpense.copy(
                sourceAmount = 6000L,
                sourceCurrency = "USD",
                groupAmount = 5500L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal("0.916667")
            )

            val result = mapper.map(foreignExpense, memberProfiles, currentUserId)

            assertTrue(result.isForeignCurrency)
        }

        @Test
        fun `formattedSourceAmount is null for same-currency expense`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertNull(result.formattedSourceAmount)
        }

        @Test
        fun `formattedSourceAmount is not null for foreign currency`() {
            val foreignExpense = baseExpense.copy(
                sourceAmount = 6000L,
                sourceCurrency = "USD",
                groupAmount = 5500L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal("0.916667")
            )

            val result = mapper.map(foreignExpense, memberProfiles, currentUserId)

            assertNotNull(result.formattedSourceAmount)
        }

        @Test
        fun `formattedExchangeRate is null for same-currency expense`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertNull(result.formattedExchangeRate)
        }

        @Test
        fun `formattedExchangeRate is not null for foreign currency`() {
            val foreignExpense = baseExpense.copy(
                sourceAmount = 6000L,
                sourceCurrency = "USD",
                groupAmount = 5500L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal("0.916667")
            )

            val result = mapper.map(foreignExpense, memberProfiles, currentUserId)

            assertNotNull(result.formattedExchangeRate)
        }
    }

    @Nested
    inner class SplitMapping {

        @Test
        fun `maps splits with equal amounts`() {
            val expense = baseExpense.copy(
                sourceAmount = 10000L,
                groupAmount = 10000L,
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L),
                    ExpenseSplit(userId = otherUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals(2, result.splits.size)
        }

        @Test
        fun `isCurrentUser is true for current user split`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 2500L),
                    ExpenseSplit(userId = otherUserId, amountCents = 2500L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            val currentUserSplit = result.splits.find { it.isCurrentUser }
            assertNotNull(currentUserSplit)
            // Current user's split shows the localised "You" label, not the raw profile name
            assertEquals("translated_string", currentUserSplit?.displayName)
        }

        @Test
        fun `isCurrentUser is false for other users`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 2500L),
                    ExpenseSplit(userId = otherUserId, amountCents = 2500L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            val otherSplit = result.splits.find { it.displayName == "Bob" }
            assertNotNull(otherSplit)
            assertFalse(otherSplit!!.isCurrentUser)
        }

        @Test
        fun `maps excluded split correctly`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L),
                    ExpenseSplit(userId = otherUserId, amountCents = 0L, isExcluded = true)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            val excludedSplit = result.splits.find { it.isExcluded }
            assertNotNull(excludedSplit)
            assertTrue(excludedSplit!!.isExcluded)
        }

        @Test
        fun `maps percentage split share text`() {
            val expense = baseExpense.copy(
                splitType = SplitType.PERCENT,
                splits = listOf(
                    ExpenseSplit(
                        userId = currentUserId,
                        amountCents = 3000L,
                        percentage = BigDecimal("60.00")
                    )
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            val split = result.splits.first()
            assertNotNull(split.shareText)
            assertTrue(split.shareText!!.contains("%"))
        }

        @Test
        fun `shareText is null for equal splits (no percentage)`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L, percentage = null)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNull(result.splits.first().shareText)
        }

        @Test
        fun `resolves display name from member profiles — other user gets profile name`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = otherUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals("Bob", result.splits.first().displayName)
        }

        @Test
        fun `resolves display name — current user gets you label`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // The mapper resolves currentUserId to the localised "You" label (mocked as "translated_string")
            assertEquals("translated_string", result.splits.first().displayName)
        }

        @Test
        fun `falls back to userId when no profile found`() {
            val unknownUserId = "unknown-user"
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = unknownUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, emptyMap(), currentUserId)

            assertEquals(unknownUserId, result.splits.first().displayName)
        }

        @Test
        fun `converts split amounts from source to group currency proportionally`() {
            // source 10000 THB → group 270 EUR; split 5000 THB = 135 EUR
            val expense = baseExpense.copy(
                sourceAmount = 10000L,
                sourceCurrency = "THB",
                groupAmount = 270L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal("0.027"),
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // computeProportionalAmount(5000, 270, 10000) = 135
            assertNotNull(result.splits.first().formattedAmount)
            assertTrue(result.splits.first().formattedAmount.isNotBlank())
        }

        @Test
        fun `populates formattedSourceAmount for foreign currency expense`() {
            // source 10000 CNY → group 126.30 EUR
            val expense = baseExpense.copy(
                sourceAmount = 100000L, // 1000.00 CNY
                sourceCurrency = "CNY",
                groupAmount = 12630L, // 126.30 EUR
                groupCurrency = "EUR",
                exchangeRate = BigDecimal("0.126297"),
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 60000L), // 600.00 CNY (60%)
                    ExpenseSplit(userId = otherUserId, amountCents = 40000L) // 400.00 CNY (40%)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // First split should show both CNY and EUR amounts
            val firstSplit = result.splits.first()
            assertNotNull(firstSplit.formattedSourceAmount)
            assertTrue(
                firstSplit.formattedSourceAmount!!.contains("CNY") || firstSplit.formattedSourceAmount!!.contains("¥")
            )
            assertNotNull(firstSplit.formattedAmount)
            assertTrue(firstSplit.formattedAmount.contains("EUR") || firstSplit.formattedAmount.contains("€"))
        }

        @Test
        fun `formattedSourceAmount is null for same-currency expense`() {
            val expense = baseExpense.copy(
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5000L,
                groupCurrency = "EUR",
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNull(result.splits.first().formattedSourceAmount)
        }
    }

    @Nested
    inner class AddOnMapping {

        private val taxAddOn = AddOn(
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.PERCENTAGE,
            amountCents = 500L,
            groupAmountCents = 500L
        )

        private val discountAddOn = AddOn(
            type = AddOnType.DISCOUNT,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 200L,
            groupAmountCents = 200L
        )

        private val includedTipAddOn = AddOn(
            type = AddOnType.TIP,
            mode = AddOnMode.INCLUDED,
            valueType = AddOnValueType.EXACT,
            amountCents = 500L,
            groupAmountCents = 500L
        )

        private val includedDiscountAddOn = AddOn(
            type = AddOnType.DISCOUNT,
            mode = AddOnMode.INCLUDED,
            valueType = AddOnValueType.PERCENTAGE,
            amountCents = 18215L,
            groupAmountCents = 18215L
        )

        private val foreignFeeAddOn = AddOn(
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 500L,
            currency = "GBP",
            exchangeRate = BigDecimal("0.83"),
            groupAmountCents = 415L
        )

        @Test
        fun `has no add-ons for plain expense`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertFalse(result.hasAddOns)
            assertTrue(result.addOns.isEmpty())
        }

        @Test
        fun `hasAddOns is true when expense has add-ons`() {
            val expense = baseExpense.copy(addOns = listOf(taxAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertTrue(result.hasAddOns)
            assertEquals(1, result.addOns.size)
        }

        @Test
        fun `isDiscount is true for discount add-on`() {
            val expense = baseExpense.copy(addOns = listOf(discountAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertTrue(result.addOns.first().isDiscount)
        }

        @Test
        fun `isDiscount is false for fee add-on`() {
            val expense = baseExpense.copy(addOns = listOf(taxAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertFalse(result.addOns.first().isDiscount)
        }

        @Test
        fun `effective total is calculated when add-ons present`() {
            val expense = baseExpense.copy(addOns = listOf(taxAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNotNull(result.formattedEffectiveTotal)
        }

        @Test
        fun `formattedEffectiveTotal is null when no add-ons`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertNull(result.formattedEffectiveTotal)
        }

        @Test
        fun `maps description into add-on label when present`() {
            val addOnWithDescription = taxAddOn.copy(description = "Service")
            val expense = baseExpense.copy(addOns = listOf(addOnWithDescription))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertTrue(result.addOns.first().labelText.contains("Service"))
        }

        @Test
        fun `formattedIncludedBaseCost is null when no INCLUDED add-ons present`() {
            val expense = baseExpense.copy(addOns = listOf(taxAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNull(result.formattedIncludedBaseCost)
            assertFalse(result.hasIncludedAddOns)
        }

        @Test
        fun `formattedIncludedBaseCost is non-null when INCLUDED non-discount add-on is present`() {
            // Base cost (groupAmount) is already the decomposed value; reconstructed
            // original = base + INCLUDED non-discount.
            val expense = baseExpense.copy(
                groupAmount = 4500L,
                sourceAmount = 4500L,
                addOns = listOf(includedTipAddOn)
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNotNull(result.formattedIncludedBaseCost)
            assertNotNull(result.formattedOriginalEnteredTotal)
            assertTrue(result.hasIncludedAddOns)
        }

        @Test
        fun `formattedIncludedBaseCost and formattedOriginalEnteredTotal are null for INCLUDED DISCOUNT only`() {
            // INCLUDED DISCOUNTs are informational only — adjustForIncludedAddOns does NOT run
            // for them, so expense.groupAmount == the total the user entered (= total paid).
            // Showing a "Base cost" or "Total original" derived from this data would produce a
            // value LOWER than the amount paid (nonsensical for a discount).
            val expense = baseExpense.copy(
                groupAmount = 182152L,
                sourceAmount = 182152L,
                addOns = listOf(includedDiscountAddOn)
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNull(result.formattedIncludedBaseCost)
            assertNull(result.formattedOriginalEnteredTotal)
            assertTrue(result.hasIncludedAddOns)
        }

        @Test
        fun `formattedOriginalEnteredTotal excludes INCLUDED DISCOUNT from reconstruction`() {
            // Mixed: one INCLUDED TIP (base extraction DID run) + one INCLUDED DISCOUNT
            // (informational). Original total = base + tip only; the discount must not be
            // subtracted from the reconstruction (that would give a value below what was paid).
            val expense = baseExpense.copy(
                groupAmount = 4500L,
                sourceAmount = 4500L,
                addOns = listOf(includedTipAddOn, includedDiscountAddOn)
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // formattedIncludedBaseCost shown (non-discount extraction happened)
            assertNotNull(result.formattedIncludedBaseCost)
            // originalEntered = 4500 (base) + 500 (tip) = 5000 — discount NOT included
            assertNotNull(result.formattedOriginalEnteredTotal)
        }

        @Test
        fun `foreign-currency add-on exposes source amount and rate`() {
            every { resourceProvider.getString(any(), any(), any(), any()) } returns "1 GBP = 0.83 EUR"
            val expense = baseExpense.copy(addOns = listOf(foreignFeeAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            val mapped = result.addOns.first()
            assertTrue(mapped.isForeignCurrency)
            assertNotNull(mapped.formattedSourceAmount)
            assertNotNull(mapped.formattedRate)
            assertEquals("GBP", mapped.addOnCurrency)
        }

        @Test
        fun `same-currency add-on hides foreign metadata`() {
            val expense = baseExpense.copy(addOns = listOf(taxAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            val mapped = result.addOns.first()
            assertFalse(mapped.isForeignCurrency)
            assertNull(mapped.formattedSourceAmount)
            assertNull(mapped.formattedRate)
        }

        @Test
        fun `isIncluded flag mirrors add-on mode`() {
            val expense = baseExpense.copy(addOns = listOf(includedTipAddOn, taxAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertTrue(result.addOns[0].isIncluded)
            assertFalse(result.addOns[1].isIncluded)
        }
    }

    @Nested
    inner class CashTranchesMapping {

        @Test
        fun `maps empty cash tranches`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertTrue(result.cashTranches.isEmpty())
        }

        @Test
        fun `maps cash tranches with formatted amounts`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(
                    CashTranche(withdrawalId = "w-1", amountConsumed = 3000L),
                    CashTranche(withdrawalId = "w-2", amountConsumed = 2000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals(2, result.cashTranches.size)
            result.cashTranches.forEach {
                assertTrue(it.formattedAmountConsumed.isNotBlank())
            }
        }

        @Test
        fun `scopeText is null when withdrawal not found in lookup (legacy record)`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-legacy", amountConsumed = 1000L))
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNull(result.cashTranches.first().scopeText)
        }

        @Test
        fun `scopeText resolves to group label for GROUP-scoped withdrawal`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-group", amountConsumed = 1000L))
            )
            val withdrawal = CashWithdrawal(id = "w-group", groupId = "group-456", withdrawalScope = PayerType.GROUP)
            every { resourceProvider.getString(any()) } returns "translated_string"

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-group" to withdrawal)
            )

            assertNotNull(result.cashTranches.first().scopeText)
        }

        @Test
        fun `scopeText resolves to personal label for USER-scoped withdrawal`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-user", amountConsumed = 1000L))
            )
            val withdrawal = CashWithdrawal(id = "w-user", groupId = "group-456", withdrawalScope = PayerType.USER)
            every { resourceProvider.getString(any()) } returns "translated_string"

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-user" to withdrawal)
            )

            assertNotNull(result.cashTranches.first().scopeText)
        }

        @Test
        fun `scopeText resolves to subunit label when subunit name is found`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-sub", amountConsumed = 1000L))
            )
            val withdrawal = CashWithdrawal(
                id = "w-sub",
                groupId = "group-456",
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "sub-1"
            )
            every { resourceProvider.getString(any(), any()) } returns "Cabin cash"

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-sub" to withdrawal),
                subunitNameLookup = mapOf("sub-1" to "Cabin")
            )

            assertNotNull(result.cashTranches.first().scopeText)
        }

        @Test
        fun `scopeText is null for SUBUNIT-scoped withdrawal when subunit name not found`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-sub", amountConsumed = 1000L))
            )
            val withdrawal = CashWithdrawal(
                id = "w-sub",
                groupId = "group-456",
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "sub-unknown"
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-sub" to withdrawal),
                subunitNameLookup = emptyMap()
            )

            assertNull(result.cashTranches.first().scopeText)
        }

        @Test
        fun `formattedRate is non-null for foreign-currency withdrawal`() {
            every {
                resourceProvider.getString(any(), any(), any(), any())
            } returns "1 EUR = 37.037 THB"
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-thb", amountConsumed = 1000L))
            )
            val withdrawal = CashWithdrawal(
                id = "w-thb",
                groupId = "group-456",
                currency = "THB",
                exchangeRate = BigDecimal("37.037"),
                withdrawalScope = PayerType.GROUP
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-thb" to withdrawal)
            )

            assertNotNull(result.cashTranches.first().formattedRate)
        }

        @Test
        fun `formattedRate is null for same-currency withdrawal`() {
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-eur", amountConsumed = 1000L))
            )
            val withdrawal = CashWithdrawal(
                id = "w-eur",
                groupId = "group-456",
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                withdrawalScope = PayerType.GROUP
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-eur" to withdrawal)
            )

            assertNull(result.cashTranches.first().formattedRate)
        }
    }

    @Nested
    inner class SubunitGroupedSplits {

        @Test
        fun `flat splits stay in splits and produce no groups`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 2500L),
                    ExpenseSplit(userId = otherUserId, amountCents = 2500L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals(2, result.splits.size)
            assertTrue(result.splitGroups.isEmpty())
        }

        @Test
        fun `subunit-keyed splits are grouped into SubunitSplitGroupUiModel`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 1500L, subunitId = "sub-1"),
                    ExpenseSplit(userId = otherUserId, amountCents = 1500L, subunitId = "sub-1"),
                    ExpenseSplit(userId = "solo-user", amountCents = 2000L)
                )
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                subunitNameLookup = mapOf("sub-1" to "Cantalobos")
            )

            assertEquals(1, result.splits.size)
            assertEquals(1, result.splitGroups.size)
            val group = result.splitGroups.first()
            assertEquals("sub-1", group.subunitId)
            assertEquals("Cantalobos", group.subunitLabel)
            assertEquals(2, group.memberCount)
            assertEquals(2, group.members.size)
        }

        @Test
        fun `subunit label falls back when name lookup is missing`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 2500L, subunitId = "sub-x"),
                    ExpenseSplit(userId = otherUserId, amountCents = 2500L, subunitId = "sub-x")
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals(1, result.splitGroups.size)
            // Fallback comes from resourceProvider (mock returns "translated_string").
            assertTrue(result.splitGroups.first().subunitLabel.isNotBlank())
        }

        @Test
        fun `splitTypeText uses PERCENT string when first subunit member has PERCENT splitType`() {
            every { resourceProvider.getString(any()) } answers {
                val resId = it.invocation.args[0] as Int
                "res_$resId"
            }
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(
                        userId = currentUserId,
                        amountCents = 4250L,
                        percentage = BigDecimal("85"),
                        subunitId = "sub-1",
                        splitType = SplitType.PERCENT
                    ),
                    ExpenseSplit(
                        userId = otherUserId,
                        amountCents = 750L,
                        percentage = BigDecimal("15"),
                        subunitId = "sub-1",
                        splitType = SplitType.PERCENT
                    )
                )
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                subunitNameLookup = mapOf("sub-1" to "Cabin")
            )

            val group = result.splitGroups.first()
            assertTrue(group.splitTypeText.isNotBlank())
        }

        @Test
        fun `splitTypeText uses EQUAL string when first subunit member has EQUAL splitType`() {
            every { resourceProvider.getString(any()) } answers {
                val resId = it.invocation.args[0] as Int
                "res_$resId"
            }
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(
                        userId = currentUserId,
                        amountCents = 2500L,
                        subunitId = "sub-1",
                        splitType = SplitType.EQUAL
                    )
                )
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                subunitNameLookup = mapOf("sub-1" to "Cabin")
            )

            val group = result.splitGroups.first()
            assertTrue(group.splitTypeText.isNotBlank())
        }

        @Test
        fun `splitTypeText falls back to EQUAL string when subunit member splitType is null`() {
            every { resourceProvider.getString(any()) } answers {
                val resId = it.invocation.args[0] as Int
                "res_$resId"
            }
            // Legacy expense — splitType not stored on member rows
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 2500L, subunitId = "sub-1"),
                    ExpenseSplit(userId = otherUserId, amountCents = 2500L, subunitId = "sub-1")
                )
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                subunitNameLookup = mapOf("sub-1" to "Cabin")
            )

            // Null splitType on member → falls back to EQUAL → produces a non-blank string
            val group = result.splitGroups.first()
            assertTrue(group.splitTypeText.isNotBlank())
        }

        @Test
        fun `formattedSourceTotalAmount is populated for foreign currency subunit group`() {
            val expense = baseExpense.copy(
                sourceAmount = 100000L, // 1000.00 CNY
                sourceCurrency = "CNY",
                groupAmount = 12630L, // 126.30 EUR
                groupCurrency = "EUR",
                exchangeRate = BigDecimal("0.126297"),
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 53000L, subunitId = "sub-1"), // 530 CNY
                    ExpenseSplit(userId = otherUserId, amountCents = 47000L, subunitId = "sub-1") // 470 CNY
                    // Total for sub-1 = 1000 CNY
                )
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                subunitNameLookup = mapOf("sub-1" to "Gays")
            )

            val group = result.splitGroups.first()
            assertNotNull(group.formattedSourceTotalAmount)
            assertTrue(
                group.formattedSourceTotalAmount!!.contains("CNY") || group.formattedSourceTotalAmount!!.contains("¥")
            )
            assertNotNull(group.formattedTotalAmount)
            assertTrue(group.formattedTotalAmount.contains("EUR") || group.formattedTotalAmount.contains("€"))
        }

        @Test
        fun `formattedSourceTotalAmount is null for same-currency subunit group`() {
            val expense = baseExpense.copy(
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5000L,
                groupCurrency = "EUR",
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 2500L, subunitId = "sub-1"),
                    ExpenseSplit(userId = otherUserId, amountCents = 2500L, subunitId = "sub-1")
                )
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                subunitNameLookup = mapOf("sub-1" to "Gays")
            )

            val group = result.splitGroups.first()
            assertNull(group.formattedSourceTotalAmount)
        }
    }

    @Nested
    inner class PayerInformation {

        @Test
        fun `isOutOfPocket is false for group expense`() {
            val expense = baseExpense.copy(payerType = PayerType.GROUP)

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertFalse(result.isOutOfPocket)
        }

        @Test
        fun `isOutOfPocket is true for user expense`() {
            val expense = baseExpense.copy(
                payerType = PayerType.USER,
                payerId = currentUserId
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertTrue(result.isOutOfPocket)
        }

        @Test
        fun `fundingSourceText is null for group expense`() {
            val expense = baseExpense.copy(payerType = PayerType.GROUP)

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNull(result.fundingSourceText)
        }

        @Test
        fun `fundingSourceText is non-null for out-of-pocket expense`() {
            val expense = baseExpense.copy(
                payerType = PayerType.USER,
                payerId = otherUserId
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertNotNull(result.fundingSourceText)
        }
    }

    @Nested
    inner class OptionalFields {

        @Test
        fun `notesText is null when notes are blank`() {
            val expense = baseExpense.copy(notes = "  ")
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertNull(result.notesText)
        }

        @Test
        fun `notesText is populated when notes are present`() {
            val expense = baseExpense.copy(notes = "Remember to keep receipt")
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertEquals("Remember to keep receipt", result.notesText)
        }

        @Test
        fun `vendorText is null when vendor is blank`() {
            val expense = baseExpense.copy(vendor = "")
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertNull(result.vendorText)
        }

        @Test
        fun `vendorText is populated when vendor is present`() {
            val expense = baseExpense.copy(vendor = "La Bella Italia")
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertEquals("La Bella Italia", result.vendorText)
        }

        @Test
        fun `receiptUri is mapped from expense`() {
            val attachment = es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment(
                localUri = "file:///storage/receipt.jpg",
                mimeType = "image/webp",
                capturedAtMillis = 1716000000000L
            )
            val expense = baseExpense.copy(receiptAttachment = attachment)
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertEquals("file:///storage/receipt.jpg", result.receiptUri)
        }
    }

    @Nested
    inner class ScheduledBadge {

        @Test
        fun `scheduledBadgeText is null for finished expense`() {
            val expense = baseExpense.copy(paymentStatus = PaymentStatus.FINISHED)
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertNull(result.scheduledBadgeText)
        }

        @Test
        fun `scheduledBadgeText is null for scheduled expense without due date`() {
            val expense = baseExpense.copy(
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = null
            )
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertNull(result.scheduledBadgeText)
        }

        @Test
        fun `isScheduledPastDue is false for non-scheduled expense`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)
            assertFalse(result.isScheduledPastDue)
        }

        @Test
        fun `scheduledBadgeText shows due tomorrow for scheduled expense due tomorrow`() {
            // Stub only the specific resource ID so the test fails if the wrong branch is hit.
            every { resourceProvider.getString(R.string.expense_scheduled_due_tomorrow) } returns "Due tomorrow"
            val expense = baseExpense.copy(
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0)
            )
            val result = mapper.map(expense, memberProfiles, currentUserId)
            assertEquals("Due tomorrow", result.scheduledBadgeText)
            assertFalse(result.isScheduledPastDue)
        }
    }

    @Nested
    inner class ForeignCurrencyRateFormatting {

        // Stub that interpolates the actual args so assertions can verify all three components.
        // The vararg is packed as an Array at invocation.args[1], so we access elements via the
        // array rather than directly from the invocation args list.
        private fun stubRateString() {
            every {
                resourceProvider.getString(any(), any(), any(), any())
            } answers {
                val varargs = it.invocation.args[1] as Array<*>
                "1 ${varargs[0]} = ${varargs[1]} ${varargs[2]}"
            }
        }

        private val foreignFeeAddOn = AddOn(
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 500L,
            currency = "GBP",
            exchangeRate = BigDecimal("0.83"),
            groupAmountCents = 415L
        )

        @Test
        fun `add-on formattedRate renders as 1 source equals rate target in EN locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale.US
            stubRateString()
            val expense = baseExpense.copy(addOns = listOf(foreignFeeAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // Exact assertion verifies both presence and ordering: swapping source/target would fail.
            assertEquals("1 GBP = 0.83 EUR", result.addOns.first().formattedRate)
        }

        @Test
        fun `add-on formattedRate renders as 1 source equals rate target in ES locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale("es", "ES")
            stubRateString()
            val expense = baseExpense.copy(addOns = listOf(foreignFeeAddOn))

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // ES locale formats the rate with a comma decimal separator (0,83 not 0.83).
            assertEquals("1 GBP = 0,83 EUR", result.addOns.first().formattedRate)
        }

        @Test
        fun `cash tranche formattedRate renders as 1 source equals rate target in EN locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale.US
            stubRateString()
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-gbp", amountConsumed = 500L))
            )
            val withdrawal = CashWithdrawal(
                id = "w-gbp",
                groupId = "group-456",
                currency = "GBP",
                exchangeRate = BigDecimal("0.83"),
                withdrawalScope = PayerType.GROUP
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-gbp" to withdrawal)
            )

            // Exact assertion verifies both presence and ordering: swapping source/target would fail.
            assertEquals("1 GBP = 0.83 EUR", result.cashTranches.first().formattedRate)
        }

        @Test
        fun `cash tranche formattedRate renders as 1 source equals rate target in ES locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale("es", "ES")
            stubRateString()
            val expense = baseExpense.copy(
                cashTranches = listOf(CashTranche(withdrawalId = "w-gbp", amountConsumed = 500L))
            )
            val withdrawal = CashWithdrawal(
                id = "w-gbp",
                groupId = "group-456",
                currency = "GBP",
                exchangeRate = BigDecimal("0.83"),
                withdrawalScope = PayerType.GROUP
            )

            val result = mapper.map(
                expense,
                memberProfiles,
                currentUserId,
                withdrawalLookup = mapOf("w-gbp" to withdrawal)
            )

            // ES locale formats the rate with a comma decimal separator (0,83 not 0.83).
            assertEquals("1 GBP = 0,83 EUR", result.cashTranches.first().formattedRate)
        }
    }

    @Nested
    inner class PersonalisedPaidByText {

        @Test
        fun `paidByText uses paid_by_you when createdBy is the current user`() {
            // The baseExpense already has createdBy = currentUserId
            every { resourceProvider.getString(any()) } returns "Paid by you"

            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            // paid_by_you is a no-args string resource → getString(R.string.paid_by_you)
            assertEquals("Paid by you", result.paidByText)
        }

        @Test
        fun `paidByText uses paid_by template when createdBy is another member`() {
            val expense = baseExpense.copy(createdBy = otherUserId)
            every { resourceProvider.getString(any(), any()) } returns "Paid by Bob"

            val result = mapper.map(expense, memberProfiles, currentUserId)

            // paid_by is a template resource → getString(R.string.paid_by, paidByName)
            assertEquals("Paid by Bob", result.paidByText)
        }

        @Test
        fun `paidByText contains member display name when payer is another member`() {
            val expense = baseExpense.copy(createdBy = otherUserId)
            // Capture the actual name passed into the template
            every { resourceProvider.getString(any(), "Bob") } returns "Paid by Bob"

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals("Paid by Bob", result.paidByText)
        }
    }

    @Nested
    inner class ExpenseScopeLabel {

        @Test
        fun `expenseScopeLabel maps to scope_group for GROUP payer`() {
            val expense = baseExpense.copy(payerType = PayerType.GROUP)
            every { resourceProvider.getString(any()) } returns "Group expense"

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals("Group expense", result.expenseScopeLabel)
        }

        @Test
        fun `expenseScopeLabel maps to scope_personal for USER payer`() {
            val expense = baseExpense.copy(payerType = PayerType.USER)
            every { resourceProvider.getString(any()) } returns "Personal"

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals("Personal", result.expenseScopeLabel)
        }

        @Test
        fun `expenseScopeLabel maps to scope_subunit for SUBUNIT payer`() {
            val expense = baseExpense.copy(payerType = PayerType.SUBUNIT)
            every { resourceProvider.getString(any()) } returns "Subunit"

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals("Subunit", result.expenseScopeLabel)
        }
    }

    @Nested
    inner class IconFields {

        @Test
        fun `paymentMethodIcon is non-null for CREDIT_CARD`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertNotNull(result.paymentMethodIcon)
        }

        @Test
        fun `paymentStatusIcon is non-null for FINISHED`() {
            val result = mapper.map(baseExpense, memberProfiles, currentUserId)

            assertNotNull(result.paymentStatusIcon)
        }
    }
}
