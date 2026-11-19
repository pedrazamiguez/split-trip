package es.pedrazamiguez.splittrip.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.install.model.ActivityResult as PlayActivityResult
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
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppUpdateCoordinatorImplTest {

    private lateinit var playAppUpdateClient: PlayAppUpdateClient
    private lateinit var resolveAppUpdateDecisionUseCase: ResolveAppUpdateDecisionUseCase
    private lateinit var recordOptionalUpdateDismissalUseCase: RecordOptionalUpdateDismissalUseCase
    private lateinit var appConfigService: AppConfigService
    private lateinit var appMetadataProvider: AppMetadataProvider
    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var initialFetchSettledFlow: MutableStateFlow<Boolean>

    @BeforeEach
    fun setUp() {
        playAppUpdateClient = mockk()
        resolveAppUpdateDecisionUseCase = mockk()
        recordOptionalUpdateDismissalUseCase = mockk()
        appConfigService = mockk()
        appMetadataProvider = mockk()
        launcher = mockk(relaxed = true)

        initialFetchSettledFlow = MutableStateFlow(true)
        every { appConfigService.isInitialFetchSettled } returns initialFetchSettledFlow
        every { appMetadataProvider.isEmulator } returns false
        every { appMetadataProvider.appVersionCode } returns INSTALLED_VERSION_CODE
    }

    private fun createCoordinator(
        isDebugBuild: Boolean = false,
        settleTimeoutMs: Long = 50L
    ): AppUpdateCoordinatorImpl = AppUpdateCoordinatorImpl(
        playAppUpdateClient = playAppUpdateClient,
        resolveAppUpdateDecisionUseCase = resolveAppUpdateDecisionUseCase,
        recordOptionalUpdateDismissalUseCase = recordOptionalUpdateDismissalUseCase,
        appConfigService = appConfigService,
        appMetadataProvider = appMetadataProvider,
        isDebugBuild = isDebugBuild,
        remoteConfigSettleTimeoutMs = settleTimeoutMs
    )

    private fun snapshot(
        availability: PlayUpdateAvailability = PlayUpdateAvailability.UPDATE_AVAILABLE,
        availableVersionCode: Long = AVAILABLE_VERSION_CODE,
        isImmediateAllowed: Boolean = true,
        isFlexibleAllowed: Boolean = true,
        installStatus: PlayInstallStatus = PlayInstallStatus.UNKNOWN
    ) = PlayUpdateSnapshot(
        availability = availability,
        availableVersionCode = availableVersionCode,
        isImmediateAllowed = isImmediateAllowed,
        isFlexibleAllowed = isFlexibleAllowed,
        installStatus = installStatus
    )

    @Test
    fun `skips update checks in debug builds without querying Play`() = runTest {
        val coordinator = AppUpdateCoordinatorImpl(
            playAppUpdateClient = playAppUpdateClient,
            resolveAppUpdateDecisionUseCase = resolveAppUpdateDecisionUseCase,
            recordOptionalUpdateDismissalUseCase = recordOptionalUpdateDismissalUseCase,
            appConfigService = appConfigService,
            appMetadataProvider = appMetadataProvider,
            isDebugBuild = true
        )

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.onResume(launcher)
        coordinator.retryForcedUpdate(launcher)

        coVerify(exactly = 0) { playAppUpdateClient.fetchUpdateSnapshot() }
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `skips update checks on emulators without querying Play`() = runTest {
        every { appMetadataProvider.isEmulator } returns true
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.onResume(launcher)

        coVerify(exactly = 0) { playAppUpdateClient.fetchUpdateSnapshot() }
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `fails open silently to Idle when Play fetchUpdateSnapshot fails`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns
            Result.failure(IllegalStateException("ERROR_APP_NOT_OWNED"))
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.onResume(launcher)

        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `fails open when Remote Config settle times out and evaluates cached policy`() = runTest {
        initialFetchSettledFlow.value = false
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Optional(AVAILABLE_VERSION_CODE, SemanticVersion(1, 1, 0))
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) } returns
            Result.success(true)
        val coordinator = createCoordinator(settleTimeoutMs = 10L)

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)

        verify(exactly = 1) { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) }
    }

    @Test
    fun `launches IMMEDIATE flow immediately on Forced decision even when allowOptionalPrompt is false`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Forced(
            availableVersionCode = AVAILABLE_VERSION_CODE,
            availableVersion = SemanticVersion(1, 0, 1),
            reason = ForcedUpdateReason.PATCH_HOTFIX_UPDATE
        )
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
            Result.success(true)
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)

        verify(exactly = 1) { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) }
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `shows ForcedUpdateBlocked gate when Forced update is confirmed but IMMEDIATE flow is not allowed or fails`() =
        runTest {
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(isImmediateAllowed = false)
            )
            coEvery {
                resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
            } returns AppUpdateDecision.Forced(
                availableVersionCode = AVAILABLE_VERSION_CODE,
                availableVersion = SemanticVersion(1, 0, 1),
                reason = ForcedUpdateReason.PATCH_HOTFIX_UPDATE
            )
            val coordinatorNotAllowed = createCoordinator()

            coordinatorNotAllowed.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)

            assertEquals(
                AppUpdateUiState.ForcedUpdateBlocked(AVAILABLE_VERSION_CODE, isRetrying = false),
                coordinatorNotAllowed.uiState.value
            )

            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(isImmediateAllowed = true)
            )
            every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
                Result.failure(RuntimeException("Play failure"))
            val coordinatorStartFailed = createCoordinator()

            coordinatorStartFailed.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)

            assertEquals(
                AppUpdateUiState.ForcedUpdateBlocked(AVAILABLE_VERSION_CODE, isRetrying = false),
                coordinatorStartFailed.uiState.value
            )
        }

    @Test
    fun `defers Optional prompt when allowOptionalPrompt is false and launches FLEXIBLE once true`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Optional(AVAILABLE_VERSION_CODE, SemanticVersion(1, 1, 0))
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) } returns
            Result.success(true)
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
        verify(exactly = 0) { playAppUpdateClient.startUpdateFlow(any(), any()) }

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        verify(exactly = 1) { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) }
    }

    @Test
    fun `does not re-query Play for optional prompt once optional check completed in current session`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE)
        )
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)

        coVerify(exactly = 1) { playAppUpdateClient.fetchUpdateSnapshot() }
    }

    @Test
    fun `records dismissal cooldown when user cancels FLEXIBLE flow`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Optional(AVAILABLE_VERSION_CODE, SemanticVersion(1, 1, 0))
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) } returns
            Result.success(true)
        coEvery { recordOptionalUpdateDismissalUseCase(AVAILABLE_VERSION_CODE) } just runs
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.onActivityResult(Activity.RESULT_CANCELED)

        coVerify(exactly = 1) { recordOptionalUpdateDismissalUseCase(AVAILABLE_VERSION_CODE) }
    }

    @Test
    fun `does not record dismissal cooldown when FLEXIBLE flow fails technically`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Optional(AVAILABLE_VERSION_CODE, SemanticVersion(1, 1, 0))
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) } returns
            Result.success(true)
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.onActivityResult(PlayActivityResult.RESULT_IN_APP_UPDATE_FAILED)

        coVerify(exactly = 0) { recordOptionalUpdateDismissalUseCase(any()) }
    }

    @Test
    fun `fails open if recording optional update dismissal throws`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Optional(AVAILABLE_VERSION_CODE, SemanticVersion(1, 1, 0))
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher) } returns
            Result.success(true)
        coEvery { recordOptionalUpdateDismissalUseCase(AVAILABLE_VERSION_CODE) } throws
            IllegalStateException("Disk full")
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
        coordinator.onActivityResult(Activity.RESULT_CANCELED)

        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `transitions to ForcedUpdateBlocked when user cancels IMMEDIATE flow and clears on RESULT_OK`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Forced(
            availableVersionCode = AVAILABLE_VERSION_CODE,
            availableVersion = SemanticVersion(2, 0, 0),
            reason = ForcedUpdateReason.MAJOR_VERSION_UPDATE
        )
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
            Result.success(true)
        val coordinator = createCoordinator()

        coordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
        coordinator.onActivityResult(Activity.RESULT_CANCELED)

        assertEquals(
            AppUpdateUiState.ForcedUpdateBlocked(AVAILABLE_VERSION_CODE, isRetrying = false),
            coordinator.uiState.value
        )

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coordinator.retryForcedUpdate(launcher)
        coordinator.onActivityResult(Activity.RESULT_OK)

        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `transitions to FlexibleUpdateReadyToInstall on DOWNLOADED and resets on INSTALLED FAILED CANCELED`() {
        val listenerSlot = slot<(PlayInstallStatus) -> Unit>()
        val unregisterMock: () -> Unit = mockk(relaxed = true)
        every { playAppUpdateClient.registerInstallStatusListener(capture(listenerSlot)) } returns unregisterMock
        val coordinator = createCoordinator()

        val unregister = coordinator.registerInstallListener()
        listenerSlot.captured(PlayInstallStatus.DOWNLOADING)
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallStatus.DOWNLOADED)
        assertEquals(AppUpdateUiState.FlexibleUpdateReadyToInstall, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallStatus.INSTALLED)
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallStatus.DOWNLOADED)
        listenerSlot.captured(PlayInstallStatus.FAILED)
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        listenerSlot.captured(PlayInstallStatus.DOWNLOADED)
        listenerSlot.captured(PlayInstallStatus.CANCELED)
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        unregister()
        verify(exactly = 1) { unregisterMock() }
    }

    @Test
    fun `onResume resumes stalled DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS Immediate flow`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(availability = PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
        )
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
            Result.success(true)
        val coordinator = createCoordinator()

        coordinator.onResume(launcher)

        verify(exactly = 1) { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) }
    }

    @Test
    fun `onResume surfaces FlexibleUpdateReadyToInstall when snapshot installStatus is DOWNLOADED`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(
                availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                installStatus = PlayInstallStatus.DOWNLOADED
            )
        )
        val coordinator = createCoordinator()

        coordinator.onResume(launcher)

        assertEquals(AppUpdateUiState.FlexibleUpdateReadyToInstall, coordinator.uiState.value)
    }

    @Test
    fun `onResume clears ForcedUpdateBlocked gate when update unavailable or killswitch disabled`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(isImmediateAllowed = false)
        )
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Forced(
            AVAILABLE_VERSION_CODE,
            SemanticVersion(2, 0, 0),
            ForcedUpdateReason.MAJOR_VERSION_UPDATE
        )
        val coordinator = createCoordinator()
        coordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
        assertTrue(coordinator.uiState.value is AppUpdateUiState.ForcedUpdateBlocked)

        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.None
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
        coordinator.onResume(launcher)
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)

        // Gate again and verify UPDATE_NOT_AVAILABLE clears it too.
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
        coordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
        assertTrue(coordinator.uiState.value is AppUpdateUiState.ForcedUpdateBlocked)

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(availability = PlayUpdateAvailability.UPDATE_NOT_AVAILABLE)
        )
        coordinator.onResume(launcher)
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `retryForcedUpdate fetches fresh snapshot and relaunches IMMEDIATE flow when allowed`() = runTest {
        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(isImmediateAllowed = false)
        )
        coEvery {
            resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
        } returns AppUpdateDecision.Forced(
            AVAILABLE_VERSION_CODE,
            SemanticVersion(2, 0, 0),
            ForcedUpdateReason.MAJOR_VERSION_UPDATE
        )
        val coordinator = createCoordinator()
        coordinator.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)

        coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
            snapshot(isImmediateAllowed = true)
        )
        every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
            Result.success(true)

        coordinator.retryForcedUpdate(launcher)

        coVerify(exactly = 2) { playAppUpdateClient.fetchUpdateSnapshot() }
        verify(exactly = 1) { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) }
    }

    @Test
    fun `retryForcedUpdate emits OpenPlayStoreListing when retry fails or immediate disallowed`() =
        runTest {
            // 1. Fetch fails on retry
            val coordinatorFetchFail = createBlockedCoordinator()
            val action1 = async(start = CoroutineStart.UNDISPATCHED) {
                coordinatorFetchFail.actions.first()
            }
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns
                Result.failure(IllegalStateException("Network error"))
            coordinatorFetchFail.retryForcedUpdate(launcher)
            assertEquals(AppUpdateCoordinatorAction.OpenPlayStoreListing, action1.await())

            // 2. IMMEDIATE not allowed on retry
            val coordinatorNotAllowed = createBlockedCoordinator()
            val action2 = async(start = CoroutineStart.UNDISPATCHED) {
                coordinatorNotAllowed.actions.first()
            }
            coordinatorNotAllowed.retryForcedUpdate(launcher)
            assertEquals(AppUpdateCoordinatorAction.OpenPlayStoreListing, action2.await())

            // 3. startUpdateFlow fails on retry
            val coordinatorStartFail = createBlockedCoordinator()
            val action3 = async(start = CoroutineStart.UNDISPATCHED) {
                coordinatorStartFail.actions.first()
            }
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(isImmediateAllowed = true)
            )
            every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
                Result.success(false)
            coordinatorStartFail.retryForcedUpdate(launcher)
            assertEquals(AppUpdateCoordinatorAction.OpenPlayStoreListing, action3.await())

            // 4. Retried Play activity returns RESULT_IN_APP_UPDATE_FAILED
            val coordinatorActivityFail = createBlockedCoordinator()
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(isImmediateAllowed = true)
            )
            every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
                Result.success(true)
            coordinatorActivityFail.retryForcedUpdate(launcher)

            val action4 = async(start = CoroutineStart.UNDISPATCHED) {
                coordinatorActivityFail.actions.first()
            }
            coordinatorActivityFail.onActivityResult(PlayActivityResult.RESULT_IN_APP_UPDATE_FAILED)
            assertEquals(AppUpdateCoordinatorAction.OpenPlayStoreListing, action4.await())
        }

    private suspend fun createBlockedCoordinator(): AppUpdateCoordinatorImpl {
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
        return createCoordinator().also {
            it.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
        }
    }

    @Test
    fun `completeFlexibleUpdate delegates to client and resets to Idle on failure`() = runTest {
        val listenerSlot = slot<(PlayInstallStatus) -> Unit>()
        every { playAppUpdateClient.registerInstallStatusListener(capture(listenerSlot)) } returns {}
        coEvery { playAppUpdateClient.completeFlexibleUpdate() } returns
            Result.failure(IllegalStateException("Install error"))
        val coordinator = createCoordinator()

        coordinator.registerInstallListener()
        listenerSlot.captured(PlayInstallStatus.DOWNLOADED)
        assertEquals(AppUpdateUiState.FlexibleUpdateReadyToInstall, coordinator.uiState.value)

        coordinator.completeFlexibleUpdate()

        coVerify(exactly = 1) { playAppUpdateClient.completeFlexibleUpdate() }
        assertEquals(AppUpdateUiState.Idle, coordinator.uiState.value)
    }

    @Test
    fun `handles checkForUpdates DOWNLOADED, DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS, None, and retry idle clears`() =
        runTest {
            // DOWNLOADED in checkForUpdates
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(installStatus = PlayInstallStatus.DOWNLOADED)
            )
            val coordinatorDownloaded = createCoordinator()
            coordinatorDownloaded.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
            assertEquals(AppUpdateUiState.FlexibleUpdateReadyToInstall, coordinatorDownloaded.uiState.value)

            // DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS in checkForUpdates
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(availability = PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
            )
            every { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) } returns
                Result.success(true)
            val coordinatorInProgress = createCoordinator()
            coordinatorInProgress.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
            verify(exactly = 1) { playAppUpdateClient.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher) }

            // AppUpdateDecision.None in checkForUpdates
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(snapshot())
            coEvery {
                resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
            } returns AppUpdateDecision.None
            val coordinatorNone = createCoordinator()
            coordinatorNone.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
            coordinatorNone.checkForUpdates(allowOptionalPrompt = true, launcher = launcher)
            coVerify(exactly = 1) { resolveAppUpdateDecisionUseCase(any(), any()) }

            // retryForcedUpdate when decision becomes None clears gate
            coEvery { playAppUpdateClient.fetchUpdateSnapshot() } returns Result.success(
                snapshot(isImmediateAllowed = false)
            )
            coEvery {
                resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
            } returns AppUpdateDecision.Forced(
                AVAILABLE_VERSION_CODE,
                SemanticVersion(2, 0, 0),
                ForcedUpdateReason.MAJOR_VERSION_UPDATE
            )
            val coordinatorRetryClears = createCoordinator()
            coordinatorRetryClears.checkForUpdates(allowOptionalPrompt = false, launcher = launcher)
            coEvery {
                resolveAppUpdateDecisionUseCase(INSTALLED_VERSION_CODE, AVAILABLE_VERSION_CODE)
            } returns AppUpdateDecision.None
            coordinatorRetryClears.retryForcedUpdate(launcher)
            assertEquals(AppUpdateUiState.Idle, coordinatorRetryClears.uiState.value)
        }

    private companion object {
        const val INSTALLED_VERSION_CODE = 1_000_000L
        const val AVAILABLE_VERSION_CODE = 1_001_000L
    }
}
