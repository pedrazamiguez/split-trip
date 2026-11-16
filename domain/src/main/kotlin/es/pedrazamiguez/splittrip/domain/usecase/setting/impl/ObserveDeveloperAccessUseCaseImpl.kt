package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveDeveloperAccessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

class ObserveDeveloperAccessUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val appConfigService: AppConfigService,
    private val isDebugBuild: Boolean
) : ObserveDeveloperAccessUseCase {

    override operator fun invoke(): Flow<Boolean> {
        if (isDebugBuild) {
            return flowOf(true)
        }
        return combine(
            authenticationService.authState,
            appConfigService.developerAllowlistEmailHashes
        ) { isLoggedIn, allowlistHashes ->
            if (!isLoggedIn || authenticationService.isAnonymous() || allowlistHashes.isEmpty()) {
                false
            } else {
                val email = authenticationService.currentUserEmail()
                if (email.isNullOrBlank()) {
                    false
                } else {
                    User.hashEmail(email) in allowlistHashes
                }
            }
        }.distinctUntilChanged()
    }
}
