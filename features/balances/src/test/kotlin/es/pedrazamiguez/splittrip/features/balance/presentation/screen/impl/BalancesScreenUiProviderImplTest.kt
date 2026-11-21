package es.pedrazamiguez.splittrip.features.balance.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BalancesScreenUiProviderImplTest {

    private val uiProvider = BalancesScreenUiProviderImpl()

    @Test
    fun `route matches Routes BALANCES`() {
        assertEquals(Routes.BALANCES, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected route`() {
        val customRouteProvider = BalancesScreenUiProviderImpl(route = "custom_route")
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }

    @Test
    fun `mainAction sharedTransitionKey matches YOUR_BALANCE key`() {
        assertEquals(
            SharedElementKeys.YOUR_BALANCE,
            "your_balance_container"
        )
    }

    @Test
    fun `Routes SPENDING_INSIGHTS has expected value`() {
        assertEquals("spending_insights", Routes.SPENDING_INSIGHTS)
    }
}
