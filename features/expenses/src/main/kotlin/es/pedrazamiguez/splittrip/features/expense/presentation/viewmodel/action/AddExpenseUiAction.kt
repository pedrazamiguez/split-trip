package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface AddExpenseUiAction {
    data object None : AddExpenseUiAction
    data class ShowError(val message: UiText) : AddExpenseUiAction
    data class ShowPill(val message: UiText) : AddExpenseUiAction
    data object NavigateBack : AddExpenseUiAction

    /**
     * Emitted when a cash-tranche conflict is detected at save time (i.e. another group
     * member consumed cash between the preview and the user's submit, or a Firestore
     * transaction detected a concurrent modification — Phase 2).
     *
     * The Feature handles this by refreshing the tranche preview with the latest Room
     * data and showing the guided conflict-resolution bottom sheet.
     *
     * @param availableAmountForInput Pre-formatted, locale-aware decimal string for the
     *   source-amount input field (e.g. "30.00" or "30,00"). `null` when the available
     *   amount cannot be determined (e.g. [CashConflictException]).
     * @param availableAmountDisplay Pre-formatted, locale-aware string with currency
     *   symbol for the "Use remaining cash" CTA label in the resolution sheet
     *   (e.g. "€30.00" or "30,00 €"). `null` when [availableAmountForInput] is null.
     */
    data class ShowCashConflictResolution(
        val availableAmountForInput: String?,
        val availableAmountDisplay: String?
    ) : AddExpenseUiAction
}
