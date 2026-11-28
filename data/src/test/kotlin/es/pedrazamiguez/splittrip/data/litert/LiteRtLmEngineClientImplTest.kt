package es.pedrazamiguez.splittrip.data.litert

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiteRtLmEngineClientImplTest {

    @Test
    fun `generate creates engine once, reuses cached engine, aggregates chunks, and closes conversation`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val firstResultDeferred = launchAndCaptureResult {
                client.generate(
                    modelPath = MODEL_A,
                    prompt = "prompt-1",
                    systemInstruction = "sys"
                )
            }
            runCurrent()

            assertEquals(1, factory.createdEngines.size)
            val engine = factory.createdEngines.single()
            assertEquals(1, engine.conversations.size)
            val conv1 = engine.conversations[0]
            conv1.latestCallback?.apply {
                onTextChunk("{\"category\":\"FOOD\",")
                onTextChunk("\"subcategory\":\"CAFE\"}")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("{\"category\":\"FOOD\",\"subcategory\":\"CAFE\"}", firstResultDeferred.value)
            assertEquals(1, conv1.closeCount)
            assertEquals(0, engine.closeCount)

            val secondResultDeferred = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "prompt-2")
            }
            runCurrent()

            assertEquals(1, factory.createdEngines.size)
            assertEquals(2, engine.conversations.size)
            val conv2 = engine.conversations[1]
            conv2.latestCallback?.apply {
                onTextChunk("{\"category\":\"TRAVEL\"}")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("{\"category\":\"TRAVEL\"}", secondResultDeferred.value)
            assertEquals(1, conv2.closeCount)
            assertEquals(0, engine.closeCount)
        }

    @Test
    fun `generate reinitializes and closes previous engine when modelPath changes or uninitialized`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val jobA = launch {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()
            val engineA = factory.createdEngines[0]
            engineA.conversations.single().latestCallback?.apply {
                onTextChunk("A")
                onDone()
            }
            advanceUntilIdle()
            assertTrue(jobA.isCompleted)

            val jobB = launch {
                client.generate(modelPath = MODEL_B, prompt = "p2")
            }
            runCurrent()
            assertEquals(1, engineA.closeCount)
            assertEquals(2, factory.createdEngines.size)
            val engineB = factory.createdEngines[1]
            assertTrue(engineA.events.indexOf("ENGINE_CLOSE") >= 0)
            engineB.conversations.single().latestCallback?.apply {
                onTextChunk("B")
                onDone()
            }
            advanceUntilIdle()
            assertTrue(jobB.isCompleted)
            assertEquals(0, engineB.closeCount)

            engineB.initialized = false
            val jobB2 = launch {
                client.generate(modelPath = MODEL_B, prompt = "p3")
            }
            runCurrent()
            assertEquals(1, engineB.closeCount)
            assertEquals(3, factory.createdEngines.size)
            val engineB2 = factory.createdEngines[2]
            engineB2.conversations.single().latestCallback?.apply {
                onTextChunk("B2")
                onDone()
            }
            advanceUntilIdle()
            assertTrue(jobB2.isCompleted)
            assertEquals(0, engineB2.closeCount)
        }

    @Test
    fun `cancelling generate calls cancelProcess and waits for terminal onError before closing conversation`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)
            var cancellationError: Throwable? = null

            val job = launch {
                try {
                    client.generate(modelPath = MODEL_A, prompt = "entradas")
                } catch (e: CancellationException) {
                    cancellationError = e
                    throw e
                }
            }
            runCurrent()

            val engine = factory.createdEngines.single()
            val conversation = engine.conversations.single()
            assertNotNull(conversation.latestCallback)

            job.cancel()
            runCurrent()

            assertEquals(1, conversation.cancelProcessCount)
            assertEquals(0, conversation.closeCount)
            assertEquals(0, engine.closeCount)
            assertFalse(job.isCompleted)

            conversation.latestCallback?.onError(IllegalStateException("Cancelled"))
            advanceUntilIdle()

            assertTrue(job.isCancelled)
            assertNotNull(cancellationError)
            assertEquals(
                listOf("SEND_ASYNC", "CANCEL_PROCESS", "CONVERSATION_CLOSE"),
                conversation.events
            )
            assertEquals(0, engine.closeCount)
        }

    @Test
    fun `cancelling generate waits for terminal onDone before closing conversation`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val job = launch {
                client.generate(modelPath = MODEL_A, prompt = "entradas")
            }
            runCurrent()

            val engine = factory.createdEngines.single()
            val conversation = engine.conversations.single()

            job.cancel()
            runCurrent()

            assertEquals(1, conversation.cancelProcessCount)
            assertEquals(0, conversation.closeCount)

            conversation.latestCallback?.onDone()
            advanceUntilIdle()

            assertTrue(job.isCancelled)
            assertEquals(
                listOf("SEND_ASYNC", "CANCEL_PROCESS", "CONVERSATION_CLOSE"),
                conversation.events
            )
            assertEquals(0, engine.closeCount)

            // Cached engine remains reusable for subsequent generations.
            val nextResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "next")
            }
            runCurrent()
            assertEquals(1, factory.createdEngines.size)
            engine.conversations.last().latestCallback?.apply {
                onTextChunk("ok")
                onDone()
            }
            advanceUntilIdle()
            assertEquals("ok", nextResult.value)
        }

    @Test
    fun `cancelling generate skips conversation close and detaches engine on drain timeout`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(
                engineFactory = factory,
                callbackDrainTimeoutMs = 500L
            )

            val job = launch {
                client.generate(modelPath = MODEL_A, prompt = "hung")
            }
            runCurrent()

            val engine1 = factory.createdEngines.single()
            val conversation1 = engine1.conversations.single()

            job.cancel()
            runCurrent()
            assertEquals(1, conversation1.cancelProcessCount)
            assertEquals(0, conversation1.closeCount)

            advanceTimeBy(500L)
            advanceUntilIdle()

            assertTrue(job.isCancelled)
            assertEquals(1, conversation1.cancelProcessCount)
            assertEquals(0, conversation1.closeCount)
            assertEquals(0, engine1.closeCount)

            // Subsequent generate must create a fresh engine instead of reusing the detached one.
            val nextResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "fresh")
            }
            runCurrent()

            assertEquals(2, factory.createdEngines.size)
            val engine2 = factory.createdEngines[1]
            engine2.conversations.single().latestCallback?.apply {
                onTextChunk("recovered")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("recovered", nextResult.value)
            assertEquals(0, engine1.closeCount)
        }

    @Test
    fun `sendMessageAsync throw closes conversation immediately without cancelProcess and invalidates`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            factory.nextConversationCustomizer = { conversation ->
                conversation.sendAsyncThrow = IllegalStateException("boom")
            }
            val client = LiteRtLmEngineClientImpl(
                engineFactory = factory,
                callbackDrainTimeoutMs = 5_000L
            )

            var caught: Throwable? = null
            val job = launch {
                try {
                    client.generate(modelPath = MODEL_A, prompt = "p")
                } catch (t: Throwable) {
                    caught = t
                }
            }
            runCurrent()

            // Completed immediately without waiting 5000 ms drain timeout.
            assertTrue(job.isCompleted)
            assertTrue(caught is IllegalStateException)
            val engine = factory.createdEngines.single()
            val conversation = engine.conversations.single()
            assertEquals(0, conversation.cancelProcessCount)
            assertEquals(1, conversation.closeCount)
            assertEquals(1, engine.closeCount)
        }

    @Test
    fun `onError callback closes conversation immediately and invalidates cached engine`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val events = mutableListOf<String>()
            factory.sharedEventLog = events
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            var caught: Throwable? = null
            val job = launch {
                try {
                    client.generate(modelPath = MODEL_A, prompt = "p")
                } catch (t: Throwable) {
                    caught = t
                }
            }
            runCurrent()

            val engine1 = factory.createdEngines.single()
            val conversation1 = engine1.conversations.single()
            conversation1.latestCallback?.onError(IllegalStateException("native fail"))
            advanceUntilIdle()

            assertTrue(caught is IllegalStateException)
            assertEquals(1, conversation1.closeCount)
            assertEquals(1, engine1.closeCount)
            val convCloseIndex = events.indexOf("CONVERSATION_CLOSE")
            val engineCloseIndex = events.indexOf("ENGINE_CLOSE")
            assertTrue(convCloseIndex in 0 until engineCloseIndex)

            val nextResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "retry")
            }
            runCurrent()
            assertEquals(2, factory.createdEngines.size)
            factory.createdEngines[1].conversations.single().latestCallback?.apply {
                onTextChunk("ok")
                onDone()
            }
            advanceUntilIdle()
            assertEquals("ok", nextResult.value)
        }

    @Test
    fun `concurrent invalidate during generate defers engine close until conversation closes on onDone`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val events = mutableListOf<String>()
            factory.sharedEventLog = events
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val result = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()

            val engine1 = factory.createdEngines.single()
            val conversation1 = engine1.conversations.single()

            client.invalidate(MODEL_A)
            assertEquals(0, engine1.closeCount)

            conversation1.latestCallback?.apply {
                onTextChunk("done")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("done", result.value)
            assertEquals(1, conversation1.closeCount)
            assertEquals(1, engine1.closeCount)
            assertTrue(events.indexOf("CONVERSATION_CLOSE") < events.indexOf("ENGINE_CLOSE"))

            val nextResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p2")
            }
            runCurrent()
            assertEquals(2, factory.createdEngines.size)
            factory.createdEngines[1].conversations.single().latestCallback?.apply {
                onTextChunk("fresh")
                onDone()
            }
            advanceUntilIdle()
            assertEquals("fresh", nextResult.value)
        }

    @Test
    fun `concurrent invalidate during active generate defers engine close across cancellation and onError`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val events = mutableListOf<String>()
            factory.sharedEventLog = events
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val job = launch {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()

            val engine1 = factory.createdEngines.single()
            val conversation1 = engine1.conversations.single()

            client.invalidate(MODEL_A)
            assertEquals(0, engine1.closeCount)

            job.cancel()
            runCurrent()
            assertEquals(1, conversation1.cancelProcessCount)
            assertEquals(0, conversation1.closeCount)
            assertEquals(0, engine1.closeCount)

            conversation1.latestCallback?.onError(IllegalStateException("Cancelled"))
            advanceUntilIdle()

            assertEquals(1, conversation1.closeCount)
            assertEquals(1, engine1.closeCount)
            assertTrue(events.indexOf("CONVERSATION_CLOSE") < events.indexOf("ENGINE_CLOSE"))
        }

    @Test
    fun `concurrent close during active generate defers engine close until generation completes`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val events = mutableListOf<String>()
            factory.sharedEventLog = events
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val result = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()

            val engine1 = factory.createdEngines.single()
            val conversation1 = engine1.conversations.single()

            client.close()
            assertEquals(0, engine1.closeCount)

            conversation1.latestCallback?.apply {
                onTextChunk("completed")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("completed", result.value)
            assertEquals(1, conversation1.closeCount)
            assertEquals(1, engine1.closeCount)
            assertTrue(events.indexOf("CONVERSATION_CLOSE") < events.indexOf("ENGINE_CLOSE"))
        }

    @Test
    fun `invalidate for non-matching modelPath leaves active and cached engine untouched`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val firstResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()
            val engine = factory.createdEngines.single()
            engine.conversations.single().latestCallback?.apply {
                onTextChunk("first")
                onDone()
            }
            advanceUntilIdle()
            assertEquals("first", firstResult.value)

            client.invalidate("/models/other.litertlm")
            assertEquals(0, engine.closeCount)

            val secondResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p2")
            }
            runCurrent()
            assertEquals(1, factory.createdEngines.size)
            engine.conversations.last().latestCallback?.apply {
                onTextChunk("second")
                onDone()
            }
            advanceUntilIdle()
            assertEquals("second", secondResult.value)
            assertEquals(0, engine.closeCount)
        }

    @Test
    fun `invalidate when idle immediately closes cached engine`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val job = launch {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()
            val engine = factory.createdEngines.single()
            engine.conversations.single().latestCallback?.apply {
                onTextChunk("ok")
                onDone()
            }
            advanceUntilIdle()
            assertTrue(job.isCompleted)
            assertEquals(0, engine.closeCount)

            client.invalidate(MODEL_A)

            assertEquals(1, engine.closeCount)
        }

    @Test
    fun `invalidate during engine initialization marks newly created engine for deferred close after generation`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            lateinit var client: LiteRtLmEngineClientImpl
            factory.onCreateAndInitialize = { modelPath ->
                client.invalidate(modelPath)
            }
            client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val result = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p1")
            }
            runCurrent()

            val engine1 = factory.createdEngines.single()
            assertEquals(0, engine1.closeCount)
            val conv1 = engine1.conversations.single()
            conv1.latestCallback?.apply {
                onTextChunk("init-race")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("init-race", result.value)
            assertEquals(1, conv1.closeCount)
            assertEquals(1, engine1.closeCount)

            // Engine was not retained in cache; next generate creates a new one.
            factory.onCreateAndInitialize = null
            val nextResult = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "p2")
            }
            runCurrent()
            assertEquals(2, factory.createdEngines.size)
            factory.createdEngines[1].conversations.single().latestCallback?.apply {
                onTextChunk("next")
                onDone()
            }
            advanceUntilIdle()
            assertEquals("next", nextResult.value)
        }

    @Test
    fun `second generate waits on mutex until cancelled first generate drains and closes conversation`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            val job1 = launch {
                client.generate(modelPath = MODEL_A, prompt = "first")
            }
            runCurrent()

            val engine = factory.createdEngines.single()
            val conv1 = engine.conversations.single()

            job1.cancel()
            runCurrent()
            assertEquals(1, conv1.cancelProcessCount)
            assertEquals(0, conv1.closeCount)

            val job2Result = launchAndCaptureResult {
                client.generate(modelPath = MODEL_A, prompt = "second")
            }
            runCurrent()

            // Job 2 must still be waiting on mutex while job 1 drains.
            assertEquals(1, engine.conversations.size)

            conv1.latestCallback?.onError(IllegalStateException("Cancelled"))
            runCurrent()

            assertTrue(job1.isCancelled)
            assertEquals(1, conv1.closeCount)
            assertEquals(2, engine.conversations.size)

            val conv2 = engine.conversations[1]
            conv2.latestCallback?.apply {
                onTextChunk("second-ok")
                onDone()
            }
            advanceUntilIdle()

            assertEquals("second-ok", job2Result.value)
            assertEquals(1, conv2.closeCount)
            assertEquals(1, factory.createdEngines.size)
        }

    @Test
    fun `createConversation failure closes invalidated engine in finally`() =
        runTest(StandardTestDispatcher()) {
            val factory = FakeLiteRtNativeEngineFactory()
            factory.nextEngineCustomizer = { engine ->
                engine.createConversationThrow = IllegalStateException("createConversation failed")
            }
            val client = LiteRtLmEngineClientImpl(engineFactory = factory)

            var caught: Throwable? = null
            val job = launch {
                try {
                    client.generate(modelPath = MODEL_A, prompt = "p")
                } catch (t: Throwable) {
                    caught = t
                }
            }
            advanceUntilIdle()

            assertTrue(job.isCompleted)
            assertTrue(caught is IllegalStateException)
            val engine = factory.createdEngines.single()
            assertEquals(1, engine.closeCount)
        }

    private fun TestScope.launchAndCaptureResult(
        block: suspend () -> String
    ): ResultHolder {
        val holder = ResultHolder()
        launch {
            holder.value = block()
        }
        return holder
    }

    private class ResultHolder {
        var value: String? = null
    }

    private class FakeLiteRtNativeEngineFactory : LiteRtNativeEngineFactory {
        val createdEngines = mutableListOf<FakeManagedLiteRtEngine>()
        var sharedEventLog: MutableList<String>? = null
        var onCreateAndInitialize: ((String) -> Unit)? = null
        var nextEngineCustomizer: ((FakeManagedLiteRtEngine) -> Unit)? = null
        var nextConversationCustomizer: ((FakeManagedLiteRtConversation) -> Unit)? = null

        override fun createAndInitialize(modelPath: String): ManagedLiteRtEngine {
            onCreateAndInitialize?.invoke(modelPath)
            val engine = FakeManagedLiteRtEngine(
                sharedEventLog = sharedEventLog,
                conversationCustomizer = nextConversationCustomizer
            )
            nextEngineCustomizer?.invoke(engine)
            createdEngines.add(engine)
            return engine
        }
    }

    private class FakeManagedLiteRtEngine(
        private val sharedEventLog: MutableList<String>?,
        private val conversationCustomizer: ((FakeManagedLiteRtConversation) -> Unit)?
    ) : ManagedLiteRtEngine {
        var initialized: Boolean = true
        var createConversationThrow: Throwable? = null
        var closeCount: Int = 0
        val conversations = mutableListOf<FakeManagedLiteRtConversation>()
        val events = mutableListOf<String>()

        override fun isInitialized(): Boolean = initialized

        override fun createConversation(
            conversationSpec: LiteRtRuntimeConfig.ConversationSpec
        ): ManagedLiteRtConversation {
            createConversationThrow?.let { throw it }
            val conversation = FakeManagedLiteRtConversation(sharedEventLog)
            conversationCustomizer?.invoke(conversation)
            conversations.add(conversation)
            return conversation
        }

        override fun close() {
            closeCount++
            events.add("ENGINE_CLOSE")
            sharedEventLog?.add("ENGINE_CLOSE")
        }
    }

    private class FakeManagedLiteRtConversation(
        private val sharedEventLog: MutableList<String>?
    ) : ManagedLiteRtConversation {
        var sendAsyncThrow: Throwable? = null
        var latestCallback: LiteRtConversationCallback? = null
        var cancelProcessCount: Int = 0
        var closeCount: Int = 0
        val events = mutableListOf<String>()

        override fun sendMessageAsync(
            prompt: String,
            conversationSpec: LiteRtRuntimeConfig.ConversationSpec,
            callback: LiteRtConversationCallback
        ) {
            events.add("SEND_ASYNC")
            sharedEventLog?.add("SEND_ASYNC")
            sendAsyncThrow?.let { throw it }
            latestCallback = callback
        }

        override fun cancelProcess() {
            cancelProcessCount++
            events.add("CANCEL_PROCESS")
            sharedEventLog?.add("CANCEL_PROCESS")
        }

        override fun close() {
            closeCount++
            events.add("CONVERSATION_CLOSE")
            sharedEventLog?.add("CONVERSATION_CLOSE")
        }
    }

    private companion object {
        private const val MODEL_A = "/models/a.litertlm"
        private const val MODEL_B = "/models/b.litertlm"
    }
}
