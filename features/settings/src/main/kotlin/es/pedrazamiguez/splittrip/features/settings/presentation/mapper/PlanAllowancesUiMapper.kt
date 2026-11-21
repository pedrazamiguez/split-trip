package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.PlanAllowancesUiState

interface PlanAllowancesUiMapper {
    fun map(allowances: PlanAllowances): PlanAllowancesUiState
}
