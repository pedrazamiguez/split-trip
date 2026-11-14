package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.service.SyncTeardownService
import es.pedrazamiguez.splittrip.domain.usecase.auth.HandleSessionExpiredUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.support.executeAuthTeardown
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase

class HandleSessionExpiredUseCaseImpl(
    private val unregisterDeviceTokenUseCase: UnregisterDeviceTokenUseCase,
    private val localDatabaseCleaner: LocalDatabaseCleanerService,
    private val authenticationService: AuthenticationService,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val syncTeardownService: SyncTeardownService
) : HandleSessionExpiredUseCase {

    override suspend operator fun invoke(): Result<Unit> = executeAuthTeardown(
        unregisterDeviceTokenUseCase = unregisterDeviceTokenUseCase,
        localDatabaseCleaner = localDatabaseCleaner,
        userPreferenceRepository = userPreferenceRepository,
        syncTeardownService = syncTeardownService,
        teardownListenersBeforeTokenUnregister = true,
        abortOnSignOutFailure = false,
        signOutAction = { authenticationService.signOut() }
    )
}
