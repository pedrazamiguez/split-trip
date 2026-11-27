package es.pedrazamiguez.splittrip.data.litert

import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LiteRtDynamicRuntimeManagerImplTest {

    private lateinit var splitInstallClient: PlaySplitInstallClient
    private lateinit var factoryLoader: LiteRtEngineClientFactoryLoader
    private val listenerSlot = slot<(SplitInstallSessionSnapshot) -> Unit>()
    private val unregisterLambda = mockk<() -> Unit>(relaxed = true)

    @BeforeEach
    fun setUp() {
        splitInstallClient = mockk(relaxed = true)
        factoryLoader = mockk()
        every { splitInstallClient.getInstalledModules() } returns emptySet()
        every { splitInstallClient.registerListener(capture(listenerSlot)) } returns unregisterLambda
        every { factoryLoader.isFactoryAvailable() } returns false
    }

    private fun createManager(): LiteRtDynamicRuntimeManagerImpl =
        LiteRtDynamicRuntimeManagerImpl(
            splitInstallClient = splitInstallClient,
            factoryLoader = factoryLoader
        )

    @Test
    fun `initial state is Installed when factory loader already finds factory`() {
        every { factoryLoader.isFactoryAvailable() } returns true

        val manager = createManager()

        assertEquals(LiteRtDynamicRuntimeState.Installed, manager.observeState().value)
        assertTrue(manager.isRuntimeInstalled())
    }

    @Test
    fun `initial state is Installed when module is in installedModules and SplitCompat activates factory`() {
        every { factoryLoader.isFactoryAvailable() } returnsMany listOf(false, true)
        every { splitInstallClient.getInstalledModules() } returns setOf(LiteRtDynamicRuntimeManager.MODULE_NAME)
        every { splitInstallClient.installSplitCompat() } returns true

        val manager = createManager()

        assertEquals(LiteRtDynamicRuntimeState.Installed, manager.observeState().value)
        verify(exactly = 1) { splitInstallClient.installSplitCompat() }
    }

    @Test
    fun `initial state is NotInstalled when module is absent and factory is unavailable`() {
        val manager = createManager()

        assertEquals(LiteRtDynamicRuntimeState.NotInstalled, manager.observeState().value)
        assertFalse(manager.isRuntimeInstalled())
    }

    @Test
    fun `requestInstall transitions through Pending, Downloading, Installing, and Installed`() {
        val onSuccessSlot = slot<(Int) -> Unit>()
        every {
            splitInstallClient.startInstall(
                moduleName = LiteRtDynamicRuntimeManager.MODULE_NAME,
                onSuccess = capture(onSuccessSlot),
                onFailure = any()
            )
        } answers { onSuccessSlot.captured(42) }
        every { splitInstallClient.installSplitCompat() } returns true

        val manager = createManager()
        manager.requestInstall()

        assertEquals(LiteRtDynamicRuntimeState.Pending, manager.observeState().value)
        assertNull(manager.observeState().value.toProgressPercent())

        listenerSlot.captured(
            snapshot(status = SplitInstallSessionStatus.PENDING)
        )
        assertEquals(LiteRtDynamicRuntimeState.Pending, manager.observeState().value)

        listenerSlot.captured(
            snapshot(
                status = SplitInstallSessionStatus.DOWNLOADING,
                bytesDownloaded = 5_000_000L,
                totalBytesToDownload = 10_000_000L
            )
        )
        val downloadingState = manager.observeState().value
        assertTrue(downloadingState is LiteRtDynamicRuntimeState.Downloading)
        assertEquals(50, downloadingState.toProgressPercent())

        listenerSlot.captured(snapshot(status = SplitInstallSessionStatus.INSTALLING))
        assertEquals(LiteRtDynamicRuntimeState.Installing, manager.observeState().value)

        every { factoryLoader.isFactoryAvailable() } returns true
        listenerSlot.captured(snapshot(status = SplitInstallSessionStatus.INSTALLED))

        assertEquals(LiteRtDynamicRuntimeState.Installed, manager.observeState().value)
        verify(atLeast = 1) { splitInstallClient.installSplitCompat() }
    }

    @Test
    fun `requestInstall deduplicates concurrent calls while Pending or Downloading`() {
        every {
            splitInstallClient.startInstall(
                moduleName = LiteRtDynamicRuntimeManager.MODULE_NAME,
                onSuccess = any(),
                onFailure = any()
            )
        } just runs

        val manager = createManager()
        manager.requestInstall()
        manager.requestInstall()

        verify(exactly = 1) {
            splitInstallClient.startInstall(
                moduleName = LiteRtDynamicRuntimeManager.MODULE_NAME,
                onSuccess = any(),
                onFailure = any()
            )
        }
    }

    @Test
    fun `requestInstall sets Installed immediately when runtime is already available`() {
        val manager = createManager()
        every { factoryLoader.isFactoryAvailable() } returns true

        manager.requestInstall()

        assertEquals(LiteRtDynamicRuntimeState.Installed, manager.observeState().value)
        verify(exactly = 0) { splitInstallClient.startInstall(any(), any(), any()) }
    }

    @Test
    fun `requestInstall failure maps NETWORK_ERROR to ModelPackErrorReason NETWORK_ERROR`() {
        val onFailureSlot = slot<(Throwable) -> Unit>()
        val exception = mockk<SplitInstallException>()
        every { exception.errorCode } returns SplitInstallErrorCode.NETWORK_ERROR
        every {
            splitInstallClient.startInstall(any(), any(), capture(onFailureSlot))
        } answers { onFailureSlot.captured(exception) }

        val manager = createManager()
        manager.requestInstall()

        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.NETWORK_ERROR,
                errorCode = SplitInstallErrorCode.NETWORK_ERROR
            ),
            manager.observeState().value
        )
    }

    @Test
    fun `requestInstall failure maps INSUFFICIENT_STORAGE to ModelPackErrorReason INSUFFICIENT_STORAGE`() {
        val onFailureSlot = slot<(Throwable) -> Unit>()
        val exception = mockk<SplitInstallException>()
        every { exception.errorCode } returns SplitInstallErrorCode.INSUFFICIENT_STORAGE
        every {
            splitInstallClient.startInstall(any(), any(), capture(onFailureSlot))
        } answers { onFailureSlot.captured(exception) }

        val manager = createManager()
        manager.requestInstall()

        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.INSUFFICIENT_STORAGE,
                errorCode = SplitInstallErrorCode.INSUFFICIENT_STORAGE
            ),
            manager.observeState().value
        )
    }

    @Test
    fun `requestInstall failure maps API_NOT_AVAILABLE and MODULE_UNAVAILABLE to RUNTIME_UNAVAILABLE`() {
        val onFailureSlot = slot<(Throwable) -> Unit>()
        every {
            splitInstallClient.startInstall(any(), any(), capture(onFailureSlot))
        } just runs

        val manager = createManager()

        val apiException = mockk<SplitInstallException>()
        every { apiException.errorCode } returns SplitInstallErrorCode.API_NOT_AVAILABLE
        manager.requestInstall()
        onFailureSlot.captured(apiException)
        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = SplitInstallErrorCode.API_NOT_AVAILABLE
            ),
            manager.observeState().value
        )

        val moduleException = mockk<SplitInstallException>()
        every { moduleException.errorCode } returns SplitInstallErrorCode.MODULE_UNAVAILABLE
        manager.requestInstall()
        onFailureSlot.captured(moduleException)
        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = SplitInstallErrorCode.MODULE_UNAVAILABLE
            ),
            manager.observeState().value
        )
    }

    @Test
    fun `session update ignores unrelated module names`() {
        val manager = createManager()

        listenerSlot.captured(
            snapshot(
                status = SplitInstallSessionStatus.DOWNLOADING,
                moduleNames = listOf("other_module")
            )
        )

        assertEquals(LiteRtDynamicRuntimeState.NotInstalled, manager.observeState().value)
    }

    @Test
    fun `session update INSTALLED transitions to Failed RUNTIME_UNAVAILABLE when factory still cannot be loaded`() {
        val manager = createManager()
        every { splitInstallClient.installSplitCompat() } returns true
        every { factoryLoader.isFactoryAvailable() } returns false

        listenerSlot.captured(snapshot(status = SplitInstallSessionStatus.INSTALLED))

        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = 0
            ),
            manager.observeState().value
        )
    }

    @Test
    fun `session update REQUIRES_USER_CONFIRMATION transitions cleanly to Failed RUNTIME_UNAVAILABLE`() {
        val manager = createManager()

        listenerSlot.captured(snapshot(status = SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION))

        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = 0
            ),
            manager.observeState().value
        )
    }

    @Test
    fun `session update FAILED maps error code and clears active session`() {
        val onSuccessSlot = slot<(Int) -> Unit>()
        every {
            splitInstallClient.startInstall(any(), capture(onSuccessSlot), any())
        } answers { onSuccessSlot.captured(77) }

        val manager = createManager()
        manager.requestInstall()

        listenerSlot.captured(
            snapshot(
                sessionId = 77,
                status = SplitInstallSessionStatus.FAILED,
                errorCode = SplitInstallErrorCode.NETWORK_ERROR
            )
        )

        assertEquals(
            LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.NETWORK_ERROR,
                errorCode = SplitInstallErrorCode.NETWORK_ERROR
            ),
            manager.observeState().value
        )

        manager.cancelInstall()
        verify(exactly = 0) { splitInstallClient.cancelInstall(77) }
    }

    @Test
    fun `session update CANCELED resets state to NotInstalled`() {
        val manager = createManager()
        listenerSlot.captured(snapshot(status = SplitInstallSessionStatus.PENDING))
        assertEquals(LiteRtDynamicRuntimeState.Pending, manager.observeState().value)

        listenerSlot.captured(snapshot(status = SplitInstallSessionStatus.CANCELED))

        assertEquals(LiteRtDynamicRuntimeState.NotInstalled, manager.observeState().value)
    }

    @Test
    fun `cancelInstall cancels active Play session and resets state to NotInstalled`() {
        val onSuccessSlot = slot<(Int) -> Unit>()
        every {
            splitInstallClient.startInstall(any(), capture(onSuccessSlot), any())
        } answers { onSuccessSlot.captured(99) }

        val manager = createManager()
        manager.requestInstall()
        manager.cancelInstall()

        verify(exactly = 1) { splitInstallClient.cancelInstall(99) }
        assertEquals(LiteRtDynamicRuntimeState.NotInstalled, manager.observeState().value)
    }

    @Test
    fun `requestDeferredUninstall delegates to splitInstallClient and ignores exceptions`() {
        every {
            splitInstallClient.deferredUninstall(LiteRtDynamicRuntimeManager.MODULE_NAME)
        } throws IllegalStateException("Play Core error")

        val manager = createManager()
        manager.requestDeferredUninstall()

        verify(exactly = 1) {
            splitInstallClient.deferredUninstall(LiteRtDynamicRuntimeManager.MODULE_NAME)
        }
    }

    @Test
    fun `close unregisters session listener`() {
        val manager = createManager()

        manager.close()

        verify(exactly = 1) { unregisterLambda.invoke() }
    }

    private fun snapshot(
        sessionId: Int = 1,
        status: Int,
        errorCode: Int = 0,
        bytesDownloaded: Long = 0L,
        totalBytesToDownload: Long = 0L,
        moduleNames: List<String> = listOf(LiteRtDynamicRuntimeManager.MODULE_NAME)
    ): SplitInstallSessionSnapshot = SplitInstallSessionSnapshot(
        sessionId = sessionId,
        status = status,
        errorCode = errorCode,
        bytesDownloaded = bytesDownloaded,
        totalBytesToDownload = totalBytesToDownload,
        moduleNames = moduleNames
    )
}
