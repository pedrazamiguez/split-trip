package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AddOnBreakdownUiModel(
    val formattedTotalExtras: String,
    val formattedPercentageOfSpending: String? = null,
    val formattedWithdrawalFeesNote: String? = null,
    val formattedDiscountsSavedNote: String? = null,
    val typeBreakdown: ImmutableList<AddOnTypeBreakdownUiModel> = persistentListOf()
)
