package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SignInWithEmailUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var userRepository: UserRepository
    private lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
    private lateinit var useCase: SignInWithEmailUseCase

    private val email = "user@example.com"
    private val password = "password123"
    private val userId = "firebase-uid-123"

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        userRepository = mockk()
        registerDeviceTokenUseCase = mockk()
        useCase = SignInWithEmailUseCase(
            authenticationService = authenticationService,
            userRepository = userRepository,
            registerDeviceTokenUseCase = registerDeviceTokenUseCase
        )
        coEvery { userRepository.getCurrentUserProfile() } returns User(userId, email, "user", null, null)
        coEvery { userRepository.saveUser(any()) } returns Result.success(Unit)
    }

    @Nested
    inner class SuccessPath {

        @Test
        fun `returns userId on successful sign-in`() = runTest {
            // Given
            coEvery { authenticationService.signIn(email, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            val result = useCase(email, password)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(userId, result.getOrNull())
        }

        @Test
        fun `calls signIn with correct email and password`() = runTest {
            // Given
            coEvery { authenticationService.signIn(any(), any()) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            useCase(email, password)

            // Then
            coVerify(exactly = 1) { authenticationService.signIn(email, password) }
        }

        @Test
        fun `registers device token after successful sign-in`() = runTest {
            // Given
            coEvery { authenticationService.signIn(email, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)

            // When
            useCase(email, password)

            // Then
            coVerify(exactly = 1) { registerDeviceTokenUseCase() }
        }

        @Test
        fun `succeeds even when device token registration fails`() = runTest {
            // Given
            coEvery { authenticationService.signIn(email, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.failure(RuntimeException("Token failed"))

            // When
            val result = useCase(email, password)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(userId, result.getOrNull())
        }

        @Test
        fun `does not save user if profile already exists`() = runTest {
            // Given
            coEvery { authenticationService.signIn(email, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)
            coEvery { userRepository.getCurrentUserProfile() } returns User(userId, email, "user", null, null)

            // When
            useCase(email, password)

            // Then
            coVerify(exactly = 0) { userRepository.saveUser(any()) }
        }

        @Test
        fun `creates and saves default profile if it does not exist`() = runTest {
            // Given
            coEvery { authenticationService.signIn(email, password) } returns Result.success(userId)
            coEvery { registerDeviceTokenUseCase() } returns Result.success(Unit)
            coEvery { userRepository.getCurrentUserProfile() } returns null
            coEvery { userRepository.saveUser(any()) } returns Result.success(Unit)

            // When
            val result = useCase(email, password)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                userRepository.saveUser(
                    withArg {
                        assertEquals(userId, it.userId)
                        assertEquals(email, it.email)
                        assertEquals("user", it.displayName)
                        org.junit.jupiter.api.Assertions.assertNull(it.profileImagePath)
                        org.junit.jupiter.api.Assertions.assertNotNull(it.createdAt)
                    }
                )
            }
        }
    }

    @Nested
    inner class FailurePath {

        @Test
        fun `fails when authentication fails`() = runTest {
            // Given
            val exception = RuntimeException("Auth failed")
            coEvery { authenticationService.signIn(email, password) } returns Result.failure(exception)

            // When
            val result = useCase(email, password)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Auth failed", result.exceptionOrNull()?.message)
            coVerify(exactly = 0) { registerDeviceTokenUseCase() }
        }

        @Test
        fun `fails when profile creation fails`() = runTest {
            // Given
            coEvery { authenticationService.signIn(email, password) } returns Result.success(userId)
            coEvery { userRepository.getCurrentUserProfile() } returns null
            coEvery { userRepository.saveUser(any()) } returns Result.failure(RuntimeException("Save failed"))

            // When
            val result = useCase(email, password)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Save failed", result.exceptionOrNull()?.message)
        }
    }
}
