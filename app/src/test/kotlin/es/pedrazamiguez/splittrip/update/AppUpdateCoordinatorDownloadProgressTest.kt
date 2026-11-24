package es.pedrazamiguez.splittrip.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import es.pedrazamiguez.splittrip.core.common.provider.AppMetadataProvider
import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.model.ForcedUpdateReason
import es.pedrazamiguez.splittrip.domain.model.SemanticVersion
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.update.RecordOptionalUpdateDismissalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.update.ResolveAppUpdateDecisionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppUpdateCoordinatorDownloadProgressTest {

    private lateinit var playAppUpdateClient: PlayAppUpdateClient
    private lateinit var resolveAppUpdateDecisionUseCase: ResolveAppUpdateDecisionUseCase
    private lateinit var recordOptionalUpdateDismissalUseCase: RecordOptionalUpdateDismissalUseCase
    private lateinit var appConfigService: AppConfigService
    private lateinit var appMetadataProvider: AppMetadataProvider
    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>

    @BeforeEach
    fun setUp() {
        playAppUpdateClient = mockk()
        resolveAppUpdateDecisionUseCase = mockk()
        recordOptionalUpdateDismissalUseCase = mockk()
        appConfigService = mockk()
        appMetadataProvider = mockk()
        launcher = mockk(relaxed = true)

        every { appConfigService.isInitialFetchSettled } returns MutableStateFlow(true)
        every { appMetadataProvider.isEmulator } returns false
        every { appMetadataProvider.appVersionCode } returns INSTALLED_VERSION_CODE
    }

    private fun createCoordinator(): AppUpdateCoordinatorImpl = AppUpdateCoordinatorImpl(
        playAppUpdateClient = playAppUpdateClient,
        resolveAppUpdateDecisionUseCase = resolveAppUpdateDecisionUseCase,
        recordOptionalUpdateDismissalUseCase = recordOptionalUpdateDismissalUseCase,
        appConfigService = appConfigService,
        appMetadataProvider = appMetadataProvider,
        isDebugBuild = false,
        remoteConfigSettleTimeoutMs = 50L
    )

    private fun snapshot(
        availability: PlayUpdateAvailability = PlayUpdateAvailability.UPDATE_AVAILABLE,
        availableVersionCode: Long = AVAILABLE_VERSION_CODE,
        isImmediateAllowed: Boolean = true,
        isFlexibleAllowed: Boolean = true,
        installStatus: PlayInstallStatus = PlayInstallStatus.UNKNOWN,
        bytesDownloaded: Long = 0L,
        totalBytesToDownload: Long = 0L
    ) = PlayUpdateSnapshot(
        availability = availability,
        availableVersionCode = availableVersionCode,
        isImmediateAllowed = isImmediateAllowed,
        isFlexibleAllowed = isFlexibleAllowed,
        installStatus = installStatus,
        bytesDownloaded = bytesDownloaded,
        totalBytesToDownload = totalBytesToDownload
    )

    @Test
    fun `transitions through PENDING and DOWNLOADING to ReadyToInstall and resets`() {
        val listenerSlot = slot<(PlayInstallState) -> Unit>()
        val unregisterMock: () -> Unit = mockk(relaxed = true)
        every { playAppUpdateClient.registerInstallStatusListener(capture(listenerSlot)) } returns unregisterMock
        val coordinator = createCoordinator()

        val unregister = coordinator.registerInstallListener()

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.PENDING, 50L, 100L))
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(0L, 0L), coordinator.uiState.value)
        val pendingState = coordinator.uiState.value as AppUpdateUiState.FlexibleUpdateDownloading
        assertEquals(null, pendingState.progressFraction)
        assertEquals(null, pendingState.progressPercent)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADING, 0L, 0L))
        val unknownTotalState = coordinator.uiState.value as AppUpdateUiState.FlexibleUpdateDownloading
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(0L, 0L), unknownTotalState)
        assertEquals(null, unknownTotalState.progressFraction)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADING, 450L, 1_000L))
        val determinateState = coordinator.uiState.value as AppUpdateUiState.FlexibleUpdateDownloading
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(450L, 1_000L), determinateState)
        assertEquals(0.45f, determinateState.progressFraction)
        assertEquals(45, determinateState.progressPercent)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADED, 1_000L, 1_000L))
        assertEquals(AppUpdateUiState.FlexibleUpdateReadyToInstall, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.INSTALLED, 1_000L, 1_000L))
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADING, 200L, 1_000L))
        listenerSlot.captured(PlayInstallState(PlayInstallStatus.FAILED, 200L, 1_000L))
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADING, 200L, 1_000L))
        listenerSlot.captured(PlayInstallState(PlayInstallStatus.CANCELED, 200L, 1_000L))
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        unregister()
        verify(exactly = 1) { unregisterMock() }
    }

    @Test
    fun `ignores install listener updates when ForcedUpdateBlocked or IMMEDIATE flow is active`() = runTest {
        val listenerSlot = slot<(PlayInstallState) -> Unit>()
        every { playAppUpdateClient.registerInstallStatusListener(capture(listenerSlot)) } returns {}

        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Forced(
            AVAILABLE_VERSION_CODE,
            SemanticVersion(2, 0, 0),
            ForcedUpdateReason.MAJOR_VERSION_UPDATE
        )
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(isImmediateAllowed = false)
        )
        val blockedCoordinator = createCoordinator()
        blockedCoordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
        blockedCoordinator.registerInstallListener()

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.PENDING))
        assertTrue(blockedCoordinator.uiState.value is AppUpdateUiState.ForcedUpdateBlocked)
        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADING, 500L, 1_000L))
        assertTrue(blockedCoordinator.uiState.value is AppUpdateUiState.ForcedUpdateBlocked)
        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADED, 1_000L, 1_000L))
        assertTrue(blockedCoordinator.uiState.value is AppUpdateUiState.ForcedUpdateBlocked)

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
            Result.success(true)
        val immediateCoordinator = createCoordinator()
        immediateCoordinator.registerInstallListener()
        immediateCoordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)

        listenerSlot.captured(PlayInstallState(PlayInstallStatus.PENDING))
        assertEquals(AppUpdateUiState.Idle, immediateCoordinator.uiState.value)
        listenerSlot.captured(PlayInstallState(PlayInstallStatus.DOWNLOADING, 500L, 1_000L))
        assertEquals(AppUpdateUiState.Idle, immediateCoordinator.uiState.value)
    }

    @Test
    fun `checkForUpdates surfaces FlexibleUpdateDownloading without relaunching Play flow`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(installStatus = PlayInstallStatus.PENDING)
        )
        val pendingCoordinator = createCoordinator()
        pendingCoordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(0L, 0L), pendingCoordinator.uiState.value)
        verify(exactly = 0) { playAppUpdateClient.startUpdateFlow(any(), any()) }

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(
                installStatus = PlayInstallStatus.DOWNLOADING,
                bytesDownloaded = 300L,
                totalBytesToDownload = 1_000L
            )
        )
        val downloadingCoordinator = createCoordinator()
        downloadingCoordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        downloadingCoordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        assertEquals(
            AppUpdateUiState.FlexibleUpdateDownloading(300L, 1_000L),
            downloadingCoordinator.uiState.value
        )
        verify(exactly = 0) { playAppUpdateClient.startUpdateFlow(any(), any()) }
        coVerify(exactly = 2) { playAppUpdateClient.fetchUpdateSnapshot() }
    }

    @Test
    fun `onResume surfaces FlexibleUpdateDownloading on PENDING and DOWNLOADING`() = runTest {
        val coordinator = createCoordinator()

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(
                availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = PlayInstallStatus.PENDING
            )
        )
        coordinator.onResume(launcher)
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(0L, 0L), coordinator.uiState.value)

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(
                availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = PlayInstallStatus.DOWNLOADING,
                bytesDownloaded = 600L,
                totalBytesToDownload = 1_000L
            )
        )
        coordinator.onResume(launcher)
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(600L, 1_000L), coordinator.uiState.value)

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(
                availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = PlayInstallStatus.UNKNOWN
            )
        )
        coordinator.onResume(launcher)
        assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(600L, 1_000L), coordinator.uiState.value)

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(
                availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = PlayInstallStatus.DOWNLOADED,
                bytesDownloaded = 1_000L,
                totalBytesToDownload = 1_000L
            )
        )
        coordinator.onResume(launcher)
        assertEquals(AppUpdateUiState.FlexibleUpdateReadyToInstall, coordinator.uiState.value)
    }

    @Test
    fun `onResume resets stale downloading state on terminal statuses`() = runTest {
        val coordinator = createCoordinator()
        val terminalStatuses = listOf(
            PlayInstallStatus.FAILED,
            PlayInstallStatus.CANCELED,
            PlayInstallStatus.INSTALLED
        )
        for (terminalStatus in terminalStatuses) {
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(
                    availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                    installStatus = PlayInstallStatus.DOWNLOADING,
                    bytesDownloaded = 600L,
                    totalBytesToDownload = 1_000L
                )
            )
            coordinator.onResume(launcher)
            assertEquals(AppUpdateUiState.FlexibleUpdateDownloading(600L, 1_000L), coordinator.uiState.value)

            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(
                    availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                    installStatus = terminalStatus
                )
            )
            coordinator.onResume(launcher)
            assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
        }
    }

    private companion object {
        const val INSTALLED_VERSION_CODE = 1_000_000L
        const val AVAILABLE_VERSION_CODE = 1_001_000L
    }
}
