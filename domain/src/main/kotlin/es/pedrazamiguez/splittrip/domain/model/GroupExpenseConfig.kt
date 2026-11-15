package es.pedrazamiguez.splittrip.domain.model

/**
 * Configuration data needed for adding an expense to a group.
 * Encapsulates all the context the Add Expense screen needs.
 *
 * @param subunits Subunits defined in the group. When non-empty, the split editor
 *                 can offer a "Split by subunit" mode alongside the standard flat split.
 * @param hasManualPocketContributions True when the group has at least one manual pocket
 *                                     contribution (`linkedExpenseId == null && linkedSettlementId == null`)
 *                                     or at least one cash withdrawal.
 */
data class GroupExpenseConfig(
    val group: Group,
    val groupCurrency: Currency,
    val availableCurrencies: List<Currency>,
    val subunits: List<Subunit> = emptyList(),
    val hasManualPocketContributions: Boolean = false
)
