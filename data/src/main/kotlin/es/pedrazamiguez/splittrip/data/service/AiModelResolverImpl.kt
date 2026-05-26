package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.model.AiEngineType
import es.pedrazamiguez.splittrip.domain.service.AiModelResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AiModelResolverImpl(
    private val userPreferences: UserPreferences
) : AiModelResolver {

    override fun getActiveModel(): Flow<AiEngineType> {
        return userPreferences.activeAiModel.map { overrideName ->
            val overrideModel = overrideName?.let {
                runCatching { AiEngineType.valueOf(it) }.getOrNull()
            }
            overrideModel ?: resolveAutomatic()
        }
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

    private fun resolveAutomatic(): AiEngineType {
        // Since LiteRT-LM is not fully implemented/available yet,
        // we default to AI_CORE_GEMMA_4 (which returns a clean NO_OP fallback if unsupported).
        return AiEngineType.AI_CORE_GEMMA_4
    }
}
