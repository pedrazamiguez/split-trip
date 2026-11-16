package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.ObserveDeveloperAccessUseCaseImpl
import io.mockk.every
import io.mockk.mockk
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

class ObserveDeveloperAccessUseCaseImplTest {

    private val authenticationService: AuthenticationService = mockk()
    private val appConfigService: AppConfigService = mockk()

    private fun createUseCase(isDebugBuild: Boolean): ObserveDeveloperAccessUseCaseImpl =
        ObserveDeveloperAccessUseCaseImpl(
            authenticationService = authenticationService,
            appConfigService = appConfigService,
            isDebugBuild = isDebugBuild
        )

    @Test
    fun `emits true in debug build when user is signed out and allowlist is empty`() = runTest {
        every { authenticationService.authState } returns flowOf(false)
        every { appConfigService.developerAllowlistEmailHashes } returns MutableStateFlow(emptySet())

        val result = createUseCase(isDebugBuild = true)().first()

        assertTrue(result)
    }

    @Test
    fun `emits true in debug build for anonymous user`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns true
        every { appConfigService.developerAllowlistEmailHashes } returns MutableStateFlow(emptySet())

        val result = createUseCase(isDebugBuild = true)().first()

        assertTrue(result)
    }

    @Test
    fun `emits false in non-debug build when allowlist is empty`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "dev@example.com"
        every { appConfigService.developerAllowlistEmailHashes } returns MutableStateFlow(emptySet())

        val result = createUseCase(isDebugBuild = false)().first()

        assertFalse(result)
    }

    @Test
    fun `emits false in non-debug build when user is signed out`() = runTest {
        every { authenticationService.authState } returns flowOf(false)
        every {
            appConfigService.developerAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("dev@example.com")))

        val result = createUseCase(isDebugBuild = false)().first()

        assertFalse(result)
    }

    @Test
    fun `emits false in non-debug build when user is anonymous even if allowlist is non-empty`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns true
        every {
            appConfigService.developerAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("dev@example.com")))

        val result = createUseCase(isDebugBuild = false)().first()

        assertFalse(result)
    }

    @Test
    fun `emits false in non-debug build when currentUserEmail is null or blank`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "  "
        every {
            appConfigService.developerAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("dev@example.com")))

        val blankResult = createUseCase(isDebugBuild = false)().first()
        assertFalse(blankResult)

        every { authenticationService.currentUserEmail() } returns null
        val nullResult = createUseCase(isDebugBuild = false)().first()
        assertFalse(nullResult)
    }

    @Test
    fun `emits false in non-debug build when email hash is not in allowlist`() = runTest {
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "user@example.com"
        every {
            appConfigService.developerAllowlistEmailHashes
        } returns MutableStateFlow(setOf(User.hashEmail("other@example.com")))

        val result = createUseCase(isDebugBuild = false)().first()

        assertFalse(result)
    }

    @Test
    fun `emits true in non-debug build when normalized email hash matches allowlist`() =
        runTest {
            every { authenticationService.authState } returns flowOf(true)
            every { authenticationService.isAnonymous() } returns false
            every { authenticationService.currentUserEmail() } returns "  DEV@Example.COM  "
            every {
                appConfigService.developerAllowlistEmailHashes
            } returns MutableStateFlow(setOf(User.hashEmail("dev@example.com")))

            val result = createUseCase(isDebugBuild = false)().first()

            assertTrue(result)
        }

    @Test
    fun `updates access reactively when developerAllowlistEmailHashes flow changes`() = runTest {
        val devHash = User.hashEmail("dev@example.com")
        val allowlistFlow = MutableStateFlow<Set<String>>(emptySet())
        every { authenticationService.authState } returns flowOf(true)
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "dev@example.com"
        every { appConfigService.developerAllowlistEmailHashes } returns allowlistFlow

        val emissions = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            createUseCase(isDebugBuild = false)().take(3).toList(emissions)
        }

        allowlistFlow.value = setOf(devHash)
        allowlistFlow.value = emptySet()
        job.join()

        assertEquals(listOf(false, true, false), emissions)
    }

    @Test
    fun `updates access reactively when authState changes`() = runTest {
        val devHash = User.hashEmail("dev@example.com")
        val authFlow = MutableStateFlow(false)
        every { authenticationService.authState } returns authFlow
        every { authenticationService.isAnonymous() } returns false
        every { authenticationService.currentUserEmail() } returns "dev@example.com"
        every { appConfigService.developerAllowlistEmailHashes } returns MutableStateFlow(setOf(devHash))

        val emissions = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            createUseCase(isDebugBuild = false)().take(3).toList(emissions)
        }

        authFlow.value = true
        authFlow.value = false
        job.join()

        assertEquals(listOf(false, true, false), emissions)
    }
}
