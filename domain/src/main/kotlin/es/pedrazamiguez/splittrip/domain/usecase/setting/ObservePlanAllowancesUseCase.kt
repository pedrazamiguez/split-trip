package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import kotlinx.coroutines.flow.Flow

interface ObservePlanAllowancesUseCase {
    operator fun invoke(): Flow<PlanAllowances>
}
