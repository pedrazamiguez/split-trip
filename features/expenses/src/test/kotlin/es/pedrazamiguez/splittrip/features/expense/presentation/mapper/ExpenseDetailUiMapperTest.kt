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
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
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
        val expenseCalculatorService = ExpenseCalculatorService()
        val addOnCalculationService = AddOnCalculationService()

        mapper = ExpenseDetailUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider,
            expenseCalculatorService = expenseCalculatorService,
            addOnCalculationService = addOnCalculationService
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
            assertEquals("Alice", currentUserSplit?.displayName)
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
        fun `resolves display name from member profiles`() {
            val expense = baseExpense.copy(
                splits = listOf(
                    ExpenseSplit(userId = currentUserId, amountCents = 5000L)
                )
            )

            val result = mapper.map(expense, memberProfiles, currentUserId)

            assertEquals("Alice", result.splits.first().displayName)
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
            val expense = baseExpense.copy(receiptLocalUri = "file:///storage/receipt.jpg")
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
    }
}
