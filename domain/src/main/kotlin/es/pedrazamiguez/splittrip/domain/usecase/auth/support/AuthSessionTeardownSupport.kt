package es.pedrazamiguez.splittrip.domain.usecase.auth.support

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.service.SyncTeardownService
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase

internal suspend fun executeAuthTeardown(
    unregisterDeviceTokenUseCase: UnregisterDeviceTokenUseCase,
    localDatabaseCleaner: LocalDatabaseCleanerService,
    userPreferenceRepository: UserPreferenceRepository,
    syncTeardownService: SyncTeardownService,
    teardownListenersBeforeTokenUnregister: Boolean,
    abortOnSignOutFailure: Boolean,
    signOutAction: suspend () -> Result<Unit>
): Result<Unit> {
    if (teardownListenersBeforeTokenUnregister) {
        syncTeardownService.teardownAll()
    }

    // Best-effort device token unregistration — failure must not block teardown
    runCatching { unregisterDeviceTokenUseCase() }

    val signOutResult = signOutAction()
    if (abortOnSignOutFailure && signOutResult.isFailure) {
        return signOutResult
    }

    // DataStore is NOT cleared: keys are scoped by userId, preserving user preferences
    // (default currency, MRU lists, onboarding state) when signing back in on this device.
    userPreferenceRepository.setHasSignedOut(true)
    if (!teardownListenersBeforeTokenUnregister) {
        syncTeardownService.teardownAll()
    }

    return runCatching { localDatabaseCleaner.clearAll() }
}
