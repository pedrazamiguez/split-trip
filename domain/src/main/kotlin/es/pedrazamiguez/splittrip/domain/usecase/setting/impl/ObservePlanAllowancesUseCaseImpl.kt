package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObservePlanAllowancesUseCase
import kotlinx.coroutines.flow.Flow

class ObservePlanAllowancesUseCaseImpl(
    private val featureGateService: FeatureGateService
) : ObservePlanAllowancesUseCase {

    override fun invoke(): Flow<PlanAllowances> = featureGateService.observePlanAllowances()
}
