package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

sealed interface PlanAllowancesUiAction {
    data object NavigateToUpgrade : PlanAllowancesUiAction
}
