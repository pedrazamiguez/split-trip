package es.pedrazamiguez.splittrip.features.balance.presentation.model

/**
 * UI model representing an individual row within a scope group in a metric breakdown.
 *
 * For extras: [title] is the localized add-on type name ("Fees", "Surcharges", "Tips"), [subtitle] is null.
 * For scheduled/refundable: [title] is the expense title, [subtitle] is the formatted due date or expense date.
 */
data class MetricBreakdownItemUiModel(
    val title: String = "",
    val subtitle: String? = null,
    val formattedAmount: String = ""
)
