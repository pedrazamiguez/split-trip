package es.pedrazamiguez.splittrip.features.expense.presentation.model

/**
 * Cash tranche row in the "Paid with" section of the expense detail screen.
 *
 * @param withdrawalLabel Display label for the source withdrawal
 *                        (title if available, fallback "ATM"/"Cajero").
 *                        Future: enriched with scope (personal/group/subunit)
 *                        once withdrawal lookup is available at detail-screen time.
 * @param formattedAmountConsumed Amount consumed from this withdrawal, formatted
 *                                with currency symbol (e.g. "฿ 45.00").
 */
data class CashTrancheDetailUiModel(
    val withdrawalLabel: String,
    val formattedAmountConsumed: String
)
