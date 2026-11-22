package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.AddOnType

data class AddOnTypeSpendingInsight(
    val type: AddOnType,
    val totalAmountCents: Long,
    val itemCount: Int
)
