package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveProAllowlistAccessUseCaseImplTest {

    private val authenticationService: AuthenticationService = mockk()
    private val appConfigService: AppConfigService = mockk()

    private fun createUseCase(): ObserveProAllowlistAccessUseCaseImpl =
        ObserveProAllowlistAccessUseCaseImpl(
            authenticationService = authenticationService,
            appConfigService = appConfigService
        )

    @Test
    fun `emits false when user is signed out`() = runTest {
        every { authenticationService.authState } returns flowOf(false)
        every {
            appConfigService.proAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("pro@example.com")))

        val result = createUseCase()().first()

        assertFalse(result)
    }

    @Test
    fun `emits false when user is anonymous even if allowlist is non-empty`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns true
        every {
            appConfigService.proAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("pro@example.com")))

        val result = createUseCase()().first()

        assertFalse(result)
    }

    @Test
    fun `emits false when allowlist is empty`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "pro@example.com"
        every { appConfigService.proAllowlistEmailHashes } returns MutableStateFlow(emptySet())

        val result = createUseCase()().first()

        assertFalse(result)
    }

    @Test
    fun `emits false when currentUserEmail is null or blank`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every {
            appConfigService.proAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("pro@example.com")))

        every { authenticationService.currentUserEmail() } returns "   "
        assertFalse(createUseCase()().first())

        every { authenticationService.currentUserEmail() } returns null
        assertFalse(createUseCase()().first())
    }

    @Test
    fun `emits false when email hash is not in allowlist`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "user@example.com"
        every {
            appConfigService.proAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("other@example.com")))

        val result = createUseCase()().first()

        assertFalse(result)
    }

    @Test
    fun `emits true when normalized email hash matches allowlist regardless of case and whitespace`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "  PRO@Example.COM  "
        every {
            appConfigService.proAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("pro@example.com")))

        val result = createUseCase()().first()

        assertTrue(result)
    }

    @Test
    fun `updates access reactively when proAllowlistEmailHashes flow changes`() = runTest {
        val proHash = User.hashEmail("pro@example.com")
        val allowlistFlow = MutableStateFlow<Set<String>>(emptySet())
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "pro@example.com"
        every { appConfigService.proAllowlistEmailHashes } returns allowlistFlow

        val emissions = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            createUseCase()().take(3).toList(emissions)
        }

        allowlistFlow.value = setOf(proHash)
        allowlistFlow.value = emptySet()
        job.join()

        assertEquals(listOf(false, true, false), emissions)
    }

    @Test
    fun `updates access reactively when authState changes`() = runTest {
        val proHash = User.hashEmail("pro@example.com")
        val authFlow = MutableStateFlow(false)
        every { authenticationService.authState } returns authFlow
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "pro@example.com"
        every { appConfigService.proAllowlistEmailHashes } returns MutableStateFlow(setOf(proHash))

        val emissions = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            createUseCase()().take(3).toList(emissions)
        }

        authFlow.value = true
        authFlow.value = false
        job.join()

        assertEquals(listOf(false, true, false), emissions)
    }
}
