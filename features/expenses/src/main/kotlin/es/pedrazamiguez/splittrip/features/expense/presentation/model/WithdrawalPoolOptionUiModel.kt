package es.pedrazamiguez.splittrip.features.expense.presentation.model

import es.pedrazamiguez.splittrip.domain.enums.PayerType

/**
 * UI model representing a selectable cash withdrawal pool in the pool-selection widget.
 *
 * Shown in the Exchange Rate step when multiple pools (e.g. "Your personal cash" + "Group cash")
 * hold available funds for the current expense's currency, allowing the user to explicitly
 * choose which pool funds the expense instead of the default FIFO priority.
 *
 * @param scope        The scope of the pool (GROUP, USER, or SUBUNIT).
 * @param ownerId      The userId for USER-scoped pools, the subunitId for SUBUNIT-scoped pools,
 *                     or null for GROUP-scoped pools.
 * @param displayLabel Human-readable label shown in the selection UI
 *                     (e.g. "Your personal cash", "Group cash", or the subunit name).
 */
data class WithdrawalPoolOptionUiModel(
    val scope: PayerType,
    val ownerId: String?,
    val displayLabel: String
)
