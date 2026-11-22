package es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SpendingInsightsScreenUiProviderImplTest {

    private val uiProvider = SpendingInsightsScreenUiProviderImpl()

    @Test
    fun `route matches Routes SPENDING_INSIGHTS by default`() {
        assertEquals(Routes.SPENDING_INSIGHTS, uiProvider.route)
    }

    @Test
    fun `provider configuration accepts custom route`() {
        val customRouteProvider = SpendingInsightsScreenUiProviderImpl(route = "custom_route")
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }
}
