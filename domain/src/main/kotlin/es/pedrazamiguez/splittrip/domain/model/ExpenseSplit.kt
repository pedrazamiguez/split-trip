package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import java.math.BigDecimal

/**
 * Represents a single user's share of an expense.
 *
 * [amountCents] is the **single source of truth** for the share amount in
 * **source currency** (the currency the payer used). It is NOT in group currency.
 * The balance layer converts splits to group currency at read time via
 * `convertSplitToGroupCurrency(splitAmountCents, sourceAmount, groupAmount)`.
 *
 * When using the PERCENT strategy, [percentage] is stored alongside for reference,
 * but [amountCents] is always the authoritative calculated value (3NF).
 * Percentages should never be back-calculated from amounts at read time.
 *
 * For subunit-member splits, [splitType] records the Level 2 (intra-subunit) strategy
 * chosen for the owning subunit. Solo-member splits keep [splitType] null — their
 * strategy is implied by [Expense.splitType] at the top level.
 */
data class ExpenseSplit(
    val userId: String,
    val amountCents: Long,
    val percentage: BigDecimal? = null,
    val isExcluded: Boolean = false,
    val isCoveredById: String? = null,
    val subunitId: String? = null,
    val splitType: SplitType? = null
)
