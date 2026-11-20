package es.pedrazamiguez.splittrip.features.balance.presentation.model

internal data class SecondaryBalanceMetricItem(
    val label: String,
    val formattedAmount: String,
    val metricType: BalanceMetricType
)
