package es.pedrazamiguez.splittrip.data.litert

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal class LiteRtLmEngineClientImpl(
    private val engineFactory: LiteRtNativeEngineFactory,
    private val callbackDrainTimeoutMs: Long = DEFAULT_CALLBACK_DRAIN_TIMEOUT_MS
) : LiteRtEngineClient {

    constructor(context: Context) : this(
        engineFactory = AndroidLiteRtNativeEngineFactory(context)
    )

    private val mutex = Mutex()
    private val engineStateLock = Any()
    private var cachedEngine: ManagedLiteRtEngine? = null
    private var cachedModelPath: String? = null
    private var initializingModelPath: String? = null
    private var activeGenerationEngine: ManagedLiteRtEngine? = null
    private var activeGenerationInvalidated: Boolean = false
    private var engineCreationEpoch: Long = 0L

    override suspend fun generate(
        modelPath: String,
        prompt: String,
        systemInstruction: String?
    ): String = mutex.withLock {
        val engine = acquireEngineForGeneration(modelPath)
        var conversationDrained = true
        try {
            val conversationSpec = LiteRtRuntimeConfig.buildConversationSpec(systemInstruction)
            val conversation = engine.createConversation(conversationSpec)
            awaitConversationWithSafeTeardown(
                conversation = conversation,
                prompt = prompt,
                conversationSpec = conversationSpec,
                onDrainTimeout = { conversationDrained = false }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            invalidate(modelPath)
            throw t
        } finally {
            val engineToClose = releaseEngineAfterGeneration(
                engine = engine,
                conversationDrained = conversationDrained
            )
            engineToClose?.let { closeEngineSafely(it) }
        }
    }

    override fun invalidate(modelPath: String?) {
        val engineToCloseNow = synchronized(engineStateLock) {
            val matchesTarget = modelPath == null ||
                modelPath == cachedModelPath ||
                modelPath == initializingModelPath
            if (matchesTarget) {
                engineCreationEpoch++
            }
            val current = cachedEngine
            if (current != null && (modelPath == null || modelPath == cachedModelPath)) {
                cachedEngine = null
                cachedModelPath = null
                if (activeGenerationEngine === current) {
                    activeGenerationInvalidated = true
                    null
                } else {
                    current
                }
            } else {
                null
            }
        }
        engineToCloseNow?.let { closeEngineSafely(it) }
    }

    override fun close() {
        invalidate()
    }

    private suspend fun awaitConversationWithSafeTeardown(
        conversation: ManagedLiteRtConversation,
        prompt: String,
        conversationSpec: LiteRtRuntimeConfig.ConversationSpec,
        onDrainTimeout: () -> Unit
    ): String {
        val terminalCallbackReceived = CompletableDeferred<Unit>()
        val cancelRequested = AtomicBoolean(false)
        var asyncRequestScheduled = false

        fun requestCancelOnce() {
            if (cancelRequested.compareAndSet(false, true)) {
                runCatching { conversation.cancelProcess() }
                    .onFailure { Timber.w(it, "LiteRtLmEngineClientImpl: cancelProcess() failed") }
            }
        }

        try {
            return awaitConversationResponse(
                conversation = conversation,
                prompt = prompt,
                conversationSpec = conversationSpec,
                terminalCallbackReceived = terminalCallbackReceived,
                onAsyncScheduled = { asyncRequestScheduled = true },
                onCancelRequested = ::requestCancelOnce
            )
        } finally {
            // Native Conversation::session_ is freed by close(); wait for the worker callback to drain first.
            withContext(NonCancellable) {
                val canCloseSafely = if (!asyncRequestScheduled || terminalCallbackReceived.isCompleted) {
                    true
                } else {
                    requestCancelOnce()
                    val drained = withTimeoutOrNull(callbackDrainTimeoutMs) {
                        terminalCallbackReceived.await()
                        true
                    } ?: false
                    if (!drained) {
                        onDrainTimeout()
                        Timber.w(
                            "LiteRtLmEngineClientImpl: drain timed out after %d ms; skipping close",
                            callbackDrainTimeoutMs
                        )
                    }
                    drained
                }
                if (canCloseSafely) {
                    runCatching { conversation.close() }
                        .onFailure { Timber.w(it, "LiteRtLmEngineClientImpl: conversation.close() failed") }
                }
            }
        }
    }

    private suspend fun awaitConversationResponse(
        conversation: ManagedLiteRtConversation,
        prompt: String,
        conversationSpec: LiteRtRuntimeConfig.ConversationSpec,
        terminalCallbackReceived: CompletableDeferred<Unit>,
        onAsyncScheduled: () -> Unit,
        onCancelRequested: () -> Unit
    ): String = suspendCancellableCoroutine { continuation ->
        if (!continuation.isActive) return@suspendCancellableCoroutine

        val outputBuffer = StringBuffer()
        val callback = buildConversationCallback(
            outputBuffer = outputBuffer,
            terminalCallbackReceived = terminalCallbackReceived,
            continuation = continuation
        )

        try {
            conversation.sendMessageAsync(
                prompt = prompt,
                conversationSpec = conversationSpec,
                callback = callback
            )
            onAsyncScheduled()
        } catch (t: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(t)
            }
            return@suspendCancellableCoroutine
        }

        // Abort the native C++ decoding loop immediately when coroutine timeout or typing cancels generation.
        continuation.invokeOnCancellation {
            onCancelRequested()
        }
    }

    private fun buildConversationCallback(
        outputBuffer: StringBuffer,
        terminalCallbackReceived: CompletableDeferred<Unit>,
        continuation: CancellableContinuation<String>
    ): LiteRtConversationCallback = object : LiteRtConversationCallback {
        override fun onTextChunk(text: String) {
            outputBuffer.append(text)
        }

        override fun onDone() {
            terminalCallbackReceived.complete(Unit)
            if (continuation.isActive) {
                continuation.resume(outputBuffer.toString())
            }
        }

        override fun onError(throwable: Throwable) {
            terminalCallbackReceived.complete(Unit)
            if (continuation.isActive) {
                continuation.resumeWithException(throwable)
            }
        }
    }

    private fun acquireEngineForGeneration(modelPath: String): ManagedLiteRtEngine {
        var staleEngineToClose: ManagedLiteRtEngine? = null
        var observedEpoch = 0L

        synchronized(engineStateLock) {
            val current = cachedEngine
            if (current != null && cachedModelPath == modelPath && current.isInitialized()) {
                activeGenerationEngine = current
                activeGenerationInvalidated = false
                return current
            }
            staleEngineToClose = current
            cachedEngine = null
            cachedModelPath = null
            initializingModelPath = modelPath
            observedEpoch = engineCreationEpoch
        }

        staleEngineToClose?.let { closeEngineSafely(it) }

        val newEngine = try {
            engineFactory.createAndInitialize(modelPath)
        } catch (t: Throwable) {
            synchronized(engineStateLock) {
                initializingModelPath = null
            }
            throw t
        }

        synchronized(engineStateLock) {
            initializingModelPath = null
            activeGenerationEngine = newEngine
            if (engineCreationEpoch == observedEpoch) {
                cachedEngine = newEngine
                cachedModelPath = modelPath
                activeGenerationInvalidated = false
            } else {
                activeGenerationInvalidated = true
            }
        }
        return newEngine
    }

    private fun releaseEngineAfterGeneration(
        engine: ManagedLiteRtEngine,
        conversationDrained: Boolean
    ): ManagedLiteRtEngine? = synchronized(engineStateLock) {
        val shouldClose = activeGenerationInvalidated || cachedEngine !== engine
        if (!conversationDrained && cachedEngine === engine) {
            cachedEngine = null
            cachedModelPath = null
        }
        activeGenerationEngine = null
        activeGenerationInvalidated = false
        if (!conversationDrained) {
            Timber.w(
                "LiteRtLmEngineClientImpl: detached engine without closing after un-drained conversation timeout"
            )
            null
        } else if (shouldClose) {
            engine
        } else {
            null
        }
    }

    private fun closeEngineSafely(engine: ManagedLiteRtEngine) {
        runCatching { engine.close() }
            .onFailure { Timber.w(it, "LiteRtLmEngineClientImpl: engine.close() failed") }
    }

    internal companion object {
        const val DEFAULT_CALLBACK_DRAIN_TIMEOUT_MS = 5_000L
    }
}
