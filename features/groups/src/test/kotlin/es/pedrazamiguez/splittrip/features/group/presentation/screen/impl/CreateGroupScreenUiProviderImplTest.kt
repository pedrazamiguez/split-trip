package es.pedrazamiguez.splittrip.features.group.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CreateGroupScreenUiProviderImplTest {

    private val uiProvider = CreateGroupScreenUiProviderImpl()

    @Test
    fun `route matches Routes CREATE_GROUP`() {
        assertEquals(Routes.CREATE_GROUP, uiProvider.route)
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
