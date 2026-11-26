package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveLocalAiModelPackStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveLocalAiModelPackStateUseCaseImpl(
    private val localAiModelPackService: LocalAiModelPackService,
    private val featureGateService: FeatureGateService,
    private val aiModelResolverService: AiModelResolverService
) : ObserveLocalAiModelPackStateUseCase {

    override fun invoke(): Flow<LocalAiModelPackState> = combine(
        localAiModelPackService.observeStatus(),
        featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK),
        localAiModelPackService.observeWifiOnlyPreference(),
        aiModelResolverService.getActiveModel()
    ) { status, isEntitled, isWifiOnly, activeEngine ->
        LocalAiModelPackState(
            status = status,
            compatibility = localAiModelPackService.checkDeviceCompatibility(),
            isEntitled = isEntitled,
            isWifiOnly = isWifiOnly,
            isAiCoreSupported = aiModelResolverService.isAiCoreSupported(),
            activeEngine = activeEngine
        )
    }
}
