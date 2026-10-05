package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
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

@DisplayName("PaymentStatusBadgeUiMapper")
class PaymentStatusBadgeUiMapperTest {

    private lateinit var mapper: PaymentStatusBadgeUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk()

        every { localeProvider.getCurrentLocale() } returns Locale.US

        every { resourceProvider.getString(R.string.expense_relative_yesterday) } returns "Paid"
        every { resourceProvider.getString(R.string.expense_relative_today) } returns "Today"
        every { resourceProvider.getString(R.string.expense_relative_tomorrow) } returns "Tomorrow"
        every { resourceProvider.getString(R.string.expense_status_cancelled_refunded) } returns "Cancelled - Refunded"

        mapper = PaymentStatusBadgeUiMapper(
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
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertNull(badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns null badge and isPastDue false for PENDING expense`() {
            val expense = Expense(id = "e2", paymentStatus = PaymentStatus.PENDING)
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertNull(badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns null badge and isPastDue false for CANCELLED expense`() {
            val expense = Expense(id = "e3", paymentStatus = PaymentStatus.CANCELLED)
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
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
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
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
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals(LocalDateTime.now().minusDays(5).formatShortDate(Locale.US), badge)
            assertTrue(isPastDue)
        }

        @Test
        fun `returns Paid badge in ES locale for past due expense`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every { resourceProvider.getString(R.string.expense_relative_yesterday) } returns "Pagado"

            val expense = Expense(
                id = "e6",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().minusDays(1)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Pagado", badge)
            assertTrue(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — due today")
    inner class DueToday {

        @Test
        fun `returns badge with Today and isPastDue true`() {
            val expense = Expense(
                id = "e7",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().withHour(12).withMinute(0)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Today", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns ES badge for due today`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every { resourceProvider.getString(R.string.expense_relative_today) } returns "Hoy"

            val expense = Expense(
                id = "e8",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().withHour(10).withMinute(0)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Hoy", badge)
            assertFalse(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — due tomorrow")
    inner class DueTomorrow {

        @Test
        fun `returns Tomorrow badge and isPastDue false`() {
            val expense = Expense(
                id = "e9",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Tomorrow", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns ES badge for due tomorrow`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            every { resourceProvider.getString(R.string.expense_relative_tomorrow) } returns "Mañana"

            val expense = Expense(
                id = "e10",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Mañana", badge)
            assertFalse(isPastDue)
        }
    }

    @Nested
    @DisplayName("SCHEDULED — future (beyond tomorrow)")
    inner class FutureDate {

        @Test
        fun `returns formatted date badge for future date in EN locale`() {
            val futureDate = LocalDateTime.now().plusDays(10).withHour(12).withMinute(0)
            val expense = Expense(
                id = "e11",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDate
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals(futureDate.formatShortDate(Locale.US), badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns formatted date badge for future date in ES locale`() {
            val esLocale = Locale.forLanguageTag("es-ES")
            every { localeProvider.getCurrentLocale() } returns esLocale

            val futureDate = LocalDateTime.of(2027, 8, 20, 12, 0)
            val expense = Expense(
                id = "e12",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDate
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals(futureDate.formatShortDate(esLocale), badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns formatted date badge for known date in EN locale`() {
            val futureDate = LocalDateTime.of(2027, 8, 20, 12, 0)
            val expense = Expense(
                id = "e13",
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = futureDate
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("20 Aug 2027", badge)
            assertFalse(isPastDue)
        }
    }

    @Nested
    @DisplayName("REFUNDABLE status")
    inner class RefundableStatus {

        @Test
        fun `returns formatted date badge for future date`() {
            val futureDate = LocalDateTime.of(2027, 8, 20, 12, 0)
            val expense = Expense(
                id = "e14",
                paymentStatus = PaymentStatus.REFUNDABLE,
                dueDate = futureDate
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("20 Aug 2027", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns badge with Today when due today`() {
            val expense = Expense(
                id = "e14b",
                paymentStatus = PaymentStatus.REFUNDABLE,
                dueDate = LocalDateTime.now().withHour(12).withMinute(0)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Today", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns badge with Tomorrow when due tomorrow`() {
            val expense = Expense(
                id = "e14c",
                paymentStatus = PaymentStatus.REFUNDABLE,
                dueDate = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Tomorrow", badge)
            assertFalse(isPastDue)
        }

        @Test
        fun `returns yesterday badge when due date was yesterday`() {
            val expense = Expense(
                id = "e14d",
                paymentStatus = PaymentStatus.REFUNDABLE,
                dueDate = LocalDateTime.now().minusDays(1)
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertEquals("Paid", badge)
            assertTrue(isPastDue)
        }

        @Test
        fun `returns null badge when dueDate is null`() {
            val expense = Expense(
                id = "e15",
                paymentStatus = PaymentStatus.REFUNDABLE,
                dueDate = null
            )
            val badgeData = mapper.buildBadge(expense)
            val badge = badgeData?.text
            val isPastDue = badgeData?.isPassed ?: false
            assertNull(badge)
            assertFalse(isPastDue)
        }
    }
}
