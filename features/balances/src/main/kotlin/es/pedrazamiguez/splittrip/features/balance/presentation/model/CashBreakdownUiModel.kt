package es.pedrazamiguez.splittrip.features.balance.presentation.model

/**
 * UI model for a single withdrawal's attributed cash share shown in the cash breakdown sheet.
 *
 * Each instance maps to one [CashWithdrawal], carrying only the member's attributed portion
 * of that withdrawal's remaining balance — not the full withdrawal amount.
 *
 * @param withdrawalLabel Withdrawal title or "ATM — Jan 10" fallback when title is blank.
 * @param dateText Short date string for the withdrawal (e.g., "Jan 10").
 * @param formattedRate Exchange rate label (e.g., "@ 0.027 EUR/THB"). Empty when the
 *                      withdrawal currency matches the group currency.
 * @param formattedNativeRemaining Member's attributed share in the withdrawal's native
 *                                  currency (e.g., "฿ 47,750").
 * @param formattedEquivalent Group-currency equivalent (e.g., "≈ 11.33 €"). Empty when
 *                             the withdrawal currency matches the group currency.
 * @param scopeLabel Pre-formatted scope description ("Group cash (est. share)",
 *                   "Personal cash", or the subunit name).
 * @param isEstimatedShare `true` for GROUP-scoped withdrawals, where the member's share
 *                          is a proportional estimate (1/memberCount), not a fixed claim.
 *                          Used by the UI to render an explanatory disclaimer.
 */
data class CashBreakdownUiModel(
    val withdrawalLabel: String = "",
    val dateText: String = "",
    val formattedRate: String = "",
    val formattedNativeRemaining: String = "",
    val formattedEquivalent: String = "",
    val scopeLabel: String = "",
    val isEstimatedShare: Boolean = false
)
