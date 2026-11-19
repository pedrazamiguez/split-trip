package es.pedrazamiguez.splittrip.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlayAppUpdateClientImplTest {

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var client: PlayAppUpdateClientImpl

    @BeforeEach
    fun setUp() {
        appUpdateManager = mockk()
        launcher = mockk(relaxed = true)
        client = PlayAppUpdateClientImpl(appUpdateManager)
    }

    private fun <T> completedTask(value: T): Task<T> = mockk {
        every { isComplete } returns true
        every { isSuccessful } returns true
        every { result } returns value
    }

    private fun <T> failedTask(error: Exception): Task<T> = mockk {
        every { isComplete } returns true
        every { isSuccessful } returns false
        every { exception } returns error
    }

    private fun mockAppUpdateInfo(
        availability: Int = UpdateAvailability.UPDATE_AVAILABLE,
        versionCode: Int = 1_001_000,
        immediateAllowed: Boolean = true,
        flexibleAllowed: Boolean = false,
        installStatus: Int = InstallStatus.UNKNOWN
    ): AppUpdateInfo = mockk {
        every { updateAvailability() } returns availability
        every { availableVersionCode() } returns versionCode
        every { isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) } returns immediateAllowed
        every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns flexibleAllowed
        every { installStatus() } returns installStatus
    }

    @Test
    fun `fetchUpdateSnapshot maps all Play availability, allowed types, versionCode, and install status fields`() =
        runTest {
            val cases = listOf(
                UpdateAvailability.UPDATE_AVAILABLE to PlayUpdateAvailability.UPDATE_AVAILABLE,
                UpdateAvailability.UPDATE_NOT_AVAILABLE to PlayUpdateAvailability.UPDATE_NOT_AVAILABLE,
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS to
                    PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                UpdateAvailability.UNKNOWN to PlayUpdateAvailability.UNKNOWN
            )

            for ((playAvailability, expectedAvailability) in cases) {
                val info = mockAppUpdateInfo(
                    availability = playAvailability,
                    versionCode = 1_002_003,
                    immediateAllowed = true,
                    flexibleAllowed = true,
                    installStatus = InstallStatus.DOWNLOADED
                )
                every { appUpdateManager.appUpdateInfo } returns completedTask(info)

                val result = client.fetchUpdateSnapshot()

                assertTrue(result.isSuccess)
                assertEquals(
                    PlayUpdateSnapshot(
                        availability = expectedAvailability,
                        availableVersionCode = 1_002_003L,
                        isImmediateAllowed = true,
                        isFlexibleAllowed = true,
                        installStatus = PlayInstallStatus.DOWNLOADED
                    ),
                    result.getOrThrow()
                )
            }
        }

    @Test
    fun `fetchUpdateSnapshot returns failure when Play task fails and rethrows CancellationException`() = runTest {
        val error = IllegalStateException("ERROR_APP_NOT_OWNED")
        every { appUpdateManager.appUpdateInfo } returns failedTask(error)

        val result = client.fetchUpdateSnapshot()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("ERROR_APP_NOT_OWNED", result.exceptionOrNull()?.message)

        every { appUpdateManager.appUpdateInfo } returns failedTask(CancellationException("cancelled"))
        assertThrows<CancellationException> {
            client.fetchUpdateSnapshot()
        }
    }

    @Test
    fun `startUpdateFlow launches IMMEDIATE and FLEXIBLE flows using cached AppUpdateInfo and clears cache`() =
        runTest {
            val info = mockAppUpdateInfo(immediateAllowed = true, flexibleAllowed = true)
            every { appUpdateManager.appUpdateInfo } returns completedTask(info)
            every {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                )
            } returns true
            every {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                )
            } returns false

            client.fetchUpdateSnapshot()
            val immediateResult = client.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher)
            assertEquals(Result.success(true), immediateResult)

            // Cache cleared — immediate second call without refetch fails.
            val secondWithoutRefetch = client.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher)
            assertTrue(secondWithoutRefetch.isFailure)

            // Refetch and launch FLEXIBLE.
            client.fetchUpdateSnapshot()
            val flexibleResult = client.startUpdateFlow(PlayUpdateFlowType.FLEXIBLE, launcher)
            assertEquals(Result.success(false), flexibleResult)
        }

    @Test
    fun `startUpdateFlow returns failure when no AppUpdateInfo has been fetched yet`() {
        val result = client.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `startUpdateFlow returns failure and clears cache when AppUpdateManager throws`() = runTest {
        val info = mockAppUpdateInfo()
        every { appUpdateManager.appUpdateInfo } returns completedTask(info)
        every {
            appUpdateManager.startUpdateFlowForResult(info, launcher, any<AppUpdateOptions>())
        } throws IllegalStateException("IntentSender failed")

        client.fetchUpdateSnapshot()
        val firstAttempt = client.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher)
        assertTrue(firstAttempt.isFailure)

        // Cache must be cleared in finally block.
        val secondAttempt = client.startUpdateFlow(PlayUpdateFlowType.IMMEDIATE, launcher)
        assertTrue(secondAttempt.isFailure)
        verify(exactly = 1) {
            appUpdateManager.startUpdateFlowForResult(info, launcher, any<AppUpdateOptions>())
        }
    }

    @Test
    fun `completeFlexibleUpdate succeeds when Play task completes, fails on error, and rethrows cancellation`() =
        runTest {
            every { appUpdateManager.completeUpdate() } returns completedTask(mockk<Void>())
            assertTrue(client.completeFlexibleUpdate().isSuccess)

            val error = IllegalStateException("completeUpdate failed")
            every { appUpdateManager.completeUpdate() } returns failedTask(error)
            val failedResult = client.completeFlexibleUpdate()
            assertFalse(failedResult.isSuccess)
            assertTrue(failedResult.exceptionOrNull() is IllegalStateException)
            assertEquals("completeUpdate failed", failedResult.exceptionOrNull()?.message)

            every { appUpdateManager.completeUpdate() } returns failedTask(CancellationException("cancelled"))
            assertThrows<CancellationException> {
                client.completeFlexibleUpdate()
            }
        }

    @Test
    fun `registerInstallStatusListener maps every Play InstallStatus and unregisters on cleanup`() {
        val listenerSlot = slot<InstallStateUpdatedListener>()
        every { appUpdateManager.registerListener(capture(listenerSlot)) } just runs
        every { appUpdateManager.unregisterListener(any()) } just runs

        val observed = mutableListOf<PlayInstallStatus>()
        val unregister = client.registerInstallStatusListener { observed.add(it) }

        val statuses = listOf(
            InstallStatus.PENDING,
            InstallStatus.DOWNLOADING,
            InstallStatus.DOWNLOADED,
            InstallStatus.INSTALLING,
            InstallStatus.INSTALLED,
            InstallStatus.FAILED,
            InstallStatus.CANCELED,
            InstallStatus.UNKNOWN
        )
        statuses.forEach { playStatus ->
            val state = mockk<InstallState> {
                every { installStatus() } returns playStatus
            }
            listenerSlot.captured.onStateUpdate(state)
        }

        assertEquals(
            listOf(
                PlayInstallStatus.PENDING,
                PlayInstallStatus.DOWNLOADING,
                PlayInstallStatus.DOWNLOADED,
                PlayInstallStatus.INSTALLING,
                PlayInstallStatus.INSTALLED,
                PlayInstallStatus.FAILED,
                PlayInstallStatus.CANCELED,
                PlayInstallStatus.UNKNOWN
            ),
            observed
        )

        unregister()
        verify(exactly = 1) { appUpdateManager.unregisterListener(listenerSlot.captured) }
    }
}
