package es.pedrazamiguez.splittrip.data.repository.impl

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class LiteRtLmEngineClientImpl(
    private val context: Context
) : LiteRtEngineClient {

    private val mutex = Mutex()
    private var cachedEngine: Engine? = null
    private var cachedModelPath: String? = null

    override suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String?
    ): String = mutex.withLock {
        val engine = getOrCreateEngine(modelPath)
        val conversationConfig = ConversationConfig(
            systemInstruction = systemInstruction?.let { Contents.of(it) },
            samplerConfig = SamplerConfig(
                topK = DEFAULT_TOP_K,
                topP = DEFAULT_TOP_P,
                temperature = DEFAULT_TEMPERATURE,
                seed = DEFAULT_SEED
            )
        )
        engine.createConversation(conversationConfig).use { conversation ->
            val response = conversation.sendMessage(prompt)
            response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString(separator = "") { it.text }
                .ifBlank { response.toString() }
        }
    }

    override fun invalidate(modelPath: String?) {
        if (modelPath == null || modelPath == cachedModelPath) {
            cachedEngine?.close()
            cachedEngine = null
            cachedModelPath = null
        }
    }

    override fun close() {
        invalidate()
    }

    private fun getOrCreateEngine(modelPath: String): Engine {
        val currentEngine = cachedEngine
        if (currentEngine != null && cachedModelPath == modelPath && currentEngine.isInitialized()) {
            return currentEngine
        }

        currentEngine?.close()
        cachedEngine = null
        cachedModelPath = null

        Engine.setNativeMinLogSeverity(LogSeverity.WARNING)
        val cacheDir = context.cacheDir.absolutePath

        val engine = try {
            createAndInitEngine(modelPath = modelPath, backend = Backend.GPU(), cacheDir = cacheDir)
        } catch (gpuError: Exception) {
            Timber.w(gpuError, "LiteRtLmEngineClientImpl: GPU backend init failed, falling back to CPU")
            createAndInitEngine(modelPath = modelPath, backend = Backend.CPU(), cacheDir = cacheDir)
        }

        cachedEngine = engine
        cachedModelPath = modelPath
        return engine
    }

    private fun createAndInitEngine(
        modelPath: String,
        backend: Backend,
        cacheDir: String
    ): Engine {
        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = cacheDir
        )
        return Engine(engineConfig).apply { initialize() }
    }

    private companion object {
        private const val DEFAULT_TOP_K = 40
        private const val DEFAULT_TOP_P = 0.95
        private const val DEFAULT_TEMPERATURE = 0.2
        private const val DEFAULT_SEED = 0
    }
}
