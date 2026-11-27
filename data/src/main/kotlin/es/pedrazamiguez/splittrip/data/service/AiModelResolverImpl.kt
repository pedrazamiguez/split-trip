package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class AiModelResolverImpl(
    private val userPreferences: UserPreferences,
    private val localAiModelPackService: LocalAiModelPackService,
    private val featureGateService: FeatureGateService,
    private val aiCoreCapabilityProvider: AICoreCapabilityProvider
) : AiModelResolverService {

    override fun getActiveModel(): Flow<AiEngineType> = combine(
        userPreferences.activeAiModel,
        localAiModelPackService.observeStatus(),
        featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK)
    ) { overrideName, packStatus, isEntitled ->
        val overrideModel = overrideName?.let {
            runCatching { AiEngineType.valueOf(it) }.getOrNull()
        }
        overrideModel ?: resolveAutomatic(packStatus = packStatus, isEntitled = isEntitled)
    }

    override fun getDeveloperOverrideModel(): Flow<AiEngineType?> {
        return userPreferences.activeAiModel.map { overrideName ->
            overrideName?.let {
                runCatching { AiEngineType.valueOf(it) }.getOrNull()
            }
        }
    }

    override suspend fun setDeveloperOverrideModel(model: AiEngineType?) {
        userPreferences.setActiveAiModel(model?.name)
    }

    override fun isAiCoreSupported(): Boolean = aiCoreCapabilityProvider.isSupported()

    private fun resolveAutomatic(
        packStatus: ModelPackStatus,
        isEntitled: Boolean
    ): AiEngineType = when {
        aiCoreCapabilityProvider.isSupported() -> AiEngineType.AI_CORE_GEMMA_4
        isEntitled && packStatus is ModelPackStatus.Installed -> AiEngineType.LITE_RT_LM
        else -> AiEngineType.AI_CORE_GEMMA_4
    }
}
