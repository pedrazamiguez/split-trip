package es.pedrazamiguez.splittrip.features.balance.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI model representing a single member's financial position within the group.
 * All amounts are pre-formatted by the mapper for direct display.
 *
 * Mirrors the domain [MemberBalance] model's cash/non-cash split:
 * - [formattedPocketBalance]: virtual pocket share (contributed − withdrawn − nonCashSpent)
 * - [formattedCashInHand]: physical cash remaining (withdrawn − cashSpent)
 * - [formattedTotalSpent]: all expenses (cashSpent + nonCashSpent)
 *
 * Per-currency breakdowns enable the expandable card to show multi-currency detail.
 * [cashBreakdown] holds per-withdrawal attribution items for the cash breakdown bottom sheet.
 *
 * [displayName] holds the resolved human-readable name (not a raw userId).
 *
 * [hasNegativeCashInHand] is `true` when the domain `cashInHand` value is negative,
 * which happens when a member's share of cash expenses exceeds their attributed
 * withdrawals (e.g., cross-scope FIFO consumption). The mapper replaces the formatted
 * amount with an em-dash ("—") in this case, and the UI shows an explanatory hint.
 */
data class MemberBalanceUiModel(
    val userId: String = "",
    val displayName: String = "",
    val isCurrentUser: Boolean = false,
    val formattedContributed: String = "",
    val formattedCashInHand: String = "",
    val formattedTotalSpent: String = "",
    val formattedPocketBalance: String = "",
    val formattedTotalBalance: String = "",
    val formattedCashSpent: String = "",
    val formattedNonCashSpent: String = "",
    val isPositiveBalance: Boolean = true,
    val hasNegativeCashInHand: Boolean = false,
    val cashInHandByCurrency: ImmutableList<CurrencyBreakdownUiModel> = persistentListOf(),
    val cashSpentByCurrency: ImmutableList<CurrencyBreakdownUiModel> = persistentListOf(),
    val nonCashSpentByCurrency: ImmutableList<CurrencyBreakdownUiModel> = persistentListOf(),
    val cashBreakdown: ImmutableList<CashBreakdownUiModel> = persistentListOf(),
    val formattedTotalFees: String = ""
)
