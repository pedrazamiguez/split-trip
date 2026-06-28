package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptRefund
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.features.expense.R

@StringRes
fun PaymentStatus.toStringRes(): Int = when (this) {
    PaymentStatus.RECEIVED -> R.string.payment_status_received
    PaymentStatus.PENDING -> R.string.payment_status_pending
    PaymentStatus.FINISHED -> R.string.payment_status_finished
    PaymentStatus.SCHEDULED -> R.string.payment_status_scheduled
    PaymentStatus.CANCELLED -> R.string.payment_status_cancelled
    PaymentStatus.REFUNDABLE -> R.string.payment_status_refundable
}

fun PaymentStatus.toIconVector(): ImageVector = when (this) {
    PaymentStatus.FINISHED -> TablerIcons.Outline.CircleCheck
    PaymentStatus.RECEIVED -> TablerIcons.Outline.CircleCheck
    PaymentStatus.PENDING -> TablerIcons.Outline.Clock
    PaymentStatus.SCHEDULED -> TablerIcons.Outline.Calendar
    PaymentStatus.CANCELLED -> TablerIcons.Outline.X
    PaymentStatus.REFUNDABLE -> TablerIcons.Outline.ReceiptRefund
}
