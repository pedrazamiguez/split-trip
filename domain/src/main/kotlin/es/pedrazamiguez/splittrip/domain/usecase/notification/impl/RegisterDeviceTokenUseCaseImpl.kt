package es.pedrazamiguez.splittrip.domain.usecase.notification.impl

import es.pedrazamiguez.splittrip.domain.repository.DeviceRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import kotlinx.coroutines.flow.first

class RegisterDeviceTokenUseCaseImpl(
    private val deviceRepository: DeviceRepository,
    private val notificationRepository: NotificationRepository
) : RegisterDeviceTokenUseCase {

    /**
     * Ensures the device's FCM token is registered with the backend.
     *
     * 1. If a pending token exists (from a prior failed registration), registers it first.
     * 2. Fetches the current token from FCM.
     * 3. If the current token differs from the pending one (or no pending existed),
     *    registers the fresh token with retry.
     *
     * This merges the previous SyncPendingTokenUseCase logic so that
     * MainViewModel only needs a single call on startup.
     */
    override suspend operator fun invoke(): Result<Unit> = runCatching {
        // Phase 1: Sync any pending token from a prior failed registration
        val pendingToken = notificationRepository.getPendingTokenFlow().first()
        val pendingSyncSucceeded = if (pendingToken != null) {
            // Direct registration (no retry) — if it fails, the retry below
            // with the fresh token will handle persistence.
            runCatching { notificationRepository.registerDeviceToken(pendingToken) }.isSuccess
        } else {
            false
        }

        // Phase 2: Always register the current (possibly rotated) token
        val currentToken = deviceRepository
            .getDeviceToken()
            .getOrThrow()

        // Skip redundant registration only if the pending token was the same AND succeeded
        if (currentToken == pendingToken && pendingSyncSucceeded) return@runCatching

        notificationRepository.registerDeviceTokenWithRetry(currentToken)
    }
}
