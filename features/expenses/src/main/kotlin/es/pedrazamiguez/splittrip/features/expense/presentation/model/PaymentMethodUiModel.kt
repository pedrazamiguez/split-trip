package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard

data class PaymentMethodUiModel(
    val id: String,
    val displayText: String,
    val icon: ImageVector = TablerIcons.Outline.CreditCard
)
