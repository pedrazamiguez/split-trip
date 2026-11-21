package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class EntitySpendingInsightUiModel(
    val id: String,
    val displayName: String,
    val subtitle: String?,
    val isSubunit: Boolean,
    val isCurrentUser: Boolean,
    val formattedAmount: String,
    val formattedPercentage: String,
    val progress: Float,
    val color: Color
)
