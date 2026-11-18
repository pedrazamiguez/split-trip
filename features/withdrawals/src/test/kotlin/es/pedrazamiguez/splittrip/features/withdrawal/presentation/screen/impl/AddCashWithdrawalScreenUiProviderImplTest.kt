package es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AddCashWithdrawalScreenUiProviderImplTest {

    private val uiProvider = AddCashWithdrawalScreenUiProviderImpl()

    @Test
    fun `route matches Routes ADD_CASH_WITHDRAWAL`() {
        assertEquals(Routes.ADD_CASH_WITHDRAWAL, uiProvider.route)
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
