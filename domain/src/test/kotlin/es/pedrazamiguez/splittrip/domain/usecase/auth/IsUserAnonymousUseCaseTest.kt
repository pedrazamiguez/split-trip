package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.IsUserAnonymousUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IsUserAnonymousUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var useCase: IsUserAnonymousUseCase

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        useCase = IsUserAnonymousUseCaseImpl(authenticationService)
    }

    @Test
    fun `returns true when user is logged in and anonymous`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns true

        val result = useCase().first()

        assertTrue(result)
    }

    @Test
    fun `returns false when user is logged in but not anonymous`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false

        val result = useCase().first()

        assertFalse(result)
    }

    @Test
    fun `returns false when user is not logged in`() = runTest {
        every { authenticationService.authState } returns flowOf(false)
        every { authenticationService.isAnonymous() } returns true

        val result = useCase().first()

        assertFalse(result)
    }
}
