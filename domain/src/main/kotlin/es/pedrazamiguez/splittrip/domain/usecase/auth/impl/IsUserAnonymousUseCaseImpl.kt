package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IsUserAnonymousUseCaseImpl(
    private val authenticationService: AuthenticationService
) : IsUserAnonymousUseCase {

    override fun invoke(): Flow<Boolean> = authenticationService.authState.map { isLoggedIn ->
        isLoggedIn && authenticationService.isAnonymous()
    }
}
