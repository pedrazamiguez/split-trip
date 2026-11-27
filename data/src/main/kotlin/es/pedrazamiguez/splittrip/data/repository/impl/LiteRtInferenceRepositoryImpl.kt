package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.service.JsonSanitizer
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class LiteRtInferenceRepositoryImpl(
    private val modelPackService: LocalAiModelPackService,
    private val featureGateService: FeatureGateService,
    private val engineClient: LiteRtEngineClient,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AiInferenceRepository {

    override suspend fun generateContent(prompt: String): Result<String> = withContext(defaultDispatcher) {
        executeInference(prompt = prompt, systemInstruction = null)
    }

    override suspend fun generateStructuredOutput(
        prompt: String,
        jsonSchema: String
    ): Result<String> = withContext(defaultDispatcher) {
        val systemInstruction =
            "Respond ONLY with a valid JSON object conforming strictly to this schema. " +
                "Do not include markdown fences or commentary.\nSchema:\n$jsonSchema"
        executeInference(prompt = prompt, systemInstruction = systemInstruction)
            .map { rawOutput -> JsonSanitizer.sanitize(rawOutput) }
    }

    override fun getEngineType(): AiEngineType = AiEngineType.LITE_RT_LM

    @Suppress("ReturnCount")
    private suspend fun executeInference(
        prompt: String,
        systemInstruction: String?
    ): Result<String> {
        val isEntitled = featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK).first()
        if (!isEntitled) {
            return Result.failure(LocalAiModelNotEntitledException())
        }

        val modelPath = modelPackService.getInstalledModelPath()
            ?: return Result.failure(LocalAiModelUnavailableException())

        return try {
            val output = engineClient.generate(
                modelPath = modelPath,
                prompt = prompt,
                systemInstruction = systemInstruction
            )
            Result.success(output)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LinkageError) {
            handleInferenceFailure(modelPath = modelPath, throwable = e, invalidateEngine = true)
        } catch (e: OutOfMemoryError) {
            handleInferenceFailure(modelPath = modelPath, throwable = e, invalidateEngine = true)
        } catch (t: Throwable) {
            handleInferenceFailure(modelPath = modelPath, throwable = t, invalidateEngine = false)
        }
    }

    private fun handleInferenceFailure(
        modelPath: String,
        throwable: Throwable,
        invalidateEngine: Boolean
    ): Result<String> {
        if (invalidateEngine) {
            runCatching { engineClient.invalidate(modelPath) }
        }
        Timber.e(throwable, "LiteRT inference failed")
        return Result.failure(LocalAiModelUnavailableException("LiteRT inference failed", throwable))
    }
}
