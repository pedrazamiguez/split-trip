package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.PlanAllowancesUiState

interface PlanAllowancesUiMapper {
    fun map(
        allowances: PlanAllowances,
        extractionCapability: ExtractionCapability = ExtractionCapability.ON_DEVICE_AI
    ): PlanAllowancesUiState
}
