package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SendPasswordResetEmailUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendPasswordResetEmailUseCaseImplTest {

    private lateinit var authService: AuthenticationService
    private lateinit var emailValidationService: EmailValidationService
    private lateinit var useCase: SendPasswordResetEmailUseCase

    @BeforeEach
    fun setUp() {
        authService = mockk()
        emailValidationService = mockk()
        useCase = SendPasswordResetEmailUseCaseImpl(
            authService = authService,
            emailValidationService = emailValidationService
        )
    }

    @Test
    fun `invoke with blank or malformed email returns failure`() = runTest {
        // Given
        val invalidEmail = "invalid-email"
        every { emailValidationService.isValidEmail(invalidEmail) } returns false

        // When
        val result = useCase(invalidEmail)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Invalid email format", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { authService.sendPasswordResetEmail(any()) }
    }

    @Test
    fun `invoke with valid email delegates request to AuthenticationService and returns success`() = runTest {
        // Given
        val validEmail = "  user@example.com  "
        val trimmedEmail = "user@example.com"
        every { emailValidationService.isValidEmail(trimmedEmail) } returns true
        coEvery { authService.sendPasswordResetEmail(trimmedEmail) } returns Result.success(Unit)

        // When
        val result = useCase(validEmail)

        // Then
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authService.sendPasswordResetEmail(trimmedEmail) }
    }

    @Test
    fun `invoke returns failure if AuthenticationService fails`() = runTest {
        // Given
        val validEmail = "user@example.com"
        val exception = RuntimeException("Firebase error")
        every { emailValidationService.isValidEmail(validEmail) } returns true
        coEvery { authService.sendPasswordResetEmail(validEmail) } returns Result.failure(exception)

        // When
        val result = useCase(validEmail)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { authService.sendPasswordResetEmail(validEmail) }
    }
}
