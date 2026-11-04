package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.IsUserAnonymousUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun `emits false when authState fires and user is no longer anonymous`() = runTest {
        val authStateFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 2)
        every { authenticationService.authState } returns authStateFlow
        every { authenticationService.isAnonymous() } returnsMany listOf(true, false)

        val emissions = mutableListOf<Boolean>()
        val job = launch {
            useCase().toList(emissions)
        }
        runCurrent()

        authStateFlow.emit(true)
        runCurrent()
        authStateFlow.emit(true)
        runCurrent()

        assertEquals(listOf(true, false), emissions)
        job.cancel()
    }
}
