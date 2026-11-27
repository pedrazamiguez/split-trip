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
import es.pedrazamiguez.splittrip.data.litert.LiteRtRuntimeConfig
import timber.log.Timber

internal class AndroidLiteRtNativeEngineFactory(
    private val context: Context
) : LiteRtNativeEngineFactory {

    @Volatile
    private var nativeLibrariesPreloaded = false

    override fun createAndInitialize(modelPath: String): ManagedLiteRtEngine {
        ensureNativeLibrariesLoaded()
        Engine.setNativeMinLogSeverity(LogSeverity.WARNING)
        val engineSpec = LiteRtRuntimeConfig.buildEngineSpec(
            modelPath = modelPath,
            cacheDir = context.cacheDir.absolutePath
        )
        val engine = Engine(engineSpec.toEngineConfig())
        try {
            engine.initialize()
        } catch (t: Throwable) {
            runCatching { engine.close() }
            throw t
        }
        Timber.d(
            "AndroidLiteRtNativeEngineFactory: initialized CPU engine (maxNumTokens=%d) for %s",
            LiteRtRuntimeConfig.DEFAULT_MAX_NUM_TOKENS,
            modelPath
        )
        return AndroidManagedLiteRtEngine(engine)
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

    private class AndroidManagedLiteRtEngine(
        private val engine: Engine
    ) : ManagedLiteRtEngine {
        override fun isInitialized(): Boolean = engine.isInitialized()

        override fun createConversation(
            conversationSpec: LiteRtRuntimeConfig.ConversationSpec
        ): ManagedLiteRtConversation = AndroidManagedLiteRtConversation(
            engine.createConversation(conversationSpec.toConversationConfig())
        )

        override fun close() {
            engine.close()
        }
    }

    private class AndroidManagedLiteRtConversation(
        private val conversation: Conversation
    ) : ManagedLiteRtConversation {
        override fun sendMessageAsync(
            prompt: String,
            conversationSpec: LiteRtRuntimeConfig.ConversationSpec,
            callback: LiteRtConversationCallback
        ) {
            val messageCallback = object : MessageCallback {
                override fun onMessage(message: Message) {
                    message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .forEach { content -> callback.onTextChunk(content.text) }
                }

                override fun onDone() {
                    callback.onDone()
                }

                override fun onError(throwable: Throwable) {
                    callback.onError(throwable)
                }
            }
            conversation.sendMessageAsync(
                text = prompt,
                callback = messageCallback,
                maxOutputToken = conversationSpec.maxOutputTokens,
                thinkingConfig = conversationSpec.thinkingSpec.toThinkingConfig()
            )
        }

        override fun cancelProcess() {
            conversation.cancelProcess()
        }

        override fun close() {
            conversation.close()
        }
    }

    private companion object {
        private const val NATIVE_LIB_LITERT = "LiteRt"
        private const val NATIVE_LIB_LITERT_LM_JNI = "litertlm_jni"

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

        private fun LiteRtRuntimeConfig.ConversationSpec.toConversationConfig(): ConversationConfig =
            ConversationConfig(
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
    }
}
