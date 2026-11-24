package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CategorySuggestionTest {

    @Test
    fun `constructor accepts valid category and matching subcategory`() {
        val suggestion = CategorySuggestion(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.RESTAURANT,
            source = CategorySuggestionSource.LOCAL_RULES
        )

        assertEquals(ExpenseCategory.FOOD, suggestion.category)
        assertEquals(ExpenseSubcategory.RESTAURANT, suggestion.subcategory)
        assertEquals(CategorySuggestionSource.LOCAL_RULES, suggestion.source)
    }

    @Test
    fun `constructor accepts null subcategory`() {
        val suggestion = CategorySuggestion(
            category = ExpenseCategory.TRANSPORT,
            subcategory = null,
            source = CategorySuggestionSource.ON_DEVICE_AI
        )

        assertEquals(ExpenseCategory.TRANSPORT, suggestion.category)
        assertNull(suggestion.subcategory)
        assertEquals(CategorySuggestionSource.ON_DEVICE_AI, suggestion.source)
    }

    @Test
    fun `constructor rejects subcategory belonging to a different parent category`() {
        assertThrows(IllegalArgumentException::class.java) {
            CategorySuggestion(
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.HOTEL
            )
        }
    }

    @Test
    fun `constructor rejects UNSPECIFIED subcategory`() {
        assertThrows(IllegalArgumentException::class.java) {
            CategorySuggestion(
                category = ExpenseCategory.OTHER,
                subcategory = ExpenseSubcategory.UNSPECIFIED
            )
        }
    }

    @Test
    fun `sanitized keeps valid category and matching subcategory`() {
        val suggestion = CategorySuggestion.sanitized(
            category = ExpenseCategory.HOME,
            subcategory = ExpenseSubcategory.UTILITIES,
            source = CategorySuggestionSource.ON_DEVICE_AI
        )

        assertEquals(ExpenseCategory.HOME, suggestion.category)
        assertEquals(ExpenseSubcategory.UTILITIES, suggestion.subcategory)
        assertEquals(CategorySuggestionSource.ON_DEVICE_AI, suggestion.source)
    }

    @Test
    fun `sanitized degrades mismatched subcategory to null`() {
        val suggestion = CategorySuggestion.sanitized(
            category = ExpenseCategory.FOOD,
            subcategory = ExpenseSubcategory.HOTEL,
            source = CategorySuggestionSource.ON_DEVICE_AI
        )

        assertEquals(ExpenseCategory.FOOD, suggestion.category)
        assertNull(suggestion.subcategory)
        assertEquals(CategorySuggestionSource.ON_DEVICE_AI, suggestion.source)
    }

    @Test
    fun `sanitized degrades UNSPECIFIED subcategory to null`() {
        val suggestion = CategorySuggestion.sanitized(
            category = ExpenseCategory.OTHER,
            subcategory = ExpenseSubcategory.UNSPECIFIED
        )

        assertEquals(ExpenseCategory.OTHER, suggestion.category)
        assertNull(suggestion.subcategory)
    }
}
