package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.domain.enums.AddOnType

@Immutable
data class AddOnTypeBreakdownUiModel(
    val type: AddOnType,
    val label: String,
    val icon: ImageVector,
    val formattedAmount: String
)
