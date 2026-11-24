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
            assertEquals(13, ExpenseSubcategory.forCategory(ExpenseCategory.TRANSPORT).size)
            assertEquals(6, ExpenseSubcategory.forCategory(ExpenseCategory.FOOD).size)
            assertEquals(6, ExpenseSubcategory.forCategory(ExpenseCategory.LODGING).size)
            assertEquals(5, ExpenseSubcategory.forCategory(ExpenseCategory.HOME).size)
            assertEquals(4, ExpenseSubcategory.forCategory(ExpenseCategory.HEALTH_PERSONAL_CARE).size)
            assertEquals(7, ExpenseSubcategory.forCategory(ExpenseCategory.ACTIVITIES).size)
            assertEquals(6, ExpenseSubcategory.forCategory(ExpenseCategory.ENTERTAINMENT).size)
            assertEquals(8, ExpenseSubcategory.forCategory(ExpenseCategory.SHOPPING).size)
            assertEquals(5, ExpenseSubcategory.forCategory(ExpenseCategory.INSURANCE).size)
            assertEquals(5, ExpenseSubcategory.forCategory(ExpenseCategory.OTHER).size)
        }
    }

    @Nested
    inner class ForCategoryAndContext {

        @Test
        fun `forCategory with TRAVEL excludes everyday-only subcategories like CAR_REPAIR_MAINTENANCE and PET_CARE`() {
            val transport = ExpenseSubcategory.forCategory(ExpenseCategory.TRANSPORT, GroupContext.TRAVEL)
            val other = ExpenseSubcategory.forCategory(ExpenseCategory.OTHER, GroupContext.TRAVEL)
            val shopping = ExpenseSubcategory.forCategory(ExpenseCategory.SHOPPING, GroupContext.TRAVEL)

            assertFalse(transport.contains(ExpenseSubcategory.CAR_REPAIR_MAINTENANCE))
            assertTrue(transport.contains(ExpenseSubcategory.INTERNATIONAL_FLIGHT))
            assertFalse(other.contains(ExpenseSubcategory.PET_CARE))
            assertFalse(shopping.contains(ExpenseSubcategory.TOBACCO_VAPING))
            assertFalse(shopping.contains(ExpenseSubcategory.HOBBIES))
        }

        @Test
        fun `forCategory with EVERYDAY excludes travel-only subcategories`() {
            val transport = ExpenseSubcategory.forCategory(ExpenseCategory.TRANSPORT, GroupContext.EVERYDAY)
            val insurance = ExpenseSubcategory.forCategory(ExpenseCategory.INSURANCE, GroupContext.EVERYDAY)
            val shopping = ExpenseSubcategory.forCategory(ExpenseCategory.SHOPPING, GroupContext.EVERYDAY)

            assertFalse(transport.contains(ExpenseSubcategory.INTERNATIONAL_FLIGHT))
            assertTrue(transport.contains(ExpenseSubcategory.CAR_REPAIR_MAINTENANCE))
            assertFalse(insurance.contains(ExpenseSubcategory.TRAVEL_INSURANCE))
            assertTrue(insurance.contains(ExpenseSubcategory.HOME_INSURANCE))
            assertTrue(shopping.contains(ExpenseSubcategory.TOBACCO_VAPING))
            assertTrue(shopping.contains(ExpenseSubcategory.HOBBIES))
            assertTrue(shopping.contains(ExpenseSubcategory.PHARMACY_HEALTH))
            assertFalse(shopping.contains(ExpenseSubcategory.SOUVENIRS_GIFTS))
        }

        @Test
        fun `forCategory with EVERYDAY returns 5 subcategories for HOME and 4 for HEALTH_PERSONAL_CARE`() {
            assertEquals(5, ExpenseSubcategory.forCategory(ExpenseCategory.HOME, GroupContext.EVERYDAY).size)
            assertEquals(
                4,
                ExpenseSubcategory.forCategory(ExpenseCategory.HEALTH_PERSONAL_CARE, GroupContext.EVERYDAY).size
            )
        }

        @Test
        fun `PHARMACY_HEALTH is available in both TRAVEL and EVERYDAY under SHOPPING without duplicates`() {
            val travelShopping = ExpenseSubcategory.forCategory(ExpenseCategory.SHOPPING, GroupContext.TRAVEL)
            val everydayShopping = ExpenseSubcategory.forCategory(ExpenseCategory.SHOPPING, GroupContext.EVERYDAY)

            assertTrue(travelShopping.contains(ExpenseSubcategory.PHARMACY_HEALTH))
            assertTrue(everydayShopping.contains(ExpenseSubcategory.PHARMACY_HEALTH))
            assertEquals(
                1,
                ExpenseSubcategory.entries.count { it.name.contains("PHARMACY") }
            )
        }

        @Test
        fun `every non-UNSPECIFIED subcategory applies to at least one GroupContext`() {
            ExpenseSubcategory.entries
                .filter { it != ExpenseSubcategory.UNSPECIFIED }
                .forEach { subcategory ->
                    assertTrue(subcategory.contexts.isNotEmpty())
                }
        }

        @Test
        fun `never includes UNSPECIFIED for any category and context`() {
            ExpenseCategory.entries.forEach { category ->
                GroupContext.entries.forEach { context ->
                    assertFalse(
                        ExpenseSubcategory.forCategory(category, context).contains(ExpenseSubcategory.UNSPECIFIED)
                    )
                }
            }
        }
    }
}
