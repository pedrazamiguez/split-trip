package es.pedrazamiguez.splittrip.domain.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ExpenseCategoryTest {

    @Nested
    inner class FromString {

        @ParameterizedTest
        @EnumSource(ExpenseCategory::class)
        fun `resolves all enum entries by exact name`(category: ExpenseCategory) {
            assertEquals(category, ExpenseCategory.fromString(category.name))
        }

        @Test
        fun `resolves case-insensitive input`() {
            assertEquals(ExpenseCategory.TRANSPORT, ExpenseCategory.fromString("transport"))
            assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromString("food"))
            assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromString("other"))
        }

        @Test
        fun `throws IllegalArgumentException for unknown category`() {
            assertThrows(IllegalArgumentException::class.java) {
                ExpenseCategory.fromString("NONEXISTENT")
            }
        }

        @Test
        fun `throws IllegalArgumentException for removed categories CONTRIBUTION and REFUND`() {
            assertThrows(IllegalArgumentException::class.java) {
                ExpenseCategory.fromString("CONTRIBUTION")
            }
            assertThrows(IllegalArgumentException::class.java) {
                ExpenseCategory.fromString("REFUND")
            }
        }
    }

    @Nested
    inner class ContextFiltering {

        @Test
        fun `forContext TRAVEL returns 8 travel categories excluding HOME and HEALTH_PERSONAL_CARE`() {
            val travelCategories = ExpenseCategory.forContext(GroupContext.TRAVEL)

            assertEquals(8, travelCategories.size)
            assertFalse(travelCategories.contains(ExpenseCategory.HOME))
            assertFalse(travelCategories.contains(ExpenseCategory.HEALTH_PERSONAL_CARE))
            assertTrue(travelCategories.contains(ExpenseCategory.LODGING))
            assertTrue(travelCategories.contains(ExpenseCategory.ACTIVITIES))
        }

        @Test
        fun `forContext EVERYDAY returns 8 everyday categories excluding LODGING and ACTIVITIES`() {
            val everydayCategories = ExpenseCategory.forContext(GroupContext.EVERYDAY)

            assertEquals(8, everydayCategories.size)
            assertFalse(everydayCategories.contains(ExpenseCategory.LODGING))
            assertFalse(everydayCategories.contains(ExpenseCategory.ACTIVITIES))
            assertTrue(everydayCategories.contains(ExpenseCategory.HOME))
            assertTrue(everydayCategories.contains(ExpenseCategory.HEALTH_PERSONAL_CARE))
        }

        @Test
        fun `every category applies to at least one GroupContext`() {
            ExpenseCategory.entries.forEach { category ->
                assertTrue(category.contexts.isNotEmpty())
            }
        }

        @Test
        fun `preserves all 8 legacy enum names for backward compatibility`() {
            val legacyNames = listOf(
                "TRANSPORT",
                "FOOD",
                "LODGING",
                "ACTIVITIES",
                "INSURANCE",
                "ENTERTAINMENT",
                "SHOPPING",
                "OTHER"
            )
            val actualNames = ExpenseCategory.entries.map { it.name }
            assertTrue(actualNames.containsAll(legacyNames))
        }
    }
}
