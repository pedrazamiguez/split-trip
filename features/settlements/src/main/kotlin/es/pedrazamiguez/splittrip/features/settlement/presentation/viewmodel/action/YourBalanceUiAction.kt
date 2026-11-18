package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface YourBalanceUiAction {
    data class ShowSuccess(val message: UiText) : YourBalanceUiAction
    data class ShowError(val message: UiText) : YourBalanceUiAction
    data class NavigateToDisputeSettlement(
        val groupId: String,
        val settlementId: String
    ) : YourBalanceUiAction
}
