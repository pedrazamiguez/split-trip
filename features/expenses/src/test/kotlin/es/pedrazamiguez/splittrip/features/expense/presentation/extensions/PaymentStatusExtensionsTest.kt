package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.features.expense.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentStatusExtensionsTest {

    // ── toStringRes ───────────────────────────────────────────────────────

    @Nested
    inner class ToStringRes {

        @Test
        fun `RECEIVED maps to payment_status_received string resource`() {
            assertEquals(R.string.payment_status_received, PaymentStatus.RECEIVED.toStringRes())
        }

        @Test
        fun `PENDING maps to payment_status_pending string resource`() {
            assertEquals(R.string.payment_status_pending, PaymentStatus.PENDING.toStringRes())
        }

        @Test
        fun `FINISHED maps to payment_status_finished string resource`() {
            assertEquals(R.string.payment_status_finished, PaymentStatus.FINISHED.toStringRes())
        }

        @Test
        fun `SCHEDULED maps to payment_status_scheduled string resource`() {
            assertEquals(R.string.payment_status_scheduled, PaymentStatus.SCHEDULED.toStringRes())
        }

        @Test
        fun `CANCELLED maps to payment_status_cancelled string resource`() {
            assertEquals(R.string.payment_status_cancelled, PaymentStatus.CANCELLED.toStringRes())
        }

        @Test
        fun `all statuses map to distinct resource IDs`() {
            val ids = PaymentStatus.entries.map { it.toStringRes() }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    // ── toIconVector ──────────────────────────────────────────────────────

    @Nested
    inner class ToIconVector {

        @Test
        fun `FINISHED maps to CircleCheck icon`() {
            assertEquals(TablerIcons.Outline.CircleCheck, PaymentStatus.FINISHED.toIconVector())
        }

        @Test
        fun `RECEIVED maps to CircleCheck icon`() {
            assertEquals(TablerIcons.Outline.CircleCheck, PaymentStatus.RECEIVED.toIconVector())
        }

        @Test
        fun `PENDING maps to Clock icon`() {
            assertEquals(TablerIcons.Outline.Clock, PaymentStatus.PENDING.toIconVector())
        }

        @Test
        fun `SCHEDULED maps to Calendar icon`() {
            assertEquals(TablerIcons.Outline.Calendar, PaymentStatus.SCHEDULED.toIconVector())
        }

        @Test
        fun `CANCELLED maps to X icon`() {
            assertEquals(TablerIcons.Outline.X, PaymentStatus.CANCELLED.toIconVector())
        }

        @Test
        fun `FINISHED and RECEIVED share the same icon`() {
            assertEquals(PaymentStatus.FINISHED.toIconVector(), PaymentStatus.RECEIVED.toIconVector())
        }

        @Test
        fun `PENDING SCHEDULED CANCELLED all have distinct icons`() {
            val pendingIcon = PaymentStatus.PENDING.toIconVector()
            val scheduledIcon = PaymentStatus.SCHEDULED.toIconVector()
            val cancelledIcon = PaymentStatus.CANCELLED.toIconVector()

            assertNotEquals(pendingIcon, scheduledIcon)
            assertNotEquals(pendingIcon, cancelledIcon)
            assertNotEquals(scheduledIcon, cancelledIcon)
        }
    }
}
