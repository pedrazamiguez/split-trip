package es.pedrazamiguez.splittrip.features.settlement.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class DisputeSettlementScreenUiProviderImplTest {

    private val uiProvider = DisputeSettlementScreenUiProviderImpl()

    @Test
    fun `route matches Routes DISPUTE_SETTLEMENT by default`() {
        assertEquals(Routes.DISPUTE_SETTLEMENT, uiProvider.route)
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
