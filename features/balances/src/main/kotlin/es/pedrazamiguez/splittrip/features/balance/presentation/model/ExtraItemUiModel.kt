package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import java.time.LocalDateTime

/**
 * UI model representing an individual extra charge row in the Extras breakdown sheet.
 * Pre-formatted by the mapper for direct display.
 */
data class ExtraItemUiModel(
    val parentTitle: String = "",
    val dateText: String = "",
    val description: String? = null,
    val formattedAmount: String = "",
    // e.g., "Group", "Ana's Family", "You", "Andrés"
    val scopeLabel: String = "",
    val scopeType: PayerType = PayerType.GROUP
)

internal data class RawExtraItem(
    val parentTitle: String,
    val createdAt: LocalDateTime?,
    val addOn: AddOn,
    val scopeLabel: String,
    val scopeType: PayerType
)
