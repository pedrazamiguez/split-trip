package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.repository.DeviceRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.RegisterDeviceTokenUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RegisterDeviceTokenUseCase")
class RegisterDeviceTokenUseCaseTest {

    private lateinit var deviceRepository: DeviceRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var useCase: RegisterDeviceTokenUseCase

    private val deviceToken = "device-token-abc"

    @BeforeEach
    fun setUp() {
        deviceRepository = mockk()
        notificationRepository = mockk()
        useCase = RegisterDeviceTokenUseCaseImpl(
            deviceRepository = deviceRepository,
            notificationRepository = notificationRepository
        )
    }

    @Nested
    @DisplayName("no pending token")
    inner class NoPendingToken {

        @Test
        fun `registers fresh token when no pending token exists`() = runTest {
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(null)
            coEvery { deviceRepository.getDeviceToken() } returns Result.success(deviceToken)
            coEvery { notificationRepository.registerDeviceTokenWithRetry(deviceToken) } just Runs

            val result = useCase()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { notificationRepository.registerDeviceTokenWithRetry(deviceToken) }
            coVerify(exactly = 0) { notificationRepository.registerDeviceToken(any()) }
        }
    }

    @Nested
    @DisplayName("pending token matches current")
    inner class PendingTokenMatchesCurrent {

        @Test
        fun `syncs pending token and skips redundant registration when current matches`() = runTest {
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(deviceToken)
            coEvery { notificationRepository.registerDeviceToken(deviceToken) } just Runs
            coEvery { deviceRepository.getDeviceToken() } returns Result.success(deviceToken)

            val result = useCase()

            assertTrue(result.isSuccess)
            // Pending token was synced
            coVerify(exactly = 1) { notificationRepository.registerDeviceToken(deviceToken) }
            // Redundant retry registration was skipped (current == pending AND sync succeeded)
            coVerify(exactly = 0) { notificationRepository.registerDeviceTokenWithRetry(any()) }
        }

        @Test
        fun `retries registration when pending token matches current but sync failed`() = runTest {
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(deviceToken)
            coEvery { notificationRepository.registerDeviceToken(deviceToken) } throws RuntimeException("Sync error")
            coEvery { deviceRepository.getDeviceToken() } returns Result.success(deviceToken)
            coEvery { notificationRepository.registerDeviceTokenWithRetry(deviceToken) } just Runs

            val result = useCase()

            assertTrue(result.isSuccess)
            // Pending sync was attempted but failed
            coVerify(exactly = 1) { notificationRepository.registerDeviceToken(deviceToken) }
            // Falls through to retry registration because pending sync failed
            coVerify(exactly = 1) { notificationRepository.registerDeviceTokenWithRetry(deviceToken) }
        }
    }

    @Nested
    @DisplayName("pending token differs from current")
    inner class PendingTokenDiffersCurrent {

        @Test
        fun `syncs pending token and registers fresh token when they differ`() = runTest {
            val pendingToken = "old-pending-token"
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(pendingToken)
            coEvery { notificationRepository.registerDeviceToken(pendingToken) } just Runs
            coEvery { deviceRepository.getDeviceToken() } returns Result.success(deviceToken)
            coEvery { notificationRepository.registerDeviceTokenWithRetry(deviceToken) } just Runs

            val result = useCase()

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { notificationRepository.registerDeviceToken(pendingToken) }
            coVerify(exactly = 1) { notificationRepository.registerDeviceTokenWithRetry(deviceToken) }
        }
    }

    @Nested
    @DisplayName("failure paths")
    inner class FailurePaths {

        @Test
        fun `returns failure when getDeviceToken fails`() = runTest {
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(null)
            coEvery { deviceRepository.getDeviceToken() } returns Result.failure(
                RuntimeException("FCM unavailable")
            )

            val result = useCase()

            assertTrue(result.isFailure)
            assertEquals("FCM unavailable", result.exceptionOrNull()?.message)
        }

        @Test
        fun `does not call registerDeviceTokenWithRetry when getDeviceToken fails`() = runTest {
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(null)
            coEvery { deviceRepository.getDeviceToken() } returns Result.failure(
                RuntimeException("FCM unavailable")
            )

            useCase()

            coVerify(exactly = 0) { notificationRepository.registerDeviceTokenWithRetry(any()) }
        }

        @Test
        fun `proceeds with fresh token even when pending token sync fails`() = runTest {
            val pendingToken = "old-pending-token"
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(pendingToken)
            coEvery { notificationRepository.registerDeviceToken(pendingToken) } throws RuntimeException("Sync error")
            coEvery { deviceRepository.getDeviceToken() } returns Result.success(deviceToken)
            coEvery { notificationRepository.registerDeviceTokenWithRetry(deviceToken) } just Runs

            val result = useCase()

            assertTrue(result.isSuccess)
            // Pending sync failed but fresh token was still registered
            coVerify(exactly = 1) { notificationRepository.registerDeviceTokenWithRetry(deviceToken) }
        }

        @Test
        fun `returns failure when registerDeviceTokenWithRetry throws`() = runTest {
            every { notificationRepository.getPendingTokenFlow() } returns flowOf(null)
            coEvery { deviceRepository.getDeviceToken() } returns Result.success(deviceToken)
            coEvery {
                notificationRepository.registerDeviceTokenWithRetry(deviceToken)
            } throws RuntimeException("Network error")

            val result = useCase()

            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }
    }
}
