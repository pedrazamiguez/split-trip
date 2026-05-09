package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User

/**
 * Sealed interface representing cross-handler actions that [ConfigEventHandler]
 * needs to trigger after loading group configuration.
 *
 * Instead of directly injecting sibling handlers (which creates tight coupling),
 * [ConfigEventHandler] emits these actions via a callback, and the ViewModel
 * routes them to the appropriate handler.
 */
sealed interface PostConfigAction {

    /** Fetch the API exchange rate for a foreign currency. */
    data object FetchRate : PostConfigAction

    /**
     * Initiate pool discovery and fetch the blended CASH exchange rate from ATM withdrawals.
     *
     * Despite the name, the ViewModel routes this to [CurrencyEventHandler.fetchPoolsIfNeeded],
     * which first queries available withdrawal pools via [WithdrawalPoolSelectionDelegate]. The
     * cash rate preview is then fetched automatically via the delegate's [onPoolResolved] callback
     * once a pool is resolved (auto-selected or pre-selected from the priority list).
     */
    data object FetchCashRate : PostConfigAction

    /** Initialize entity splits when the group has subunits. */
    data class InitEntitySplits(
        val memberIds: List<String>,
        val subunits: List<Subunit>,
        val memberProfiles: Map<String, User>
    ) : PostConfigAction

    /** Clear stale subunit state when the group has no subunits. */
    data object ClearEntitySplits : PostConfigAction
}
