package es.pedrazamiguez.splittrip.features.profile.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EditProfileScreenUiProviderImplTest {

    private val uiProvider = EditProfileScreenUiProviderImpl()

    @Test
    fun `route matches Routes EDIT_PROFILE`() {
        assertEquals(Routes.EDIT_PROFILE, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected route`() {
        val customRouteProvider = EditProfileScreenUiProviderImpl(route = "custom_route")
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }
}
