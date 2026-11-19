package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategorySpendingScreenUiProviderImplTest {

    private val uiProvider = CategorySpendingScreenUiProviderImpl()

    @Test
    fun `route matches Routes CATEGORY_SPENDING by default`() {
        assertEquals(Routes.CATEGORY_SPENDING, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected custom route`() {
        val customRouteProvider = CategorySpendingScreenUiProviderImpl(route = "custom_route")
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }

    @Test
    fun `showBottomBar is true by default`() {
        assertTrue(uiProvider.showBottomBar)
    }
}
