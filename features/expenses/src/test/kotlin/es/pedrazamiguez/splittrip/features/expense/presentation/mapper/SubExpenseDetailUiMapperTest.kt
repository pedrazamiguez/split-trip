package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.expense.R
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SubExpenseDetailUiMapper")
class SubExpenseDetailUiMapperTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: SubExpenseDetailUiMapper

    private val user1 = User(userId = "user-1", displayName = "Alice", email = "alice@example.com")
    private val memberProfiles = mapOf("user-1" to user1)

    @BeforeEach
    fun setUp() {
        localeProvider = mockk {
            every { getCurrentLocale() } returns Locale.US
        }
        resourceProvider = mockk(relaxed = true) {
            every { getString(R.string.payment_status_finished) } returns "Paid"
            every { getString(R.string.payment_status_scheduled) } returns "Scheduled"
            every { getString(R.string.expense_relative_yesterday) } returns "Yesterday"
            every { getString(R.string.expense_relative_today) } returns "Today"
            every { getString(R.string.expense_relative_tomorrow) } returns "Tomorrow"
            every { getString(R.string.paid_by, any()) } answers {
                val args = secondArg<Array<*>>()
                "Paid by ${args[0]}"
            }
            every { getString(es.pedrazamiguez.splittrip.core.designsystem.R.string.user_pending_fallback) } returns
                "Pending..."
            every {
                getString(es.pedrazamiguez.splittrip.core.designsystem.R.string.self_identification_nominative)
            } returns
                "You"
        }
        userUiMapper = UserUiMapper(resourceProvider)
        mapper = SubExpenseDetailUiMapper(
            localeProvider = localeProvider,
            resourceProvider = resourceProvider,
            userUiMapper = userUiMapper
        )
    }

    @Nested
    @DisplayName("map()")
    inner class MapTests {

        @Test
        fun `maps basic fields and formatted amount correctly`() {
            val subExpense = SubExpense(
                id = "sub-1",
                title = "Down Payment",
                amountCents = 5000L,
                currency = "EUR",
                groupAmountCents = 5000L,
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                paymentStatus = PaymentStatus.FINISHED,
                payerType = PayerType.USER,
                payerId = "user-1",
                notes = "Hotel deposit"
            )

            val uiModel = mapper.map(
                subExpense = subExpense,
                groupCurrency = "EUR",
                memberProfiles = memberProfiles,
                currentUserId = "user-2"
            )

            assertEquals("sub-1", uiModel.id)
            assertEquals("Down Payment", uiModel.title)
            assertEquals("€50.00", uiModel.formattedAmount)
            assertEquals("Paid by Alice", uiModel.payerText)
            assertEquals("Hotel deposit", uiModel.notesText)
            assertEquals("Paid", uiModel.paymentStatusText)
            assertNull(uiModel.badgeText)
            assertNull(uiModel.formattedGroupAmount)
        }

        @Test
        fun `maps foreign currency with group amount`() {
            val subExpense = SubExpense(
                id = "sub-2",
                title = "Museum Ticket",
                amountCents = 1000L,
                currency = "USD",
                groupAmountCents = 920L,
                exchangeRate = BigDecimal("0.92"),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                paymentStatus = PaymentStatus.FINISHED,
                payerType = PayerType.GROUP,
                payerId = null
            )

            val uiModel = mapper.map(
                subExpense = subExpense,
                groupCurrency = "EUR",
                memberProfiles = memberProfiles,
                currentUserId = "user-1"
            )

            assertEquals("$10.00", uiModel.formattedAmount)
            assertEquals("€9.20", uiModel.formattedGroupAmount)
            assertNull(uiModel.payerText)
        }

        @Test
        fun `maps scheduled tranche with due date badge`() {
            val today = LocalDateTime.now()
            val subExpense = SubExpense(
                id = "sub-3",
                title = "Final Balance",
                amountCents = 10000L,
                currency = "EUR",
                groupAmountCents = 10000L,
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = today
            )

            val uiModel = mapper.map(
                subExpense = subExpense,
                groupCurrency = "EUR",
                memberProfiles = memberProfiles,
                currentUserId = "user-1"
            )

            assertEquals(PaymentStatus.SCHEDULED, uiModel.paymentStatus)
            assertEquals("Today", uiModel.badgeText)
            org.junit.jupiter.api.Assertions.assertNotNull(uiModel.badgeIcon)
        }

        @Test
        fun `maps relative due dates - yesterday, tomorrow, past, future`() {
            val yesterday = LocalDateTime.now().minusDays(1)
            val tomorrow = LocalDateTime.now().plusDays(1)
            val past = LocalDateTime.now().minusDays(5)
            val future = LocalDateTime.now().plusDays(5)

            val subYesterday = SubExpense(
                id = "sub-y",
                title = "Y",
                amountCents = 1000L,
                currency = "EUR",
                groupAmountCents = 1000L,
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = yesterday
            )
            val subTomorrow = subYesterday.copy(id = "sub-t", dueDate = tomorrow)
            val subPast = subYesterday.copy(id = "sub-p", dueDate = past)
            val subFuture = subYesterday.copy(id = "sub-f", dueDate = future)

            val modelY = mapper.map(subYesterday, "EUR")
            val modelT = mapper.map(subTomorrow, "EUR")
            val modelP = mapper.map(subPast, "EUR")
            val modelF = mapper.map(subFuture, "EUR")

            assertEquals("Yesterday", modelY.badgeText)
            assertTrue(modelY.isBadgeUrgent)
            assertEquals("Tomorrow", modelT.badgeText)
            org.junit.jupiter.api.Assertions.assertFalse(modelT.isBadgeUrgent)
            assertTrue(modelP.isBadgeUrgent)
            org.junit.jupiter.api.Assertions.assertFalse(modelF.isBadgeUrgent)
        }

        @Test
        fun `maps partial payment status and add-ons effective total`() {
            val addOn = es.pedrazamiguez.splittrip.domain.model.AddOn(
                id = "addon-1",
                type = es.pedrazamiguez.splittrip.domain.enums.AddOnType.SURCHARGE,
                mode = es.pedrazamiguez.splittrip.domain.enums.AddOnMode.ON_TOP,
                valueType = es.pedrazamiguez.splittrip.domain.enums.AddOnValueType.EXACT,
                amountCents = 500L,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 500L,
                paymentMethod = PaymentMethod.CASH
            )
            val opDate = LocalDateTime.of(2026, 8, 15, 10, 0)
            val subExpense = SubExpense(
                id = "sub-partial",
                title = "Partial Tranche",
                amountCents = 2000L,
                currency = "EUR",
                groupAmountCents = 2000L,
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.PARTIAL,
                operationDate = opDate,
                addOns = listOf(addOn)
            )

            val uiModel = mapper.map(subExpense, "EUR")

            assertEquals(PaymentStatus.PARTIAL, uiModel.paymentStatus)
            org.junit.jupiter.api.Assertions.assertNotNull(uiModel.badgeIcon)
            assertTrue(uiModel.hasAddOns)
            assertEquals("€25.00", uiModel.formattedEffectiveTotal)
            assertEquals("15 Aug", uiModel.dateText)
        }

        @Test
        fun `mapList maps entire list of sub-expenses`() {
            val list = listOf(
                SubExpense(
                    id = "s1",
                    title = "First",
                    amountCents = 1000L,
                    currency = "EUR",
                    groupAmountCents = 1000L,
                    exchangeRate = BigDecimal.ONE,
                    paymentMethod = PaymentMethod.CASH,
                    paymentStatus = PaymentStatus.FINISHED
                ),
                SubExpense(
                    id = "s2",
                    title = "Second",
                    amountCents = 2000L,
                    currency = "EUR",
                    groupAmountCents = 2000L,
                    exchangeRate = BigDecimal.ONE,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    paymentStatus = PaymentStatus.FINISHED
                )
            )

            val mapped = mapper.mapList(list, "EUR")
            assertEquals(2, mapped.size)
            assertEquals("First", mapped[0].title)
            assertEquals("Second", mapped[1].title)
        }
    }
}
