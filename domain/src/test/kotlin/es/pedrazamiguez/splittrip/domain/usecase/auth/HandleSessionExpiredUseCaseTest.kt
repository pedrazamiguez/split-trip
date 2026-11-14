package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.service.SyncTeardownService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.HandleSessionExpiredUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HandleSessionExpiredUseCaseTest {

    private lateinit var unregisterDeviceTokenUseCase: UnregisterDeviceTokenUseCase
    private lateinit var localDatabaseCleaner: LocalDatabaseCleanerService
    private lateinit var authenticationService: AuthenticationService
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var syncTeardownService: SyncTeardownService
    private lateinit var useCase: HandleSessionExpiredUseCase

    @BeforeEach
    fun setUp() {
        unregisterDeviceTokenUseCase = mockk()
        localDatabaseCleaner = mockk()
        authenticationService = mockk()
        userPreferenceRepository = mockk()
        syncTeardownService = mockk()
        useCase = HandleSessionExpiredUseCaseImpl(
            unregisterDeviceTokenUseCase = unregisterDeviceTokenUseCase,
            localDatabaseCleaner = localDatabaseCleaner,
            authenticationService = authenticationService,
            userPreferenceRepository = userPreferenceRepository,
            syncTeardownService = syncTeardownService
        )
        coEvery { unregisterDeviceTokenUseCase() } returns Result.success(Unit)
        coEvery { authenticationService.signOut() } returns Result.success(Unit)
        coEvery { userPreferenceRepository.setHasSignedOut(any()) } returns Unit
        coEvery { syncTeardownService.teardownAll() } returns Unit
        coEvery { localDatabaseCleaner.clearAll() } returns Unit
    }

    @Test
    fun `returns success when all teardown steps succeed`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `tears down sync listeners before token unregistration and clears Room last`() = runTest {
        useCase()

        coVerifyOrder {
            syncTeardownService.teardownAll()
            unregisterDeviceTokenUseCase()
            authenticationService.signOut()
            userPreferenceRepository.setHasSignedOut(true)
            localDatabaseCleaner.clearAll()
        }
    }

    @Test
    fun `completes teardown and returns success when device token unregistration fails`() = runTest {
        coEvery { unregisterDeviceTokenUseCase() } returns Result.failure(RuntimeException("User not logged in"))

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { syncTeardownService.teardownAll() }
        coVerify(exactly = 1) { userPreferenceRepository.setHasSignedOut(true) }
        coVerify(exactly = 1) { localDatabaseCleaner.clearAll() }
    }

    @Test
    fun `completes teardown and returns success when auth signOut returns failure`() = runTest {
        coEvery { authenticationService.signOut() } returns Result.failure(RuntimeException("Sign-out failed"))

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { localDatabaseCleaner.clearAll() }
    }

    @Test
    fun `returns failure when localDatabaseCleaner clearAll throws`() = runTest {
        coEvery { localDatabaseCleaner.clearAll() } throws RuntimeException("DB error")

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }
}
