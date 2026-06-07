package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.UnregisterDeviceTokenUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UnregisterDeviceTokenUseCaseTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var useCase: UnregisterDeviceTokenUseCase

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        useCase = UnregisterDeviceTokenUseCaseImpl(
            notificationRepository = notificationRepository
        )
    }

    @Nested
    inner class SuccessPath {

        @Test
        fun `returns success when device is unregistered`() = runTest {
            // Given
            coEvery { notificationRepository.unregisterCurrentDevice() } returns Unit

            // When
            val result = useCase()

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        fun `calls unregisterCurrentDevice`() = runTest {
            // Given
            coEvery { notificationRepository.unregisterCurrentDevice() } returns Unit

            // When
            useCase()

            // Then
            coVerify(exactly = 1) { notificationRepository.unregisterCurrentDevice() }
        }
    }

    @Nested
    inner class FailurePaths {

        @Test
        fun `fails when unregister call throws`() = runTest {
            // Given
            coEvery { notificationRepository.unregisterCurrentDevice() } throws
                RuntimeException("Unregister failed")

            // When
            val result = useCase()

            // Then
            assertTrue(result.isFailure)
            assertEquals("Unregister failed", result.exceptionOrNull()?.message)
        }
    }
}
