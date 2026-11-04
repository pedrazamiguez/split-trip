package es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ProfileScreenUiProviderImplTest {

    private val isUserAnonymousUseCase: IsUserAnonymousUseCase = mockk(relaxed = true)
    private val uiProvider = ProfileScreenUiProviderImpl(isUserAnonymousUseCase = isUserAnonymousUseCase)

    @Test
    fun `route matches Routes PROFILE`() {
        assertEquals(Routes.PROFILE, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected route`() {
        val customRouteProvider = ProfileScreenUiProviderImpl(
            isUserAnonymousUseCase = isUserAnonymousUseCase,
            route = "custom_route"
        )
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }
}
