package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveProAllowlistAccessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveProAllowlistAccessUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val appConfigService: AppConfigService
) : ObserveProAllowlistAccessUseCase {

    override operator fun invoke(): Flow<Boolean> = combine(
        authenticationService.authState,
        appConfigService.proAllowlistEmailHashes
    ) { isLoggedIn, allowlistHashes ->
        if (!isLoggedIn || authenticationService.isAnonymous()) {
            false
        } else {
            User.isEmailInAllowlist(
                email = authenticationService.currentUserEmail(),
                allowlistHashes = allowlistHashes
            )
        }
    }.distinctUntilChanged()
}
