package es.pedrazamiguez.splittrip.data.repository.impl

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class LiteRtLmEngineClientImpl(
    private val context: Context
) : LiteRtEngineClient {

    private val mutex = Mutex()
    private val engineStateLock = Any()
    private var cachedEngine: Engine? = null
    private var cachedModelPath: String? = null

    override suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String?
    ): String = mutex.withLock {
        val engine = getOrCreateEngine(modelPath)
        val conversationConfig = buildConversationConfig(systemInstruction)
        try {
            engine.createConversation(conversationConfig).use { conversation ->
                awaitConversationResponse(conversation, prompt)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            invalidate(modelPath)
            throw t
        }
    }

    override fun invalidate(modelPath: String?) {
        synchronized(engineStateLock) {
            if (modelPath == null || modelPath == cachedModelPath) {
                runCatching { cachedEngine?.close() }
                cachedEngine = null
                cachedModelPath = null
            }
        }
    }

    override fun close() {
        invalidate()
    }

    private suspend fun awaitConversationResponse(
        conversation: Conversation,
        prompt: String
    ): String = suspendCancellableCoroutine { continuation ->
        val outputBuffer = StringBuffer()
        val callback = object : MessageCallback {
            override fun onMessage(message: Message) {
                message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .forEach { content -> outputBuffer.append(content.text) }
            }

            override fun onDone() {
                if (continuation.isActive) {
                    continuation.resume(outputBuffer.toString())
                }
            }

            override fun onError(throwable: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(throwable)
                }
            }
        }

        // Abort the native C++ decoding loop immediately when coroutine timeout cancels generation.
        continuation.invokeOnCancellation {
            runCatching { conversation.cancelProcess() }
        }

        try {
            conversation.sendMessageAsync(
                text = prompt,
                callback = callback,
                maxOutputToken = DEFAULT_MAX_OUTPUT_TOKENS,
                thinkingConfig = DISABLED_THINKING_CONFIG
            )
        } catch (t: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(t)
            }
        }
    }

    private fun getOrCreateEngine(modelPath: String): Engine = synchronized(engineStateLock) {
        val currentEngine = cachedEngine
        if (currentEngine != null && cachedModelPath == modelPath && currentEngine.isInitialized()) {
            currentEngine
        } else {
            runCatching { currentEngine?.close() }
            cachedEngine = null
            cachedModelPath = null

            Engine.setNativeMinLogSeverity(LogSeverity.WARNING)
            val engineConfig = buildEngineConfig(
                modelPath = modelPath,
                cacheDir = context.cacheDir.absolutePath
            )
            val engine = Engine(engineConfig)
            engine.initialize()
            cachedEngine = engine
            cachedModelPath = modelPath
            Timber.d(
                "LiteRtLmEngineClientImpl: initialized CPU engine (maxNumTokens=%d) for %s",
                DEFAULT_MAX_NUM_TOKENS,
                modelPath
            )
            engine
        }
    }

    companion object {
        internal const val DEFAULT_MAX_NUM_TOKENS = 2_560
        internal const val DEFAULT_MAX_OUTPUT_TOKENS = 384
        internal val DISABLED_THINKING_CONFIG = ThinkingConfig(
            enableThinking = false,
            thinkingTokenBudget = 0
        )

        private const val DEFAULT_TOP_K = 40
        private const val DEFAULT_TOP_P = 0.95
        private const val DEFAULT_TEMPERATURE = 0.2
        private const val DEFAULT_SEED = 0

        internal fun buildEngineConfig(
            modelPath: String,
            cacheDir: String
        ): EngineConfig = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            maxNumTokens = DEFAULT_MAX_NUM_TOKENS,
            cacheDir = cacheDir
        )

        internal fun buildConversationConfig(
            systemInstruction: String?,
            maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS
        ): ConversationConfig = ConversationConfig(
            systemInstruction = systemInstruction?.let { Contents.of(it) },
            samplerConfig = SamplerConfig(
                topK = DEFAULT_TOP_K,
                topP = DEFAULT_TOP_P,
                temperature = DEFAULT_TEMPERATURE,
                seed = DEFAULT_SEED
            ),
            maxOutputToken = maxOutputTokens,
            thinkingConfig = DISABLED_THINKING_CONFIG
        )
    }
}
