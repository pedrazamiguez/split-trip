package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CreateEditSubunitScreenUiProviderImplTest {

    private val uiProvider = CreateEditSubunitScreenUiProviderImpl()

    @Test
    fun `route matches Routes CREATE_EDIT_SUBUNIT`() {
        assertEquals(Routes.CREATE_EDIT_SUBUNIT, uiProvider.route)
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
