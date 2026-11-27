package es.pedrazamiguez.splittrip.data.litert

import android.content.Context
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException

internal class DynamicLiteRtEngineClient(
    private val context: Context,
    private val runtimeManager: LiteRtDynamicRuntimeManager,
    private val factoryLoader: LiteRtEngineClientFactoryLoader
) : LiteRtEngineClient {

    private val delegateLock = Any()

    @Volatile
    private var delegate: LiteRtEngineClient? = null

    override suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String?
    ): String = resolveDelegate().generate(
        modelPath = modelPath,
        prompt = prompt,
        systemInstruction = systemInstruction
    )

    override fun invalidate(modelPath: String?) {
        delegate?.invalidate(modelPath)
    }

    override fun close() {
        synchronized(delegateLock) {
            runCatching { delegate?.close() }
            delegate = null
        }
    }

    private fun resolveDelegate(): LiteRtEngineClient {
        delegate?.let { return it }
        return synchronized(delegateLock) {
            delegate?.let { return@synchronized it }
            if (!runtimeManager.isRuntimeInstalled() || !runtimeManager.activateInstalledRuntime()) {
                throw LocalAiModelUnavailableException("LiteRT-LM dynamic runtime module is not installed")
            }
            val factory = factoryLoader.loadFactory()
                ?: throw LocalAiModelUnavailableException("LiteRT-LM engine factory is unavailable")
            val created = factory.create(context.applicationContext)
            delegate = created
            created
        }
    }
}
