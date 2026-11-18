package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ExpensesFilterScreenUiProviderImplTest {

    private val uiProvider = ExpensesFilterScreenUiProviderImpl()

    @Test
    fun `route matches Routes EXPENSES_FILTER by default`() {
        assertEquals(Routes.EXPENSES_FILTER, uiProvider.route)
    }

    @Test
    fun `showBottomBar is false`() {
        assertFalse(uiProvider.showBottomBar)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }
}
