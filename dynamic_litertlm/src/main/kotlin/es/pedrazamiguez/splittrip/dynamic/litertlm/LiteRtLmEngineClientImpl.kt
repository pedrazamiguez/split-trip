package es.pedrazamiguez.splittrip.dynamic.litertlm

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
import com.google.android.play.core.splitinstall.SplitInstallHelper
import es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.litert.LiteRtRuntimeConfig
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

    @Volatile
    private var nativeLibrariesPreloaded = false

    override suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String?
    ): String = mutex.withLock {
        val engine = getOrCreateEngine(modelPath)
        val conversationSpec = LiteRtRuntimeConfig.buildConversationSpec(systemInstruction)
        val conversationConfig = conversationSpec.toConversationConfig()
        try {
            engine.createConversation(conversationConfig).use { conversation ->
                awaitConversationResponse(
                    conversation = conversation,
                    prompt = prompt,
                    conversationSpec = conversationSpec
                )
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
        prompt: String,
        conversationSpec: LiteRtRuntimeConfig.ConversationSpec
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
                maxOutputToken = conversationSpec.maxOutputTokens,
                thinkingConfig = conversationSpec.thinkingSpec.toThinkingConfig()
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

            ensureNativeLibrariesLoaded()
            Engine.setNativeMinLogSeverity(LogSeverity.WARNING)
            val engineSpec = LiteRtRuntimeConfig.buildEngineSpec(
                modelPath = modelPath,
                cacheDir = context.cacheDir.absolutePath
            )
            val engine = Engine(engineSpec.toEngineConfig())
            engine.initialize()
            cachedEngine = engine
            cachedModelPath = modelPath
            Timber.d(
                "LiteRtLmEngineClientImpl: initialized CPU engine (maxNumTokens=%d) for %s",
                LiteRtRuntimeConfig.DEFAULT_MAX_NUM_TOKENS,
                modelPath
            )
            engine
        }
    }

    // Load split native .so libraries immediately after on-demand install so JNI succeeds without process restart.
    private fun ensureNativeLibrariesLoaded() {
        if (nativeLibrariesPreloaded) return
        runCatching { SplitInstallHelper.loadLibrary(context, NATIVE_LIB_LITERT) }
            .onFailure { Timber.d(it, "SplitInstallHelper preload skipped for %s", NATIVE_LIB_LITERT) }
        runCatching { SplitInstallHelper.loadLibrary(context, NATIVE_LIB_LITERT_LM_JNI) }
            .onFailure { Timber.w(it, "SplitInstallHelper preload failed for %s", NATIVE_LIB_LITERT_LM_JNI) }
        nativeLibrariesPreloaded = true
    }

    private fun LiteRtRuntimeConfig.EngineSpec.toEngineConfig(): EngineConfig = EngineConfig(
        modelPath = modelPath,
        backend = when (backend) {
            LiteRtRuntimeConfig.BackendType.CPU -> Backend.CPU()
        },
        maxNumTokens = maxNumTokens,
        cacheDir = cacheDir
    )

    private fun LiteRtRuntimeConfig.ThinkingSpec.toThinkingConfig(): ThinkingConfig = ThinkingConfig(
        enableThinking = enableThinking,
        thinkingTokenBudget = thinkingTokenBudget
    )

    private fun LiteRtRuntimeConfig.ConversationSpec.toConversationConfig(): ConversationConfig = ConversationConfig(
        systemInstruction = systemInstruction?.let { Contents.of(it) },
        samplerConfig = SamplerConfig(
            topK = samplerSpec.topK,
            topP = samplerSpec.topP,
            temperature = samplerSpec.temperature,
            seed = samplerSpec.seed
        ),
        maxOutputToken = maxOutputTokens,
        thinkingConfig = thinkingSpec.toThinkingConfig()
    )

    private companion object {
        private const val NATIVE_LIB_LITERT = "LiteRt"
        private const val NATIVE_LIB_LITERT_LM_JNI = "litertlm_jni"
    }
}
