package es.pedrazamiguez.splittrip.features.expense.presentation.model

data class AddOnCurrencyContext(
    val isForeign: Boolean,
    val isCash: Boolean,
    val shouldLockRate: Boolean
)
