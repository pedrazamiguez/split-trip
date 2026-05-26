package es.pedrazamiguez.splittrip.data.repository.impl

import com.google.ai.edge.aicore.GenerativeModel
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class AICoreInferenceRepositoryImpl(
    private val generativeModel: GenerativeModel,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AiInferenceRepository {

    @Volatile private var enginePrepared = false
    private val preparationMutex = Mutex()

    override suspend fun generateContent(prompt: String): Result<String> = withContext(defaultDispatcher) {
        try {
            ensureEngineReady()
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: ""
            Result.success(text)
        } catch (e: Exception) {
            Timber.e(e, "AICoreInferenceRepositoryImpl: generateContent failed")
            Result.failure(e)
        }
    }

    override suspend fun generateStructuredOutput(prompt: String, jsonSchema: String): Result<String> {
        return generateContent(prompt)
    }

    override fun getEngineType(): AiEngineType = AiEngineType.AI_CORE_GEMMA_4

    private suspend fun ensureEngineReady() {
        if (enginePrepared) return
        preparationMutex.withLock {
            if (!enginePrepared) {
                Timber.d("AICoreInferenceRepositoryImpl: preparing inference engine (loading weights into NPU)…")
                val startMs = System.currentTimeMillis()
                generativeModel.prepareInferenceEngine()
                enginePrepared = true
                Timber.d(
                    "AICoreInferenceRepositoryImpl: inference engine ready in ${System.currentTimeMillis() - startMs}ms"
                )
            }
        }
    }
}
