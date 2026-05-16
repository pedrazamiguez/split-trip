package es.pedrazamiguez.splittrip.features.expense.presentation.model

/**
 * Cash tranche row in the "Paid with" section of the expense detail screen.
 *
 * @param withdrawalLabel Display label for the source withdrawal
 *                        (title if available, fallback "ATM"/"Cajero").
 * @param formattedAmountConsumed Amount consumed from this withdrawal, formatted
 *                                with currency symbol (e.g. "฿ 45.00").
 * @param scopeText Secondary caption identifying the pool scope (e.g. "Group cash",
 *                  "Personal cash", "Cabin cash"). Null for legacy tranche records
 *                  where the withdrawal can't be resolved — hidden in the UI.
 */
data class CashTrancheDetailUiModel(
    val withdrawalLabel: String,
    val formattedAmountConsumed: String,
    val scopeText: String? = null,
    /**
     * Locked exchange rate captured on the source withdrawal, formatted as
     * "1 X = Y Z" for display. Null for legacy tranches with no resolved
     * withdrawal or same-currency withdrawals.
     */
    val formattedRate: String? = null
)
