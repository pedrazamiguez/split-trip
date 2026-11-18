package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface AddCashWithdrawalUiAction {
    data object None : AddCashWithdrawalUiAction
    data class ShowError(val message: UiText) : AddCashWithdrawalUiAction
    data class ShowPill(val message: UiText) : AddCashWithdrawalUiAction

    /** Signals the Feature to pop the back stack (Back pressed on the first wizard step). */
    data object NavigateBack : AddCashWithdrawalUiAction

    /** Fired when the user requests exit while the form has unsaved changes. */
    data object RequestExitConfirmation : AddCashWithdrawalUiAction
}
