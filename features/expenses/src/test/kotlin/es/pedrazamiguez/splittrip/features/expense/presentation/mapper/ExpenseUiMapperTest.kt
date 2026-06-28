package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ExpenseUiMapper")
class ExpenseUiMapperTest {

    private lateinit var mapper: ExpenseUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk()

        every { localeProvider.getCurrentLocale() } returns Locale.US

        // Stub paid_by pattern — vararg overload packs trailing args into an Array
        every { resourceProvider.getString(R.string.paid_by, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Paid by ${varargs[0]}"
        }

        // Stub expense_paid_by_member pattern (out-of-pocket badge)
        every { resourceProvider.getString(R.string.expense_paid_by_member, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Paid by ${varargs[0]}"
        }

        // Stub scope-aware badge strings
        every { resourceProvider.getString(R.string.expense_paid_by_me) } returns "Paid by me"
        every { resourceProvider.getString(R.string.expense_paid_for_scope, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Paid for ${varargs[0]}"
        }
        every { resourceProvider.getString(R.string.expense_paid_by_member_for_scope, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Paid by ${varargs[0]} for ${varargs[1]}"
        }
        every { resourceProvider.getString(R.string.expense_scope_everyone) } returns "everyone"

        // Stub all payment method string resources
        PaymentMethod.entries.forEach { method ->
            every { resourceProvider.getString(method.toStringRes()) } returns method.name
        }

        // Stub all expense category string resources
        ExpenseCategory.entries.forEach { category ->
            every { resourceProvider.getString(category.toStringRes()) } returns category.name
        }

        // Stub all payment status string resources
        PaymentStatus.entries.forEach { status ->
            every { resourceProvider.getString(status.toStringRes()) } returns status.name
        }

        // Stub scheduled badge strings
        every { resourceProvider.getString(R.string.expense_scheduled_due_today) } returns "Due today"
        every { resourceProvider.getString(R.string.expense_scheduled_due_tomorrow) } returns "Due tomorrow"
        every { resourceProvider.getString(R.string.expense_scheduled_paid) } returns "Paid"
        every { resourceProvider.getString(R.string.expense_scheduled_due_on, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Due on ${varargs[0]}"
        }
        every { resourceProvider.getString(R.string.expense_status_cancelled_refunded) } returns "Cancelled - Refunded"
        every { resourceProvider.getString(R.string.expense_refundable_until, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Refundable until ${varargs[0]}"
        }

        val formattingHelper = FormattingHelper(localeProvider)
        val scheduledBadgeUiMapper = ScheduledBadgeUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider
        )

        mapper = ExpenseUiMapper(
            localeProvider = localeProvider,
            resourceProvider = resourceProvider,
            scheduledBadgeUiMapper = scheduledBadgeUiMapper
        )
    }

    // ---------- formattedOriginalAmount ----------

    @Nested
    @DisplayName("formattedOriginalAmount mapping")
    inner class FormattedOriginalAmount {

        @Test
        fun `is null when sourceCurrency equals groupCurrency`() {
            val expense = Expense(
                id = "e1",
                sourceAmount = 5000,
                sourceCurrency = "EUR",
                groupAmount = 5000,
                groupCurrency = "EUR"
            )

            val result = mapper.map(expense)

            assertNull(result.formattedOriginalAmount)
        }

        @Test
        fun `is non-null when sourceCurrency differs from groupCurrency`() {
            val expense = Expense(
                id = "e2",
                sourceAmount = 9000,
                sourceCurrency = "THB",
                groupAmount = 248,
                groupCurrency = "EUR"
            )

            val result = mapper.map(expense)

            // THB 9000 cents = THB 90.00 formatted in US locale
            assertEquals("฿90.00", result.formattedOriginalAmount)
        }

        @Test
        fun `formats JPY source correctly with 0 decimals`() {
            val expense = Expense(
                id = "e3",
                sourceAmount = 15725,
                sourceCurrency = "JPY",
                groupAmount = 10000,
                groupCurrency = "EUR"
            )

            val result = mapper.map(expense)

            assertEquals("¥15,725", result.formattedOriginalAmount)
        }

        @Test
        fun `formats group amount regardless of currency match`() {
            val expense = Expense(
                id = "e4",
                sourceAmount = 5000,
                sourceCurrency = "USD",
                groupAmount = 4600,
                groupCurrency = "EUR"
            )

            val result = mapper.map(expense)

            // Group amount: 4600 cents EUR = €46.00
            assertEquals("€46.00", result.formattedAmount)
            // Source amount: 5000 cents USD = $50.00
            assertEquals("$50.00", result.formattedOriginalAmount)
        }
    }

    // ---------- paymentMethodText ----------

    @Nested
    @DisplayName("paymentMethodText mapping")
    inner class PaymentMethodText {

        @Test
        fun `resolves CASH payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_cash) } returns "Cash"

            val expense = Expense(id = "e5", paymentMethod = PaymentMethod.CASH)

            val result = mapper.map(expense)

            assertEquals("Cash", result.paymentMethodText)
        }

        @Test
        fun `resolves CREDIT_CARD payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_credit_card) } returns "Credit Card"

            val expense = Expense(id = "e6", paymentMethod = PaymentMethod.CREDIT_CARD)

            val result = mapper.map(expense)

            assertEquals("Credit Card", result.paymentMethodText)
        }

        @Test
        fun `resolves DEBIT_CARD payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_debit_card) } returns "Debit Card"

            val expense = Expense(id = "e7", paymentMethod = PaymentMethod.DEBIT_CARD)

            val result = mapper.map(expense)

            assertEquals("Debit Card", result.paymentMethodText)
        }

        @Test
        fun `resolves OTHER payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_other) } returns "Other"

            val expense = Expense(id = "e8", paymentMethod = PaymentMethod.OTHER)

            val result = mapper.map(expense)

            assertEquals("Other", result.paymentMethodText)
        }

        @Test
        fun `resolves BIZUM payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_bizum) } returns "Bizum"

            val expense = Expense(id = "e-bizum", paymentMethod = PaymentMethod.BIZUM)

            val result = mapper.map(expense)

            assertEquals("Bizum", result.paymentMethodText)
        }

        @Test
        fun `resolves PIX payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_pix) } returns "Pix"

            val expense = Expense(id = "e-pix", paymentMethod = PaymentMethod.PIX)

            val result = mapper.map(expense)

            assertEquals("Pix", result.paymentMethodText)
        }

        @Test
        fun `resolves BANK_TRANSFER payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_bank_transfer) } returns "Bank Transfer"

            val expense = Expense(id = "e-bt", paymentMethod = PaymentMethod.BANK_TRANSFER)

            val result = mapper.map(expense)

            assertEquals("Bank Transfer", result.paymentMethodText)
        }

        @Test
        fun `resolves PAYPAL payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_paypal) } returns "PayPal"

            val expense = Expense(id = "e-pp", paymentMethod = PaymentMethod.PAYPAL)

            val result = mapper.map(expense)

            assertEquals("PayPal", result.paymentMethodText)
        }

        @Test
        fun `resolves VENMO payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_venmo) } returns "Venmo"

            val expense = Expense(id = "e-venmo", paymentMethod = PaymentMethod.VENMO)

            val result = mapper.map(expense)

            assertEquals("Venmo", result.paymentMethodText)
        }

        @Test
        fun `resolves ALIPAY payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_alipay) } returns "Alipay"

            val expense = Expense(id = "e-alipay", paymentMethod = PaymentMethod.ALIPAY)

            val result = mapper.map(expense)

            assertEquals("Alipay", result.paymentMethodText)
        }

        @Test
        fun `resolves WECHAT_PAY payment method text`() {
            every { resourceProvider.getString(R.string.payment_method_wechat_pay) } returns "WeChat Pay"

            val expense = Expense(id = "e-wechat", paymentMethod = PaymentMethod.WECHAT_PAY)

            val result = mapper.map(expense)

            assertEquals("WeChat Pay", result.paymentMethodText)
        }
    }

    // ---------- Other existing fields ----------

    @Nested
    @DisplayName("Basic field mapping")
    inner class BasicFields {

        @Test
        fun `maps id and title`() {
            val expense = Expense(id = "abc-123", title = "Dinner")

            val result = mapper.map(expense)

            assertEquals("abc-123", result.id)
            assertEquals("Dinner", result.title)
        }

        @Test
        fun `maps paidByText using resourceProvider`() {
            val expense = Expense(id = "e9", createdBy = "Alice")

            val result = mapper.map(expense)

            assertEquals("Paid by Alice", result.paidByText)
        }

        @Test
        fun `maps dateText from createdAt`() {
            val expense = Expense(
                id = "e10",
                createdAt = LocalDateTime.of(2025, 1, 15, 12, 30)
            )

            val result = mapper.map(expense)

            // US locale short date: "15 Jan"
            assertEquals("15 Jan", result.dateText)
        }

        @Test
        fun `maps dateText as empty when createdAt is null`() {
            val expense = Expense(id = "e11", createdAt = null)

            val result = mapper.map(expense)

            assertEquals("", result.dateText)
        }

        @Test
        fun `maps isRefundable as true when paymentStatus is REFUNDABLE`() {
            val expense = Expense(id = "e12", paymentStatus = PaymentStatus.REFUNDABLE)

            val result = mapper.map(expense)

            assertTrue(result.isRefundable)
        }

        @Test
        fun `maps isRefundable as false when paymentStatus is not REFUNDABLE`() {
            val expense = Expense(id = "e13", paymentStatus = PaymentStatus.FINISHED)

            val result = mapper.map(expense)

            assertFalse(result.isRefundable)
        }
    }

    // ---------- mapList ----------

    @Nested
    @DisplayName("mapList")
    inner class MapList {

        @Test
        fun `maps a list of expenses preserving order`() {
            val expenses = listOf(
                Expense(id = "1", title = "First"),
                Expense(id = "2", title = "Second"),
                Expense(id = "3", title = "Third")
            )

            val result = mapper.mapList(expenses)

            assertEquals(3, result.size)
            assertEquals("1", result[0].id)
            assertEquals("2", result[1].id)
            assertEquals("3", result[2].id)
        }

        @Test
        fun `returns empty immutable list for empty input`() {
            val result = mapper.mapList(emptyList())

            assertTrue(result.isEmpty())
        }

        @Test
        fun `correctly maps mixed currency expenses in list`() {
            val expenses = listOf(
                Expense(
                    id = "same",
                    sourceCurrency = "EUR",
                    groupCurrency = "EUR"
                ),
                Expense(
                    id = "different",
                    sourceAmount = 9000,
                    sourceCurrency = "THB",
                    groupAmount = 248,
                    groupCurrency = "EUR"
                )
            )

            val result = mapper.mapList(expenses)

            assertNull(result[0].formattedOriginalAmount)
            assertEquals("฿90.00", result[1].formattedOriginalAmount)
        }
    }

    // ---------- Locale-dependent formatting ----------

    @Nested
    @DisplayName("Locale-dependent formatting")
    inner class LocaleFormatting {

        @Test
        fun `formats amounts using Spanish locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")

            val expense = Expense(
                id = "e12",
                sourceAmount = 9000,
                sourceCurrency = "THB",
                groupAmount = 248,
                groupCurrency = "EUR"
            )

            val result = mapper.map(expense)

            // Spanish locale: "2,48 €" (with NBSP)
            assertEquals("2,48\u00A0€", result.formattedAmount)
            // Spanish locale uses Baht symbol: "90,00 ฿" (with NBSP)
            assertEquals("90,00\u00A0฿", result.formattedOriginalAmount)
        }
    }

    // ---------- mapGroupedByDate ----------

    @Nested
    @DisplayName("mapGroupedByDate")
    inner class MapGroupedByDate {

        @Test
        fun `returns empty list for empty input`() {
            val result = mapper.mapGroupedByDate(emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `groups expenses on a single day`() {
            val date = LocalDateTime.of(2025, 3, 15, 10, 0)
            val expenses = listOf(
                Expense(id = "1", title = "Lunch", groupAmount = 2000, groupCurrency = "EUR", createdAt = date),
                Expense(
                    id = "2",
                    title = "Coffee",
                    groupAmount = 500,
                    groupCurrency = "EUR",
                    createdAt = date.plusHours(2)
                )
            )

            val result = mapper.mapGroupedByDate(expenses)

            assertEquals(1, result.size)
            assertEquals(2, result[0].expenses.size)
            // day total = 2000 + 500 = 2500 cents = €25.00
            assertEquals("€25.00", result[0].formattedDayTotal)
        }

        @Test
        fun `groups expenses across multiple days preserving order`() {
            val day1 = LocalDateTime.of(2025, 3, 15, 10, 0)
            val day2 = LocalDateTime.of(2025, 3, 16, 10, 0)
            val expenses = listOf(
                Expense(id = "1", title = "Day1", groupAmount = 1000, groupCurrency = "EUR", createdAt = day1),
                Expense(id = "2", title = "Day2a", groupAmount = 2000, groupCurrency = "EUR", createdAt = day2),
                Expense(
                    id = "3",
                    title = "Day2b",
                    groupAmount = 3000,
                    groupCurrency = "EUR",
                    createdAt = day2.plusHours(1)
                )
            )

            val result = mapper.mapGroupedByDate(expenses)

            assertEquals(2, result.size)
            assertEquals(1, result[0].expenses.size) // day1
            assertEquals(2, result[1].expenses.size) // day2
        }

        @Test
        fun `formats date text for each group`() {
            val date = LocalDateTime.of(2025, 1, 15, 10, 0)
            val expenses = listOf(
                Expense(id = "1", groupAmount = 1000, groupCurrency = "EUR", createdAt = date)
            )

            val result = mapper.mapGroupedByDate(expenses)

            assertEquals("15 Jan", result[0].dateText)
        }

        @Test
        fun `handles null createdAt with empty date text`() {
            val expenses = listOf(
                Expense(id = "1", groupAmount = 1000, groupCurrency = "EUR", createdAt = null)
            )

            val result = mapper.mapGroupedByDate(expenses)

            assertEquals(1, result.size)
            assertEquals("", result[0].dateText)
        }
    }

    // ---------- Scheduled badge ----------

    @Nested
    @DisplayName("Scheduled badge mapping")
    inner class ScheduledBadge {

        @Test
        fun `non-scheduled expense has null badge`() {
            val expense = Expense(
                id = "e1",
                paymentStatus = PaymentStatus.FINISHED
            )

            val result = mapper.map(expense)

            assertNull(result.scheduledBadgeText)
            assertFalse(result.isScheduledPastDue)
        }

        @Test
        fun `scheduled expense without dueDate has null badge`() {
            val expense = Expense(
                id = "e2",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = null
            )

            val result = mapper.map(expense)

            assertNull(result.scheduledBadgeText)
            assertFalse(result.isScheduledPastDue)
        }

        @Test
        fun `scheduled expense with future dueDate shows due on date`() {
            val futureDueDate = LocalDateTime.now().plusDays(10)
            val expense = Expense(
                id = "e3",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDueDate
            )

            val result = mapper.map(expense)

            assertNotNull(result.scheduledBadgeText)
            assertTrue(result.scheduledBadgeText!!.startsWith("Due on"))
            assertFalse(result.isScheduledPastDue)
        }

        @Test
        fun `scheduled expense with past dueDate shows paid`() {
            val pastDueDate = LocalDateTime.now().minusDays(5)
            val expense = Expense(
                id = "e4",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = pastDueDate
            )

            val result = mapper.map(expense)

            assertEquals("Paid", result.scheduledBadgeText)
            assertTrue(result.isScheduledPastDue)
        }

        @Test
        fun `scheduled expense due today shows due today`() {
            // Use a time today but not midnight to ensure toLocalDate() is today
            val todayDueDate = LocalDateTime.now().withHour(12).withMinute(0)
            val expense = Expense(
                id = "e5",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = todayDueDate
            )

            val result = mapper.map(expense)

            assertEquals("Due today", result.scheduledBadgeText)
            assertTrue(result.isScheduledPastDue)
        }

        @Test
        fun `scheduled expense due tomorrow shows due tomorrow`() {
            val tomorrowDueDate = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0)
            val expense = Expense(
                id = "e6",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = tomorrowDueDate
            )

            val result = mapper.map(expense)

            assertEquals("Due tomorrow", result.scheduledBadgeText)
            assertFalse(result.isScheduledPastDue)
        }
    }

    // ---------- Out-of-pocket mapping ----------

    @Nested
    @DisplayName("Out-of-pocket mapping")
    inner class OutOfPocketMapping {

        @Test
        fun `group-funded expense sets isOutOfPocket false and fundingSourceText null`() {
            val expense = Expense(
                id = "oop-1",
                payerType = PayerType.GROUP,
                payerId = null
            )

            val result = mapper.map(expense)

            assertFalse(result.isOutOfPocket)
            assertNull(result.fundingSourceText)
        }

        @Test
        fun `user-funded expense resolves fundingSourceText from memberProfiles`() {
            val profiles = mapOf(
                "uid-1" to User(
                    userId = "uid-1",
                    email = "maria@test.com",
                    displayName = "María"
                )
            )
            val expense = Expense(
                id = "oop-2",
                payerType = PayerType.USER,
                payerId = "uid-1"
            )

            val result = mapper.map(expense, profiles)

            assertTrue(result.isOutOfPocket)
            assertEquals("Paid by María", result.fundingSourceText)
        }

        @Test
        fun `user-funded expense falls back to payerId when profile not found`() {
            val expense = Expense(
                id = "oop-3",
                payerType = PayerType.USER,
                payerId = "uid-unknown"
            )

            val result = mapper.map(expense, emptyMap())

            assertTrue(result.isOutOfPocket)
            assertEquals("Paid by uid-unknown", result.fundingSourceText)
        }

        @Test
        fun `user-funded expense with null payerId falls back to createdBy`() {
            val profiles = mapOf(
                "uid-creator" to User(
                    userId = "uid-creator",
                    email = "creator@test.com",
                    displayName = "Creator"
                )
            )
            val expense = Expense(
                id = "oop-4",
                payerType = PayerType.USER,
                payerId = null,
                createdBy = "uid-creator"
            )

            val result = mapper.map(expense, profiles)

            assertTrue(result.isOutOfPocket)
            assertEquals("Paid by Creator", result.fundingSourceText)
        }

        @Test
        fun `user-funded expense with null payerId and blank createdBy sets fundingSourceText null`() {
            val expense = Expense(
                id = "oop-5",
                payerType = PayerType.USER,
                payerId = null,
                createdBy = ""
            )

            val result = mapper.map(expense)

            assertTrue(result.isOutOfPocket)
            assertNull(result.fundingSourceText)
        }
    }

    // ---------- Scope-aware out-of-pocket badge ----------

    @Nested
    @DisplayName("Scope-aware out-of-pocket badge")
    inner class ScopeAwareBadge {

        private val currentUserId = "uid-current"
        private val otherUserId = "uid-other"

        private val profiles = mapOf(
            currentUserId to User(userId = currentUserId, email = "me@test.com", displayName = "Me"),
            otherUserId to User(userId = otherUserId, email = "other@test.com", displayName = "María")
        )

        private val subunits = mapOf(
            "sub-1" to Subunit(id = "sub-1", name = "Cantalobos", memberIds = listOf(currentUserId, "uid-andres"))
        )

        @Test
        fun `USER scope with current user shows Paid by me`() {
            val expense = oopExpense(currentUserId)
            val contributions = userScopeContribution(expense.id, currentUserId)

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid by me", result.fundingSourceText)
            assertFalse(result.isSubunitScope)
            assertFalse(result.isGroupScope)
        }

        @Test
        fun `USER scope with other user shows Paid by name`() {
            val expense = oopExpense(otherUserId)
            val contributions = userScopeContribution(expense.id, otherUserId)

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid by María", result.fundingSourceText)
            assertFalse(result.isSubunitScope)
            assertFalse(result.isGroupScope)
        }

        @Test
        fun `SUBUNIT scope with current user shows Paid for subunit name`() {
            val expense = oopExpense(currentUserId)
            val contributions = subunitScopeContribution(expense.id, currentUserId, "sub-1")

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid for Cantalobos", result.fundingSourceText)
            assertTrue(result.isSubunitScope)
            assertFalse(result.isGroupScope)
        }

        @Test
        fun `SUBUNIT scope with other user shows Paid by name for subunit`() {
            val expense = oopExpense(otherUserId)
            val contributions = subunitScopeContribution(expense.id, otherUserId, "sub-1")

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid by María for Cantalobos", result.fundingSourceText)
            assertTrue(result.isSubunitScope)
            assertFalse(result.isGroupScope)
        }

        @Test
        fun `GROUP scope with current user shows Paid for everyone`() {
            val expense = oopExpense(currentUserId)
            val contributions = groupScopeContribution(expense.id, currentUserId)

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid for everyone", result.fundingSourceText)
            assertFalse(result.isSubunitScope)
            assertTrue(result.isGroupScope)
        }

        @Test
        fun `GROUP scope with other user shows Paid by name for everyone`() {
            val expense = oopExpense(otherUserId)
            val contributions = groupScopeContribution(expense.id, otherUserId)

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid by María for everyone", result.fundingSourceText)
            assertFalse(result.isSubunitScope)
            assertTrue(result.isGroupScope)
        }

        @Test
        fun `no paired contribution falls back to USER scope`() {
            val expense = oopExpense(otherUserId)

            val result = mapper.map(expense, profiles, currentUserId, emptyMap(), subunits)

            assertEquals("Paid by María", result.fundingSourceText)
            assertFalse(result.isSubunitScope)
            assertFalse(result.isGroupScope)
        }

        @Test
        fun `SUBUNIT scope with unknown subunit falls back to personal scope`() {
            val expense = oopExpense(currentUserId)
            val contributions = subunitScopeContribution(expense.id, currentUserId, "unknown-sub")

            val result = mapper.map(expense, profiles, currentUserId, contributions, subunits)

            assertEquals("Paid by me", result.fundingSourceText)
        }

        @Test
        fun `null currentUserId always uses payer name`() {
            val expense = oopExpense(currentUserId)
            val contributions = userScopeContribution(expense.id, currentUserId)

            val result = mapper.map(expense, profiles, null, contributions, subunits)

            assertEquals("Paid by Me", result.fundingSourceText)
        }

        private fun oopExpense(payerId: String) = Expense(
            id = "oop-scope-${payerId.hashCode()}",
            payerType = PayerType.USER,
            payerId = payerId
        )

        private fun userScopeContribution(expenseId: String, userId: String) = mapOf(
            expenseId to Contribution(
                id = "c-$expenseId",
                userId = userId,
                contributionScope = PayerType.USER,
                linkedExpenseId = expenseId
            )
        )

        private fun subunitScopeContribution(expenseId: String, userId: String, subunitId: String) = mapOf(
            expenseId to Contribution(
                id = "c-$expenseId",
                userId = userId,
                contributionScope = PayerType.SUBUNIT,
                subunitId = subunitId,
                linkedExpenseId = expenseId
            )
        )

        private fun groupScopeContribution(expenseId: String, userId: String) = mapOf(
            expenseId to Contribution(
                id = "c-$expenseId",
                userId = userId,
                contributionScope = PayerType.GROUP,
                linkedExpenseId = expenseId
            )
        )
    }

    // ---------- categoryText ----------

    @Nested
    @DisplayName("categoryText mapping")
    inner class CategoryText {

        @Test
        fun `resolves FOOD category text`() {
            every { resourceProvider.getString(R.string.expense_category_food) } returns "Food"

            val expense = Expense(id = "cat-1", category = ExpenseCategory.FOOD)

            val result = mapper.map(expense)

            assertEquals("Food", result.categoryText)
        }

        @Test
        fun `resolves TRANSPORT category text`() {
            every { resourceProvider.getString(R.string.expense_category_transport) } returns "Transport"

            val expense = Expense(id = "cat-2", category = ExpenseCategory.TRANSPORT)

            val result = mapper.map(expense)

            assertEquals("Transport", result.categoryText)
        }

        @Test
        fun `resolves LODGING category text`() {
            every { resourceProvider.getString(R.string.expense_category_lodging) } returns "Lodging"

            val expense = Expense(id = "cat-3", category = ExpenseCategory.LODGING)

            val result = mapper.map(expense)

            assertEquals("Lodging", result.categoryText)
        }

        @Test
        fun `resolves OTHER category text`() {
            every { resourceProvider.getString(R.string.expense_category_other) } returns "Other"

            val expense = Expense(id = "cat-4", category = ExpenseCategory.OTHER)

            val result = mapper.map(expense)

            assertEquals("Other", result.categoryText)
        }

        @Test
        fun `resolves CONTRIBUTION category text`() {
            every { resourceProvider.getString(R.string.expense_category_contribution) } returns "Contribution"

            val expense = Expense(id = "cat-5", category = ExpenseCategory.CONTRIBUTION)

            val result = mapper.map(expense)

            assertEquals("Contribution", result.categoryText)
        }

        @Test
        fun `resolves REFUND category text`() {
            every { resourceProvider.getString(R.string.expense_category_refund) } returns "Refund"

            val expense = Expense(id = "cat-6", category = ExpenseCategory.REFUND)

            val result = mapper.map(expense)

            assertEquals("Refund", result.categoryText)
        }

        @Test
        fun `resolves ACTIVITIES category text`() {
            every { resourceProvider.getString(R.string.expense_category_activities) } returns "Activities"

            val expense = Expense(id = "cat-7", category = ExpenseCategory.ACTIVITIES)

            val result = mapper.map(expense)

            assertEquals("Activities", result.categoryText)
        }

        @Test
        fun `resolves INSURANCE category text`() {
            every { resourceProvider.getString(R.string.expense_category_insurance) } returns "Insurance"

            val expense = Expense(id = "cat-8", category = ExpenseCategory.INSURANCE)

            val result = mapper.map(expense)

            assertEquals("Insurance", result.categoryText)
        }

        @Test
        fun `resolves ENTERTAINMENT category text`() {
            every { resourceProvider.getString(R.string.expense_category_entertainment) } returns "Entertainment"

            val expense = Expense(id = "cat-9", category = ExpenseCategory.ENTERTAINMENT)

            val result = mapper.map(expense)

            assertEquals("Entertainment", result.categoryText)
        }

        @Test
        fun `resolves SHOPPING category text`() {
            every { resourceProvider.getString(R.string.expense_category_shopping) } returns "Shopping"

            val expense = Expense(id = "cat-10", category = ExpenseCategory.SHOPPING)

            val result = mapper.map(expense)

            assertEquals("Shopping", result.categoryText)
        }
    }

    // ---------- category enum ----------

    @Nested
    @DisplayName("category enum mapping")
    inner class CategoryEnum {

        @Test
        fun `maps FOOD category enum`() {
            val expense = Expense(id = "cenum-1", category = ExpenseCategory.FOOD)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.FOOD, result.category)
        }

        @Test
        fun `maps TRANSPORT category enum`() {
            val expense = Expense(id = "cenum-2", category = ExpenseCategory.TRANSPORT)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.TRANSPORT, result.category)
        }

        @Test
        fun `maps LODGING category enum`() {
            val expense = Expense(id = "cenum-3", category = ExpenseCategory.LODGING)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.LODGING, result.category)
        }

        @Test
        fun `maps ACTIVITIES category enum`() {
            val expense = Expense(id = "cenum-4", category = ExpenseCategory.ACTIVITIES)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.ACTIVITIES, result.category)
        }

        @Test
        fun `maps INSURANCE category enum`() {
            val expense = Expense(id = "cenum-5", category = ExpenseCategory.INSURANCE)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.INSURANCE, result.category)
        }

        @Test
        fun `maps ENTERTAINMENT category enum`() {
            val expense = Expense(id = "cenum-6", category = ExpenseCategory.ENTERTAINMENT)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.ENTERTAINMENT, result.category)
        }

        @Test
        fun `maps SHOPPING category enum`() {
            val expense = Expense(id = "cenum-7", category = ExpenseCategory.SHOPPING)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.SHOPPING, result.category)
        }

        @Test
        fun `maps CONTRIBUTION category enum`() {
            val expense = Expense(id = "cenum-8", category = ExpenseCategory.CONTRIBUTION)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.CONTRIBUTION, result.category)
        }

        @Test
        fun `maps REFUND category enum`() {
            val expense = Expense(id = "cenum-9", category = ExpenseCategory.REFUND)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.REFUND, result.category)
        }

        @Test
        fun `maps OTHER category enum`() {
            val expense = Expense(id = "cenum-10", category = ExpenseCategory.OTHER)

            val result = mapper.map(expense)

            assertEquals(ExpenseCategory.OTHER, result.category)
        }
    }

    // ---------- vendorText ----------

    @Nested
    @DisplayName("vendorText mapping")
    inner class VendorText {

        @Test
        fun `maps non-null vendor`() {
            val expense = Expense(id = "v-1", vendor = "Mercadona")

            val result = mapper.map(expense)

            assertEquals("Mercadona", result.vendorText)
        }

        @Test
        fun `maps null vendor`() {
            val expense = Expense(id = "v-2", vendor = null)

            val result = mapper.map(expense)

            assertNull(result.vendorText)
        }
    }

    // ---------- paymentStatusText ----------

    @Nested
    @DisplayName("paymentStatusText mapping")
    inner class PaymentStatusText {

        @Test
        fun `resolves FINISHED payment status text`() {
            every { resourceProvider.getString(R.string.payment_status_finished) } returns "Finished"

            val expense = Expense(id = "ps-1", paymentStatus = PaymentStatus.FINISHED)

            val result = mapper.map(expense)

            assertEquals("Finished", result.paymentStatusText)
        }

        @Test
        fun `resolves PENDING payment status text`() {
            every { resourceProvider.getString(R.string.payment_status_pending) } returns "Pending"

            val expense = Expense(id = "ps-2", paymentStatus = PaymentStatus.PENDING)

            val result = mapper.map(expense)

            assertEquals("Pending", result.paymentStatusText)
        }

        @Test
        fun `resolves SCHEDULED payment status text`() {
            every { resourceProvider.getString(R.string.payment_status_scheduled) } returns "Scheduled"

            val expense = Expense(id = "ps-3", paymentStatus = PaymentStatus.SCHEDULED)

            val result = mapper.map(expense)

            assertEquals("Scheduled", result.paymentStatusText)
        }

        @Test
        fun `resolves CANCELLED payment status text`() {
            every { resourceProvider.getString(R.string.payment_status_cancelled) } returns "Cancelled"

            val expense = Expense(id = "ps-4", paymentStatus = PaymentStatus.CANCELLED)

            val result = mapper.map(expense)

            assertEquals("Cancelled", result.paymentStatusText)
        }

        @Test
        fun `resolves RECEIVED payment status text`() {
            every { resourceProvider.getString(R.string.payment_status_received) } returns "Received"

            val expense = Expense(id = "ps-5", paymentStatus = PaymentStatus.RECEIVED)

            val result = mapper.map(expense)

            assertEquals("Received", result.paymentStatusText)
        }
    }

    // ---------- hasAddOns ----------

    @Nested
    @DisplayName("hasAddOns mapping")
    inner class HasAddOns {

        @Test
        fun `hasAddOns is false when addOns is empty`() {
            val expense = Expense(id = "ao-1", addOns = emptyList())

            val result = mapper.map(expense)

            assertFalse(result.hasAddOns)
        }

        @Test
        fun `hasAddOns is true when addOns has one item`() {
            val expense = Expense(
                id = "ao-2",
                addOns = listOf(AddOn(id = "addon-1"))
            )

            val result = mapper.map(expense)

            assertTrue(result.hasAddOns)
        }

        @Test
        fun `hasAddOns is true when addOns has multiple items`() {
            val expense = Expense(
                id = "ao-3",
                addOns = listOf(
                    AddOn(id = "addon-1"),
                    AddOn(id = "addon-2")
                )
            )

            val result = mapper.map(expense)

            assertTrue(result.hasAddOns)
        }
    }

    // ---------- resolveDisplayName fallback hierarchy ----------

    @Nested
    @DisplayName("resolveDisplayName fallback hierarchy for paidByText")
    inner class ResolveDisplayName {

        @Test
        fun `uses displayName when available`() {
            val profiles = mapOf(
                "uid-1" to User(
                    userId = "uid-1",
                    email = "alice@test.com",
                    displayName = "Alice"
                )
            )
            val expense = Expense(id = "rdn-1", createdBy = "uid-1")

            val result = mapper.map(expense, profiles)

            assertEquals("Paid by Alice", result.paidByText)
        }

        @Test
        fun `falls back to email when displayName is null`() {
            val profiles = mapOf(
                "uid-2" to User(
                    userId = "uid-2",
                    email = "bob@test.com",
                    displayName = null
                )
            )
            val expense = Expense(id = "rdn-2", createdBy = "uid-2")

            val result = mapper.map(expense, profiles)

            assertEquals("Paid by bob@test.com", result.paidByText)
        }

        @Test
        fun `falls back to email when displayName is blank`() {
            val profiles = mapOf(
                "uid-3" to User(
                    userId = "uid-3",
                    email = "carol@test.com",
                    displayName = "   "
                )
            )
            val expense = Expense(id = "rdn-3", createdBy = "uid-3")

            val result = mapper.map(expense, profiles)

            assertEquals("Paid by carol@test.com", result.paidByText)
        }

        @Test
        fun `falls back to userId when both displayName and email are blank`() {
            val profiles = mapOf(
                "uid-4" to User(
                    userId = "uid-4",
                    email = "",
                    displayName = ""
                )
            )
            val expense = Expense(id = "rdn-4", createdBy = "uid-4")

            val result = mapper.map(expense, profiles)

            assertEquals("Paid by uid-4", result.paidByText)
        }

        @Test
        fun `falls back to raw createdBy when user not in profiles`() {
            val expense = Expense(id = "rdn-5", createdBy = "unknown-user")

            val result = mapper.map(expense, emptyMap())

            assertEquals("Paid by unknown-user", result.paidByText)
        }
    }

    // ---------- mapList with memberProfiles ----------

    @Nested
    @DisplayName("mapList with memberProfiles")
    inner class MapListWithProfiles {

        @Test
        fun `resolves paidByText using memberProfiles`() {
            val profiles = mapOf(
                "uid-1" to User(userId = "uid-1", email = "a@t.com", displayName = "Ana")
            )
            val expenses = listOf(
                Expense(id = "lp-1", createdBy = "uid-1")
            )

            val result = mapper.mapList(expenses, profiles)

            assertEquals("Paid by Ana", result[0].paidByText)
        }

        @Test
        fun `resolves fundingSourceText for out-of-pocket expenses in list`() {
            val profiles = mapOf(
                "uid-1" to User(userId = "uid-1", email = "a@t.com", displayName = "Ana")
            )
            val expenses = listOf(
                Expense(id = "lp-2", payerType = PayerType.USER, payerId = "uid-1"),
                Expense(id = "lp-3", payerType = PayerType.GROUP)
            )

            val result = mapper.mapList(expenses, profiles)

            assertTrue(result[0].isOutOfPocket)
            assertEquals("Paid by Ana", result[0].fundingSourceText)
            assertFalse(result[1].isOutOfPocket)
            assertNull(result[1].fundingSourceText)
        }
    }

    // ---------- mapGroupedByDate with memberProfiles ----------

    @Nested
    @DisplayName("mapGroupedByDate with memberProfiles")
    inner class MapGroupedByDateWithProfiles {

        @Test
        fun `resolves paidByText using memberProfiles in grouped output`() {
            val profiles = mapOf(
                "uid-1" to User(userId = "uid-1", email = "b@t.com", displayName = "Bruno")
            )
            val date = LocalDateTime.of(2025, 3, 15, 10, 0)
            val expenses = listOf(
                Expense(
                    id = "gdp-1",
                    createdBy = "uid-1",
                    groupAmount = 1000,
                    groupCurrency = "EUR",
                    createdAt = date
                )
            )

            val result = mapper.mapGroupedByDate(expenses, profiles)

            assertEquals("Paid by Bruno", result[0].expenses[0].paidByText)
        }
    }

    // ---------- formattedAmount edge cases ----------

    @Nested
    @DisplayName("formattedAmount edge cases")
    inner class FormattedAmountEdgeCases {

        @Test
        fun `formats zero amount`() {
            val expense = Expense(
                id = "fa-1",
                groupAmount = 0,
                groupCurrency = "EUR"
            )

            val result = mapper.map(expense)

            assertEquals("€0.00", result.formattedAmount)
        }

        @Test
        fun `formats large amount with thousand separators`() {
            val expense = Expense(
                id = "fa-2",
                groupAmount = 1_234_567,
                groupCurrency = "USD"
            )

            val result = mapper.map(expense)

            assertEquals("${'$'}12,345.67", result.formattedAmount)
        }
    }

    @Nested
    @DisplayName("SyncStatus mapping")
    inner class SyncStatusMapping {

        @Test
        fun `maps SYNCED status`() {
            val expense = Expense(id = "ss-1", syncStatus = SyncStatus.SYNCED)
            assertEquals(SyncStatus.SYNCED, mapper.map(expense).syncStatus)
        }

        @Test
        fun `maps PENDING_SYNC status`() {
            val expense = Expense(id = "ss-2", syncStatus = SyncStatus.PENDING_SYNC)
            assertEquals(SyncStatus.PENDING_SYNC, mapper.map(expense).syncStatus)
        }

        @Test
        fun `maps SYNC_FAILED status`() {
            val expense = Expense(id = "ss-3", syncStatus = SyncStatus.SYNC_FAILED)
            assertEquals(SyncStatus.SYNC_FAILED, mapper.map(expense).syncStatus)
        }
    }
}
