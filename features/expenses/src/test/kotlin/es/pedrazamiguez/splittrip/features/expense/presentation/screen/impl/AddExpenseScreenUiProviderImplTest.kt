package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AddExpenseScreenUiProviderImplTest {

    private val uiProvider = AddExpenseScreenUiProviderImpl()

    @Test
    fun `route matches Routes ADD_EXPENSE by default`() {
        assertEquals(Routes.ADD_EXPENSE, uiProvider.route)
    }

    @Test
    fun `provider configuration supports Routes EDIT_EXPENSE`() {
        val editProvider = AddExpenseScreenUiProviderImpl(route = Routes.EDIT_EXPENSE)
        assertEquals(Routes.EDIT_EXPENSE, editProvider.route)
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
