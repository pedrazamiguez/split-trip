package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.PayerType

internal data class ScopeKey(
    val label: String,
    val type: PayerType
)
