package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI model representing a scope section (Group, Subunit, or User) containing breakdown items
 * and a formatted subtotal.
 */
data class MetricScopeBreakdownUiModel(
    val scopeLabel: String = "",
    val scopeType: PayerType = PayerType.GROUP,
    val formattedSubtotal: String = "",
    val items: ImmutableList<MetricBreakdownItemUiModel> = persistentListOf()
)
