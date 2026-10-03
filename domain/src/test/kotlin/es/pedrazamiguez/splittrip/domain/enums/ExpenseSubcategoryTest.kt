package es.pedrazamiguez.splittrip.domain.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ExpenseSubcategoryTest {

    @Nested
    inner class FromString {

        @ParameterizedTest
        @EnumSource(ExpenseSubcategory::class)
        fun `resolves all enum entries by exact name`(subcategory: ExpenseSubcategory) {
            assertEquals(subcategory, ExpenseSubcategory.fromString(subcategory.name))
        }

        @Test
        fun `resolves case-insensitive input`() {
            assertEquals(ExpenseSubcategory.INTERNATIONAL_FLIGHT, ExpenseSubcategory.fromString("international_flight"))
            assertEquals(ExpenseSubcategory.PRIVATE_TRANSFER, ExpenseSubcategory.fromString("private_transfer"))
            assertEquals(ExpenseSubcategory.RESTAURANT, ExpenseSubcategory.fromString("restaurant"))
            assertEquals(ExpenseSubcategory.HOTEL, ExpenseSubcategory.fromString("hotel"))
        }

        @Test
        fun `throws IllegalArgumentException for unknown subcategory`() {
            assertThrows(IllegalArgumentException::class.java) {
                ExpenseSubcategory.fromString("NONEXISTENT")
            }
        }
    }

    @Nested
    inner class ForCategory {

        @ParameterizedTest
        @EnumSource(ExpenseCategory::class)
        fun `returns subcategories matching parent category excluding UNSPECIFIED`(category: ExpenseCategory) {
            val subcategories = ExpenseSubcategory.forCategory(category)
            assertTrue(subcategories.all { it.parentCategory == category })
            assertFalse(subcategories.contains(ExpenseSubcategory.UNSPECIFIED))
        }

        @Test
        fun `returns correct counts for each category`() {
            assertEquals(12, ExpenseSubcategory.forCategory(ExpenseCategory.TRANSPORT).size)
            assertEquals(6, ExpenseSubcategory.forCategory(ExpenseCategory.FOOD).size)
            assertEquals(6, ExpenseSubcategory.forCategory(ExpenseCategory.LODGING).size)
            assertEquals(7, ExpenseSubcategory.forCategory(ExpenseCategory.ACTIVITIES).size)
            assertEquals(5, ExpenseSubcategory.forCategory(ExpenseCategory.ENTERTAINMENT).size)
            assertEquals(5, ExpenseSubcategory.forCategory(ExpenseCategory.SHOPPING).size)
            assertEquals(4, ExpenseSubcategory.forCategory(ExpenseCategory.INSURANCE).size)
            assertEquals(4, ExpenseSubcategory.forCategory(ExpenseCategory.OTHER).size)
        }
    }
}
