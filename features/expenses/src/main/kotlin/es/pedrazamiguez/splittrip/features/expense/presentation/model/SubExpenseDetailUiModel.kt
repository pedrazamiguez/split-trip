package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus

@Immutable
data class SubExpenseDetailUiModel(
    val id: String = "",
    val title: String = "",
    val formattedAmount: String = "",
    val formattedGroupAmount: String? = null,
    val paymentMethodText: String = "",
    val paymentMethodIcon: ImageVector = TablerIcons.Outline.CreditCard,
    val paymentStatus: PaymentStatus = PaymentStatus.FINISHED,
    val paymentStatusText: String = "",
    val paymentStatusIcon: ImageVector = TablerIcons.Outline.CircleCheck,
    val badgeText: String? = null,
    val badgeIcon: ImageVector? = null,
    val isBadgeUrgent: Boolean = false,
    val payerText: String? = null,
    val dateText: String = "",
    val notesText: String? = null,
    val hasAddOns: Boolean = false,
    val formattedEffectiveTotal: String? = null
)
