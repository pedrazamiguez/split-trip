package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.expense.R
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ScheduledBadgeUiMapper")
class ScheduledBadgeUiMapperTest {

    private lateinit var mapper: ScheduledBadgeUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk()

        every { localeProvider.getCurrentLocale() } returns Locale.US

        every { resourceProvider.getString(R.string.expense_scheduled_paid) } returns "Paid"
        every { resourceProvider.getString(R.string.expense_scheduled_due_today) } returns "Due today"
        every { resourceProvider.getString(R.string.expense_scheduled_due_tomorrow) } returns "Due tomorrow"
        every { resourceProvider.getString(R.string.expense_scheduled_due_on, *anyVararg()) } answers {
            val varargs = it.invocation.args[1] as Array<*>
            "Due on ${varargs[0]}"
        }

        mapper = ScheduledBadgeUiMapper(
            formattingHelper = FormattingHelper(localeProvider),
            resourceProvider = resourceProvider
        )
    }

    @Nested
    @DisplayName("Non-SCHEDULED status")
    inner class NonScheduledStatus {

        @Test
        fun `returns null badge and isPastDue false for FINISHED expense`() {
            val expense = Expense(id = "e1", paymentStatus = PaymentStatus.FINISHED)
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertNull(badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns null badge and isPastDue false for PENDING expense`() {
            val expense = Expense(id = "e2", paymentStatus = PaymentStatus.PENDING)
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertNull(badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns null badge and isPastDue false for CANCELLED expense`() {
            val expense = Expense(id = "e3", paymentStatus = PaymentStatus.CANCELLED)
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertNull(badge)
            assertFalse(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED with null dueDate")
    inner class NullDueDate {

        @Test
        fun `returns null badge and isPastDue false when dueDate is null`() {
            val expense = Expense(id = "e4", paymentStatus = PaymentStatus.SCHEDULED, dueDate = null)
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertNull(badge)
            assertFalse(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — past due (before today)")
    inner class PastDue {

        @Test
        fun `returns Paid badge and isPastDue true for expense due 5 days ago`() {
            val expense = Expense(
                id = "e5",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().minusDays(5)
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Paid", badge)
            assertTrue(isPastDue)
        }

        @Test
        fun `returns Paid badge in ES locale for past due expense`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every { resourceProvider.getString(R.string.expense_scheduled_paid) } returns "Pagado"

            val expense = Expense(
                id = "e6",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().minusDays(1)
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Pagado", badge)
            assertTrue(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — due today")
    inner class DueToday {

        @Test
        fun `returns Due today badge and isPastDue true`() {
            val expense = Expense(
                id = "e7",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().withHour(12).withMinute(0)
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Due today", badge)
            assertTrue(isPastDue)
        }

        @Test
        fun `returns ES badge for due today`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every { resourceProvider.getString(R.string.expense_scheduled_due_today) } returns "Vence hoy"

            val expense = Expense(
                id = "e8",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().withHour(10).withMinute(0)
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Vence hoy", badge)
            assertTrue(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — due tomorrow")
    inner class DueTomorrow {

        @Test
        fun `returns Due tomorrow badge and isPastDue false`() {
            val expense = Expense(
                id = "e9",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0)
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Due tomorrow", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns ES badge for due tomorrow`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every { resourceProvider.getString(R.string.expense_scheduled_due_tomorrow) } returns "Vence mañana"

            val expense = Expense(
                id = "e10",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Vence mañana", badge)
            assertFalse(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — future (beyond tomorrow)")
    inner class FutureDate {

        @Test
        fun `returns Due on date badge and isPastDue false for EN locale`() {
            val futureDate = LocalDateTime.now().plusDays(10).withHour(12).withMinute(0)
            val expense = Expense(
                id = "e11",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDate
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertTrue(badge!!.startsWith("Due on"))
            assertFalse(isPastDue)
        }

        @Test
        fun `returns Due on date badge with formatted date in ES locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every {
                resourceProvider.getString(R.string.expense_scheduled_due_on, *anyVararg())
            } answers {
                val varargs = it.invocation.args[1] as Array<*>
                "Vence el ${varargs[0]}"
            }

            // Use a known date so we can assert the formatted output
            val futureDate = LocalDateTime.of(2027, 8, 20, 12, 0)
            val expense = Expense(
                id = "e12",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDate
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            // ES locale formats "20 ago" (short month in Spanish)
            assertEquals("Vence el 20 ago", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns Due on date badge with formatted date in EN locale`() {
            val futureDate = LocalDateTime.of(2027, 8, 20, 12, 0)
            val expense = Expense(
                id = "e13",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDate
            )
            val (badge, isPastDue) = mapper.buildBadge(expense)
            assertEquals("Due on 20 Aug", badge)
            assertFalse(isPastDue)
        }
    }
}
