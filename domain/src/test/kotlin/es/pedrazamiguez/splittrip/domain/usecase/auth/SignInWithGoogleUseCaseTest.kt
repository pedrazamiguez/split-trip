package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInWithGoogleUseCaseImpl
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

class SignInWithGoogleUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
    private lateinit var useCase: SignInWithGoogleUseCase

    private val idToken = "google-id-token"
    private val firebaseUser = User(
        userId = "firebase-uid-123",
        email = "user@example.com",
        displayName = "Test User",
        profileImagePath = "https://example.com/photo.jpg"
    )

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        registerDeviceTokenUseCase = mockk()
        userPreferenceRepository = mockk()
        reconcileUnregisteredUserUseCase = mockk()
        useCase = SignInWithGoogleUseCaseImpl(
            authenticationService = authenticationService,
            registerDeviceTokenUseCase = registerDeviceTokenUseCase,
            userPreferenceRepository = userPreferenceRepository,
            reconcileUnregisteredUserUseCase = reconcileUnregisteredUserUseCase
        )
        coEvery { authenticationService.currentUserEmail() } returns "user@example.com"
        coEvery { userPreferenceRepository.setHasSignedOut(any()) } returns Unit
        coEvery { reconcileUnregisteredUserUseCase(any(), any()) } returns Result.success(Unit)
    }

    @Nested
    inner class SuccessPath {

        @Test
        fun `returns userId on successful sign-in`() = runTest {
            // Given
            coEvery { authenticationService.signInWithGoogle(idToken) } returns Result.success(firebaseUser)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            val result = useCase(idToken)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(firebaseUser.userId, result.getOrNull())
        }

        @Test
        fun `registers device token after sign-in`() = runTest {
            // Given
            coEvery { authenticationService.signInWithGoogle(idToken) } returns Result.success(firebaseUser)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            useCase(idToken)

            // Then
            coVerify(exactly = 1) { registerDeviceTokenUseCase() }
        }

        @Test
        fun `succeeds even when device token registration fails`() = runTest {
            // Given
            coEvery { authenticationService.signInWithGoogle(idToken) } returns Result.success(firebaseUser)
            coEvery { registerDeviceTokenUseCase() } returns Result.failure(RuntimeException("Token failed"))

            // When
            val result = useCase(idToken)

            // Then - sign-in should still succeed (device token is best-effort)
            assertTrue(result.isSuccess)
            assertEquals(firebaseUser.userId, result.getOrNull())
        }

        @Test
        fun `succeeds even when reconciliation fails`() = runTest {
            // Given
            coEvery { authenticationService.signInWithGoogle(idToken) } returns Result.success(firebaseUser)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)
            coEvery { reconcileUnregisteredUserUseCase(any(), any()) } returns
                Result.failure(RuntimeException("Reconciliation failed"))

            // When
            val result = useCase(idToken)

            // Then - sign-in should still succeed (reconciliation is best-effort/non-blocking)
            assertTrue(result.isSuccess)
            assertEquals(firebaseUser.userId, result.getOrNull())
        }
    }

    @Nested
    inner class FailurePaths {

        @Test
        fun `fails when authentication fails`() = runTest {
            // Given
            val exception = RuntimeException("Auth failed")
            coEvery { authenticationService.signInWithGoogle(idToken) } returns Result.failure(exception)

            // When
            val result = useCase(idToken)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Auth failed", result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { registerDeviceTokenUseCase() }
        }
    }
}
