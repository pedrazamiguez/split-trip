package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ADD_EXPENSE_SHARED_ELEMENT_KEY
import io.mockk.mockk
import java.time.LocalDate
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
    fun `showBottomBar is true by default`() {
        assertTrue(uiProvider.showBottomBar)
    }

    @Test
    fun `spending insights route matches expected destination`() {
        assertEquals("spending_insights", Routes.SPENDING_INSIGHTS)
    }

    @Test
    fun `ADD_EXPENSE_SHARED_ELEMENT_KEY matches expected shared transition key`() {
        assertEquals("add_expense_container", ADD_EXPENSE_SHARED_ELEMENT_KEY)
    }

    @Test
    fun `shouldShowMainAction returns true when group status is ACTIVE`() {
        assertTrue(uiProvider.shouldShowMainAction(GroupStatus.ACTIVE))
    }

    @Test
    fun `shouldShowMainAction returns true when group status is null`() {
        assertTrue(uiProvider.shouldShowMainAction(null))
    }

    @Test
    fun `shouldShowMainAction returns false when group status is ARCHIVED`() {
        assertFalse(uiProvider.shouldShowMainAction(GroupStatus.ARCHIVED))
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

    @Test
    fun `resolveActiveFilterCount returns 0 when criteria is null`() {
        assertEquals(0, uiProvider.resolveActiveFilterCount(null))
    }

    @Test
    fun `resolveActiveFilterCount returns 0 for default empty criteria`() {
        assertEquals(0, uiProvider.resolveActiveFilterCount(ExpenseFilterCriteria()))
    }

    @Test
    fun `resolveActiveFilterCount ignores searchQuery-only criteria`() {
        val criteria = ExpenseFilterCriteria(searchQuery = "dinner")
        assertEquals(0, uiProvider.resolveActiveFilterCount(criteria))
    }

    @Test
    fun `resolveActiveFilterCount returns count of active non-search filter dimensions`() {
        val criteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.FOOD),
            selectedPaymentMethods = setOf(PaymentMethod.CASH),
            startDate = LocalDate.of(2026, 1, 1)
        )
        assertEquals(3, uiProvider.resolveActiveFilterCount(criteria))
    }

    @Test
    fun `canResetFilters returns false when activeFilterCount is zero`() {
        assertFalse(uiProvider.canResetFilters(0))
    }

    @Test
    fun `canResetFilters returns false when activeFilterCount is negative`() {
        assertFalse(uiProvider.canResetFilters(-1))
    }

    @Test
    fun `canResetFilters returns true when activeFilterCount is positive`() {
        assertTrue(uiProvider.canResetFilters(1))
        assertTrue(uiProvider.canResetFilters(3))
    }

    @Test
    fun `expenses filter route matches expected destination`() {
        assertEquals(Routes.EXPENSES_FILTER, Routes.expensesFilterRoute())
    }
}
