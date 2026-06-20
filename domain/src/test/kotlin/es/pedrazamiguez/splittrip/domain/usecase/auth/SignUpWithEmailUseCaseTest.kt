package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignUpWithEmailUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SignUpWithEmailUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
    private lateinit var useCase: SignUpWithEmailUseCase

    private val email = "newuser@example.com"
    private val displayName = "New User"
    private val password = "password123"
    private val userId = "firebase-uid-999"

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        registerDeviceTokenUseCase = mockk()
        userPreferenceRepository = mockk()
        reconcileUnregisteredUserUseCase = mockk()
        useCase = SignUpWithEmailUseCaseImpl(
            authenticationService = authenticationService,
            registerDeviceTokenUseCase = registerDeviceTokenUseCase,
            userPreferenceRepository = userPreferenceRepository,
            reconcileUnregisteredUserUseCase = reconcileUnregisteredUserUseCase
        )
        coEvery { userPreferenceRepository.setHasSignedOut(any()) } returns Unit
        coEvery { reconcileUnregisteredUserUseCase(any(), any()) } returns Result.success(Unit)
    }

    @Nested
    inner class SuccessPath {

        @Test
        fun `returns userId on successful sign-up`() = runTest {
            // Given
            coEvery { authenticationService.signUp(email, displayName, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            val result = useCase(email, displayName, password)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(userId, result.getOrNull())
        }

        @Test
        fun `calls signUp with correct parameters`() = runTest {
            // Given
            coEvery { authenticationService.signUp(any(), any(), any()) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            useCase(email, displayName, password)

            // Then
            coVerify(exactly = 1) { authenticationService.signUp(email, displayName, password) }
        }

        @Test
        fun `registers device token after successful sign-up`() = runTest {
            // Given
            coEvery { authenticationService.signUp(email, displayName, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            useCase(email, displayName, password)

            // Then
            coVerify(exactly = 1) { registerDeviceTokenUseCase() }
        }

        @Test
        fun `succeeds even when device token registration fails`() = runTest {
            // Given
            coEvery { authenticationService.signUp(email, displayName, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.failure(RuntimeException("Token failed"))

            // When
            val result = useCase(email, displayName, password)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(userId, result.getOrNull())
        }
    }

    @Nested
    inner class FailurePath {

        @Test
        fun `fails when authentication fails`() = runTest {
            // Given
            val exception = RuntimeException("Auth failed")
            coEvery { authenticationService.signUp(email, displayName, password) } returns Result.failure(exception)

            // When
            val result = useCase(email, displayName, password)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Auth failed", result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { registerDeviceTokenUseCase() }
        }
    }
}
