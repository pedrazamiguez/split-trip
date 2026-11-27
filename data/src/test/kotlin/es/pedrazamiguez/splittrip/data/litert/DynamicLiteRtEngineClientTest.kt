package es.pedrazamiguez.splittrip.data.litert

import android.content.Context
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DynamicLiteRtEngineClientTest {

    private lateinit var context: Context
    private lateinit var appContext: Context
    private lateinit var runtimeManager: LiteRtDynamicRuntimeManager
    private lateinit var factoryLoader: LiteRtEngineClientFactoryLoader
    private lateinit var factory: LiteRtEngineClientFactory
    private lateinit var delegate: LiteRtEngineClient
    private lateinit var client: DynamicLiteRtEngineClient

    @BeforeEach
    fun setUp() {
        context = mockk()
        appContext = mockk()
        runtimeManager = mockk()
        factoryLoader = mockk()
        factory = mockk()
        delegate = mockk()

        every { context.applicationContext } returns appContext
        every { runtimeManager.isRuntimeInstalled() } returns true
        every { runtimeManager.activateInstalledRuntime() } returns true
        every { factoryLoader.loadFactory() } returns factory
        every { factory.create(appContext) } returns delegate

        client = DynamicLiteRtEngineClient(
            context = context,
            runtimeManager = runtimeManager,
            factoryLoader = factoryLoader
        )
    }

    @Test
    fun `generate resolves factory once, caches delegate, and forwards call`() = runTest {
        coEvery { delegate.generate("/model.litertlm", "p1", null) } returns "r1"
        coEvery { delegate.generate("/model.litertlm", "p2", "sys") } returns "r2"

        val first = client.generate("/model.litertlm", "p1", null)
        val second = client.generate("/model.litertlm", "p2", "sys")

        assertEquals("r1", first)
        assertEquals("r2", second)
        verify(exactly = 1) { factory.create(appContext) }
        coVerify(exactly = 1) { delegate.generate("/model.litertlm", "p1", null) }
        coVerify(exactly = 1) { delegate.generate("/model.litertlm", "p2", "sys") }
    }

    @Test
    fun `generate throws LocalAiModelUnavailableException when runtime is not installed`() = runTest {
        every { runtimeManager.isRuntimeInstalled() } returns false

        assertThrows<LocalAiModelUnavailableException> {
            client.generate("/model.litertlm", "prompt", null)
        }
        verify(exactly = 0) { factoryLoader.loadFactory() }
    }

    @Test
    fun `generate throws LocalAiModelUnavailableException when activateInstalledRuntime returns false`() = runTest {
        every { runtimeManager.activateInstalledRuntime() } returns false

        assertThrows<LocalAiModelUnavailableException> {
            client.generate("/model.litertlm", "prompt", null)
        }
        verify(exactly = 0) { factoryLoader.loadFactory() }
    }

    @Test
    fun `generate throws LocalAiModelUnavailableException when factoryLoader returns null`() = runTest {
        every { factoryLoader.loadFactory() } returns null

        assertThrows<LocalAiModelUnavailableException> {
            client.generate("/model.litertlm", "prompt", null)
        }
    }

    @Test
    fun `invalidate is a no-op when delegate has not been loaded`() {
        client.invalidate("/model.litertlm")

        verify(exactly = 0) { runtimeManager.isRuntimeInstalled() }
        verify(exactly = 0) { factoryLoader.loadFactory() }
    }

    @Test
    fun `invalidate forwards to cached delegate when loaded`() = runTest {
        coEvery { delegate.generate(any(), any(), any()) } returns "ok"
        every { delegate.invalidate("/model.litertlm") } just runs

        client.generate("/model.litertlm", "prompt", null)
        client.invalidate("/model.litertlm")

        verify(exactly = 1) { delegate.invalidate("/model.litertlm") }
    }

    @Test
    fun `close closes cached delegate and clears cache so subsequent generate recreates client`() = runTest {
        coEvery { delegate.generate(any(), any(), any()) } returns "ok"
        every { delegate.close() } just runs

        client.generate("/model.litertlm", "prompt", null)
        client.close()
        client.generate("/model.litertlm", "prompt", null)

        verify(exactly = 1) { delegate.close() }
        verify(exactly = 2) { factory.create(appContext) }
    }
}
