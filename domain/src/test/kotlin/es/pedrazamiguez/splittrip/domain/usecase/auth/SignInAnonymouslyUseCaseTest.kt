package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInAnonymouslyUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SignInAnonymouslyUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var useCase: SignInAnonymouslyUseCase

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        userPreferenceRepository = mockk(relaxed = true)
        useCase = SignInAnonymouslyUseCaseImpl(
            authenticationService = authenticationService,
            userPreferenceRepository = userPreferenceRepository
        )
    }

    @Nested
    inner class Invoke {

        @Test
        fun `returns active user ID and resets hasSignedOut to false on successful anonymous sign-in`() = runTest {
            // Given
            val anonymousUserId = "anonymous-uid-123"
            coEvery { authenticationService.signInAnonymously() } returns Result.success(anonymousUserId)

            // When
            val result = useCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(anonymousUserId, result.getOrNull())
            coVerify(exactly = 1) { authenticationService.signInAnonymously() }
            coVerify(exactly = 1) { userPreferenceRepository.setHasSignedOut(false) }
        }

        @Test
        fun `returns failure and does not reset hasSignedOut when authentication service fails`() = runTest {
            // Given
            val exception = RuntimeException("Auth error")
            coEvery { authenticationService.signInAnonymously() } returns Result.failure(exception)

            // When
            val result = useCase()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            coVerify(exactly = 1) { authenticationService.signInAnonymously() }
            coVerify(exactly = 0) { userPreferenceRepository.setHasSignedOut(any()) }
        }
    }
}
