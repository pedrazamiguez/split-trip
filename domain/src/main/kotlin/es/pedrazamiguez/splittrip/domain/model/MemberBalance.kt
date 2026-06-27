package es.pedrazamiguez.splittrip.domain.model

/**
 * Represents a single group member's financial position within the shared pocket.
 *
 * All monetary values are stored in minor units (cents) to avoid
 * floating-point precision issues, consistent with the Expense model.
 *
 * Attribution accounts for subunit composition:
 * - Contributions made on behalf of a subunit are distributed by [Subunit.memberShares].
 * - Cash withdrawals are attributed based on their [CashWithdrawal.withdrawalScope].
 * - Expense splits are already per-user (expanded by SubunitAwareSplitService at save time).
 *
 * The model mirrors the group-level financial model exactly:
 * - **Cash expenses** reduce physical cash in hand (funded from ATM withdrawals).
 * - **Non-cash expenses** (card, Bizum, etc.) reduce the virtual pocket balance.
 *
 * @param userId The member's unique identifier.
 * @param contributed How much this member effectively contributed (in cents),
 *                    including their proportional share of subunit contributions.
 * @param withdrawn How much this member effectively withdrew (in cents),
 *                  attributed by withdrawal scope (GROUP/SUBUNIT/USER).
 * @param cashSpent How much this member spent on CASH expenses (in cents),
 *                  converted to group currency. Funded from physical cash (withdrawals).
 * @param nonCashSpent How much this member spent on non-CASH expenses (in cents),
 *                     converted to group currency. Deducted from the virtual pocket.
 * @param totalSpent Total spent across all payment methods: cashSpent + nonCashSpent.
 * @param pocketBalance The member's virtual pocket share: contributed − withdrawn − nonCashSpent.
 *                      Sums across all members should equal the group pocket virtualBalance.
 *                      Positive = has funds in the pocket, negative = overdrew from the pocket.
 * @param cashInHand Physical cash remaining for this member: rawWithdrawn − cashSpent.
 *                   Uses the raw deducted amount (excluding ATM fee add-ons) because
 *                   ATM fees reduce the virtual pocket but do not produce physical cash.
 *                   Sums across all members should equal the group's total cash in hand.
 * @param cashInHandByCurrency Per-currency breakdown of physical cash remaining.
 *                             Native amounts with group-currency equivalents.
 * @param cashSpentByCurrency Per-currency breakdown of cash expenses.
 * @param nonCashSpentByCurrency Per-currency breakdown of non-cash expenses.
 */
data class MemberBalance(
    val userId: String = "",
    val contributed: Long = 0,
    val withdrawn: Long = 0,
    val cashSpent: Long = 0,
    val nonCashSpent: Long = 0,
    val totalSpent: Long = 0,
    val pocketBalance: Long = 0,
    val cashInHand: Long = 0,
    val cashInHandByCurrency: List<CurrencyAmount> = emptyList(),
    val cashSpentByCurrency: List<CurrencyAmount> = emptyList(),
    val nonCashSpentByCurrency: List<CurrencyAmount> = emptyList()
) {
    val totalBalance: Long get() = pocketBalance + cashInHand
}
