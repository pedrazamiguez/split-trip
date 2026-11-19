package es.pedrazamiguez.splittrip.features.contribution.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AddContributionScreenUiProviderImplTest {

    private val uiProvider = AddContributionScreenUiProviderImpl()

    @Test
    fun `route matches Routes CONTRIBUTION_WIZARD`() {
        assertEquals(Routes.CONTRIBUTION_WIZARD, uiProvider.route)
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
