package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CategoryFilterLogicTest {

    @Test
    fun `resolveVisibleCategories returns empty list when available and selected are both empty`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            availableCategories = emptyList(),
            selectedCategories = emptySet()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `resolveVisibleCategories returns only available categories in canonical enum order`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            availableCategories = listOf(
                ExpenseCategory.SHOPPING,
                ExpenseCategory.TRANSPORT,
                ExpenseCategory.FOOD
            ),
            selectedCategories = emptySet()
        )

        assertEquals(
            listOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD, ExpenseCategory.SHOPPING),
            result
        )
    }

    @Test
    fun `resolveVisibleCategories retains pre-selected categories missing from available in canonical order`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            availableCategories = listOf(ExpenseCategory.FOOD),
            selectedCategories = setOf(ExpenseCategory.HOME, ExpenseCategory.TRANSPORT)
        )

        assertEquals(
            listOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD, ExpenseCategory.HOME),
            result
        )
    }

    @Test
    fun `resolveVisibleCategories deduplicates categories present in both available and selected`() {
        val result = CategoryFilterLogic.resolveVisibleCategories(
            availableCategories = listOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT),
            selectedCategories = setOf(ExpenseCategory.FOOD)
        )

        assertEquals(listOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD), result)
    }

    @Test
    fun `resolveVisibleSubcategories returns empty list when none belong to category`() {
        val result = CategoryFilterLogic.resolveVisibleSubcategories(
            category = ExpenseCategory.FOOD,
            availableSubcategories = listOf(ExpenseSubcategory.TAXI_RIDESHARE),
            selectedSubcategories = setOf(ExpenseSubcategory.HOTEL)
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `resolveVisibleSubcategories returns available subcategories for category in canonical order`() {
        val result = CategoryFilterLogic.resolveVisibleSubcategories(
            category = ExpenseCategory.FOOD,
            availableSubcategories = listOf(
                ExpenseSubcategory.DELIVERY,
                ExpenseSubcategory.TAXI_RIDESHARE,
                ExpenseSubcategory.RESTAURANT
            ),
            selectedSubcategories = emptySet()
        )

        assertEquals(listOf(ExpenseSubcategory.RESTAURANT, ExpenseSubcategory.DELIVERY), result)
    }

    @Test
    fun `resolveVisibleSubcategories retains pre-selected subcategories for category missing from available`() {
        val result = CategoryFilterLogic.resolveVisibleSubcategories(
            category = ExpenseCategory.FOOD,
            availableSubcategories = listOf(ExpenseSubcategory.RESTAURANT),
            selectedSubcategories = setOf(ExpenseSubcategory.CAFE_BREAKFAST, ExpenseSubcategory.HOTEL)
        )

        assertEquals(listOf(ExpenseSubcategory.RESTAURANT, ExpenseSubcategory.CAFE_BREAKFAST), result)
    }

    @Test
    fun `resolveVisibleSubcategories excludes UNSPECIFIED even if present in available or selected`() {
        val result = CategoryFilterLogic.resolveVisibleSubcategories(
            category = ExpenseCategory.OTHER,
            availableSubcategories = listOf(ExpenseSubcategory.UNSPECIFIED, ExpenseSubcategory.LAUNDRY),
            selectedSubcategories = setOf(ExpenseSubcategory.UNSPECIFIED)
        )

        assertEquals(listOf(ExpenseSubcategory.LAUNDRY), result)
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
