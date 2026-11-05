package es.pedrazamiguez.splittrip.features.balance.presentation.model

internal data class HeldExpenseEntry(
    val scopeKey: ScopeKey,
    val title: String,
    val dateText: String?,
    val effectiveAmount: Long
)
