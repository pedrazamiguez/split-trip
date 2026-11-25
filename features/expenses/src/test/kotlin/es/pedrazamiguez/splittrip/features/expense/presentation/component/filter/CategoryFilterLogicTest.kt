package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategoryFilterLogicTest {

    @Test
    fun `resolveVisibleCategories returns context categories when showAll is false and nothing selected`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            groupContext = GroupContext.TRAVEL,
            showAllCategories = false,
            selectedCategories = emptySet()
        )

        assertEquals(ExpenseCategory.forContext(GroupContext.TRAVEL), result)
        assertTrue(ExpenseCategory.HOME !in result)
        assertTrue(ExpenseCategory.HEALTH_PERSONAL_CARE !in result)
    }

    @Test
    fun `resolveVisibleCategories preserves out-of-context pre-selected categories when showAll is false`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            groupContext = GroupContext.TRAVEL,
            showAllCategories = false,
            selectedCategories = setOf(ExpenseCategory.HOME)
        )

        val expected = ExpenseCategory.forContext(GroupContext.TRAVEL) + ExpenseCategory.HOME
        assertEquals(expected, result)
    }

    @Test
    fun `resolveVisibleCategories returns all ExpenseCategory entries when showAll is true`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            groupContext = GroupContext.TRAVEL,
            showAllCategories = true,
            selectedCategories = emptySet()
        )

        assertEquals(ExpenseCategory.entries, result)
    }

    @Test
    fun `resolveVisibleSubcategories scopes to groupContext and keeps out-of-context selections`() {
        val result = CategoryFilterLogic.resolveVisibleSubcategories(
            category = ExpenseCategory.TRANSPORT,
            selectedSubcategories = setOf(ExpenseSubcategory.INTERNATIONAL_FLIGHT),
            groupContext = GroupContext.EVERYDAY,
            showAllCategories = false
        )

        val expected = (
            ExpenseSubcategory.forCategory(ExpenseCategory.TRANSPORT, GroupContext.EVERYDAY) +
                ExpenseSubcategory.INTERNATIONAL_FLIGHT
            ).distinct()
        assertEquals(expected, result)
    }

    @Test
    fun `resolveVisibleSubcategories returns all subcategories for out-of-context category`() {
        val result = CategoryFilterLogic.resolveVisibleSubcategories(
            category = ExpenseCategory.HOME,
            selectedSubcategories = emptySet(),
            groupContext = GroupContext.TRAVEL,
            showAllCategories = false
        )

        assertEquals(ExpenseSubcategory.forCategory(ExpenseCategory.HOME), result)
    }

    @Test
    fun `resolveFocusedCategory returns null when no categories are selected`() {
        val result = CategoryFilterLogic.resolveFocusedCategory(
            currentFocused = ExpenseCategory.FOOD,
            selectedCategories = emptySet(),
            visibleCategories = ExpenseCategory.entries
        )

        assertNull(result)
    }

    @Test
    fun `resolveFocusedCategory preserves currentFocused when still selected and visible`() {
        val result = CategoryFilterLogic.resolveFocusedCategory(
            currentFocused = ExpenseCategory.FOOD,
            selectedCategories = setOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD),
            visibleCategories = ExpenseCategory.entries
        )

        assertEquals(ExpenseCategory.FOOD, result)
    }

    @Test
    fun `resolveFocusedCategory falls back to first selected in visible order when unfocused`() {
        val result = CategoryFilterLogic.resolveFocusedCategory(
            currentFocused = null,
            selectedCategories = setOf(ExpenseCategory.SHOPPING, ExpenseCategory.FOOD),
            visibleCategories = ExpenseCategory.entries
        )

        assertEquals(ExpenseCategory.FOOD, result)
    }

    @Test
    fun `handleCategoryClick selects unselected category and focuses it`() {
        val result = CategoryFilterLogic.handleCategoryClick(
            criteria = ExpenseFilterCriteria(),
            clickedCategory = ExpenseCategory.TRANSPORT,
            focusedCategory = null,
            visibleCategories = ExpenseCategory.entries
        )

        assertEquals(setOf(ExpenseCategory.TRANSPORT), result.updatedCriteria.selectedCategories)
        assertEquals(ExpenseCategory.TRANSPORT, result.nextFocusedCategory)
    }

    @Test
    fun `handleCategoryClick focuses selected unfocused category without clearing subcategories`() {
        val criteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT),
            selectedSubcategories = setOf(
                ExpenseSubcategory.RESTAURANT,
                ExpenseSubcategory.TAXI_RIDESHARE
            )
        )

        val result = CategoryFilterLogic.handleCategoryClick(
            criteria = criteria,
            clickedCategory = ExpenseCategory.FOOD,
            focusedCategory = ExpenseCategory.TRANSPORT,
            visibleCategories = ExpenseCategory.entries
        )

        assertEquals(criteria, result.updatedCriteria)
        assertEquals(ExpenseCategory.FOOD, result.nextFocusedCategory)
    }

    @Test
    fun `handleCategoryClick deselects focused category and shifts focus to next selected`() {
        val criteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT),
            selectedSubcategories = setOf(
                ExpenseSubcategory.RESTAURANT,
                ExpenseSubcategory.TAXI_RIDESHARE
            )
        )

        val result = CategoryFilterLogic.handleCategoryClick(
            criteria = criteria,
            clickedCategory = ExpenseCategory.FOOD,
            focusedCategory = ExpenseCategory.FOOD,
            visibleCategories = ExpenseCategory.entries
        )

        assertEquals(setOf(ExpenseCategory.TRANSPORT), result.updatedCriteria.selectedCategories)
        assertEquals(setOf(ExpenseSubcategory.TAXI_RIDESHARE), result.updatedCriteria.selectedSubcategories)
        assertEquals(ExpenseCategory.TRANSPORT, result.nextFocusedCategory)
    }

    @Test
    fun `handleCategoryClick deselects last remaining focused category and clears focus to null`() {
        val criteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.FOOD),
            selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT)
        )

        val result = CategoryFilterLogic.handleCategoryClick(
            criteria = criteria,
            clickedCategory = ExpenseCategory.FOOD,
            focusedCategory = ExpenseCategory.FOOD,
            visibleCategories = ExpenseCategory.entries
        )

        assertTrue(result.updatedCriteria.selectedCategories.isEmpty())
        assertTrue(result.updatedCriteria.selectedSubcategories.isEmpty())
        assertNull(result.nextFocusedCategory)
    }

    @Test
    fun `toggleSubcategory adds unselected subcategory and removes selected subcategory`() {
        val added = CategoryFilterLogic.toggleSubcategory(
            criteria = ExpenseFilterCriteria(),
            subcategory = ExpenseSubcategory.RESTAURANT
        )
        assertEquals(setOf(ExpenseSubcategory.RESTAURANT), added.selectedSubcategories)

        val removed = CategoryFilterLogic.toggleSubcategory(
            criteria = added,
            subcategory = ExpenseSubcategory.RESTAURANT
        )
        assertTrue(removed.selectedSubcategories.isEmpty())
    }

    @Test
    fun `countSelectedSubcategories counts only subcategories belonging to the target category`() {
        val criteria = ExpenseFilterCriteria(
            selectedSubcategories = setOf(
                ExpenseSubcategory.RESTAURANT,
                ExpenseSubcategory.CAFE_BREAKFAST,
                ExpenseSubcategory.TAXI_RIDESHARE
            )
        )

        assertEquals(2, CategoryFilterLogic.countSelectedSubcategories(criteria, ExpenseCategory.FOOD))
        assertEquals(1, CategoryFilterLogic.countSelectedSubcategories(criteria, ExpenseCategory.TRANSPORT))
        assertEquals(0, CategoryFilterLogic.countSelectedSubcategories(criteria, ExpenseCategory.LODGING))
    }
}
