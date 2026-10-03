package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.core.common.R as CommonR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Keyframes
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Theater
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseSubcategoryExtensionsTest {

    @Nested
    inner class StringResMapping {

        @Test
        fun `maps international flight to string resource`() {
            assertEquals(
                CommonR.string.expense_subcategory_international_flight,
                ExpenseSubcategory.INTERNATIONAL_FLIGHT.toStringRes()
            )
        }

        @Test
        fun `maps private transfer to string resource`() {
            assertEquals(
                CommonR.string.expense_subcategory_private_transfer,
                ExpenseSubcategory.PRIVATE_TRANSFER.toStringRes()
            )
        }

        @Test
        fun `maps restaurant to string resource`() {
            assertEquals(
                CommonR.string.expense_subcategory_restaurant,
                ExpenseSubcategory.RESTAURANT.toStringRes()
            )
        }

        @Test
        fun `maps hotel to string resource`() {
            assertEquals(
                CommonR.string.expense_subcategory_hotel,
                ExpenseSubcategory.HOTEL.toStringRes()
            )
        }

        @Test
        fun `maps unspecified to string resource`() {
            assertEquals(
                CommonR.string.expense_subcategory_unspecified,
                ExpenseSubcategory.UNSPECIFIED.toStringRes()
            )
        }

        @Test
        fun `all subcategories map to distinct string resources`() {
            val resIds = ExpenseSubcategory.entries.map { it.toStringRes() }
            assertEquals(resIds.size, resIds.toSet().size)
        }
    }

    @Nested
    inner class IconVectorMapping {

        @Test
        fun `maps private transfer to keyframes icon`() {
            assertSame(
                TablerIcons.Outline.Keyframes,
                ExpenseSubcategory.PRIVATE_TRANSFER.toIconVector()
            )
        }

        @Test
        fun `maps cinema theater to theater icon`() {
            assertSame(
                TablerIcons.Outline.Theater,
                ExpenseSubcategory.CINEMA_THEATER.toIconVector()
            )
        }

        @Test
        fun `all subcategories return non-null icon vectors`() {
            ExpenseSubcategory.entries.forEach { subcategory ->
                assertNotNull(subcategory.toIconVector(), "Icon for $subcategory should not be null")
            }
        }

        @Test
        fun `icon vectors are cached — same reference returned on repeated calls`() {
            assertSame(
                ExpenseSubcategory.INTERNATIONAL_FLIGHT.toIconVector(),
                ExpenseSubcategory.INTERNATIONAL_FLIGHT.toIconVector()
            )
        }
    }
}
