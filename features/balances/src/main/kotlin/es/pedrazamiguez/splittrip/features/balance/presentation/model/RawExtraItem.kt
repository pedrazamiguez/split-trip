package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.domain.model.AddOn
import java.time.LocalDateTime

internal data class RawExtraItem(
    val parentTitle: String,
    val createdAt: LocalDateTime?,
    val addOn: AddOn,
    val scopeKey: ScopeKey
)
