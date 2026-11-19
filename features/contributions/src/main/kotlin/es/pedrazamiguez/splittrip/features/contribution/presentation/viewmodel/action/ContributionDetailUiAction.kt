package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface ContributionDetailUiAction {
    data class ShowError(val message: UiText) : ContributionDetailUiAction
    data class DeleteSuccess(val message: UiText) : ContributionDetailUiAction
    data class NavigateToExpenseDetail(val expenseId: String) : ContributionDetailUiAction
}
