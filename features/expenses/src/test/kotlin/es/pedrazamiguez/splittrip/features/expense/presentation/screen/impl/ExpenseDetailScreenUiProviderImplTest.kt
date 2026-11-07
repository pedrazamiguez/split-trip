package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExpenseDetailScreenUiProviderImplTest {

    private val uiProvider = ExpenseDetailScreenUiProviderImpl()

    @Test
    fun `route matches Routes EXPENSE_DETAIL`() {
        assertEquals(Routes.EXPENSE_DETAIL, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected custom route`() {
        val customRouteProvider = ExpenseDetailScreenUiProviderImpl(route = "custom")
        assertEquals("custom", customRouteProvider.route)
    }

    @Test
    fun `topBar is not null`() {
        assertNotNull(uiProvider.topBar)
    }

    @Test
    fun `shouldShowEditAction returns true when group is not archived`() {
        assertTrue(uiProvider.shouldShowEditAction(isGroupArchived = false))
    }

    @Test
    fun `shouldShowEditAction returns false when group is archived`() {
        assertFalse(uiProvider.shouldShowEditAction(isGroupArchived = true))
    }

    @Test
    fun `shouldShowRefundAction returns true when group is not archived and expense is refundable`() {
        assertTrue(
            uiProvider.shouldShowRefundAction(
                isGroupArchived = false,
                isRefundable = true
            )
        )
    }

    @Test
    fun `shouldShowRefundAction returns false when group is archived`() {
        assertFalse(
            uiProvider.shouldShowRefundAction(
                isGroupArchived = true,
                isRefundable = true
            )
        )
    }

    @Test
    fun `shouldShowRefundAction returns false when expense is not refundable`() {
        assertFalse(
            uiProvider.shouldShowRefundAction(
                isGroupArchived = false,
                isRefundable = false
            )
        )
    }

    @Test
    fun `shouldShowDeleteAction returns true when group is not archived`() {
        assertTrue(uiProvider.shouldShowDeleteAction(isGroupArchived = false))
    }

    @Test
    fun `shouldShowDeleteAction returns false when group is archived`() {
        assertFalse(uiProvider.shouldShowDeleteAction(isGroupArchived = true))
    }

    @Test
    fun `resolveTitleText appends cancelled suffix when expense is cancelled`() {
        val expense = ExpenseDetailUiModel(
            title = "Dinner",
            isCancelled = true
        )
        val title = uiProvider.resolveTitleText(
            expense = expense,
            cancelledSuffix = "Cancelled"
        )
        assertEquals("Dinner (Cancelled)", title)
    }

    @Test
    fun `resolveTitleText returns title without suffix when expense is not cancelled`() {
        val expense = ExpenseDetailUiModel(
            title = "Dinner",
            isCancelled = false
        )
        val title = uiProvider.resolveTitleText(
            expense = expense,
            cancelledSuffix = "Cancelled"
        )
        assertEquals("Dinner", title)
    }

    @Test
    fun `resolveTitleText returns empty string when expense is null`() {
        val title = uiProvider.resolveTitleText(
            expense = null,
            cancelledSuffix = "Cancelled"
        )
        assertEquals("", title)
    }
}
