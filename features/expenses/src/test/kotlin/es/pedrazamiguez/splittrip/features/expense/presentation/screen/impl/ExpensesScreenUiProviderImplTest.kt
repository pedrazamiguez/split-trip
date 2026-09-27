package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ADD_EXPENSE_SHARED_ELEMENT_KEY
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpensesScreenUiProviderImplTest {

    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase = mockk()

    private val uiProvider = ExpensesScreenUiProviderImpl(
        observeSelectedGroupUseCase = observeSelectedGroupUseCase
    )

    @Test
    fun `route matches Routes EXPENSES`() {
        assertEquals(Routes.EXPENSES, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected custom route`() {
        val customRouteProvider = ExpensesScreenUiProviderImpl(
            observeSelectedGroupUseCase = observeSelectedGroupUseCase,
            route = "custom_route"
        )
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }

    @Test
    fun `ADD_EXPENSE_SHARED_ELEMENT_KEY matches expected shared transition key`() {
        assertEquals("add_expense_container", ADD_EXPENSE_SHARED_ELEMENT_KEY)
    }

    @Test
    fun `isMainActionEnabled returns true when group status is ACTIVE`() {
        assertTrue(uiProvider.isMainActionEnabled(GroupStatus.ACTIVE))
    }

    @Test
    fun `isMainActionEnabled returns true when group status is null`() {
        assertTrue(uiProvider.isMainActionEnabled(null))
    }

    @Test
    fun `isMainActionEnabled returns false when group status is ARCHIVED`() {
        assertFalse(uiProvider.isMainActionEnabled(GroupStatus.ARCHIVED))
    }

    @Test
    fun `resolveSubtitle returns group name when present and non-blank`() {
        assertEquals("South Africa Safari", uiProvider.resolveSubtitle("South Africa Safari"))
    }

    @Test
    fun `resolveSubtitle returns null when group name is null`() {
        assertNull(uiProvider.resolveSubtitle(null))
    }

    @Test
    fun `resolveSubtitle returns null when group name is blank`() {
        assertNull(uiProvider.resolveSubtitle("   "))
    }
}
