package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.core.common.R as CommonR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bed
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Category
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.MasksTheater
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Route
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Run
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShoppingBag
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ToolsKitchen2
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseCategoryExtensionsTest {

    @Nested
    inner class StringResMapping {

        @Test
        fun `TRANSPORT maps to transport string resource`() {
            assertEquals(CommonR.string.expense_category_transport, ExpenseCategory.TRANSPORT.toStringRes())
        }

        @Test
        fun `FOOD maps to food string resource`() {
            assertEquals(CommonR.string.expense_category_food, ExpenseCategory.FOOD.toStringRes())
        }

        @Test
        fun `LODGING maps to lodging string resource`() {
            assertEquals(CommonR.string.expense_category_lodging, ExpenseCategory.LODGING.toStringRes())
        }

        @Test
        fun `ACTIVITIES maps to activities string resource`() {
            assertEquals(CommonR.string.expense_category_activities, ExpenseCategory.ACTIVITIES.toStringRes())
        }

        @Test
        fun `INSURANCE maps to insurance string resource`() {
            assertEquals(CommonR.string.expense_category_insurance, ExpenseCategory.INSURANCE.toStringRes())
        }

        @Test
        fun `ENTERTAINMENT maps to entertainment string resource`() {
            assertEquals(CommonR.string.expense_category_entertainment, ExpenseCategory.ENTERTAINMENT.toStringRes())
        }

        @Test
        fun `SHOPPING maps to shopping string resource`() {
            assertEquals(CommonR.string.expense_category_shopping, ExpenseCategory.SHOPPING.toStringRes())
        }

        @Test
        fun `OTHER maps to other string resource`() {
            assertEquals(CommonR.string.expense_category_other, ExpenseCategory.OTHER.toStringRes())
        }

        @Test
        fun `all categories map to distinct string resources`() {
            val resIds = ExpenseCategory.entries.map { it.toStringRes() }
            assertEquals(resIds.size, resIds.toSet().size)
        }
    }

    @Nested
    inner class IconVectorMapping {

        @Test
        fun `TRANSPORT maps to Route icon`() {
            assertSame(TablerIcons.Outline.Route, ExpenseCategory.TRANSPORT.toIconVector())
        }

        @Test
        fun `FOOD maps to ToolsKitchen2 icon`() {
            assertSame(TablerIcons.Outline.ToolsKitchen2, ExpenseCategory.FOOD.toIconVector())
        }

        @Test
        fun `LODGING maps to Bed icon`() {
            assertSame(TablerIcons.Outline.Bed, ExpenseCategory.LODGING.toIconVector())
        }

        @Test
        fun `ACTIVITIES maps to Run icon`() {
            assertSame(TablerIcons.Outline.Run, ExpenseCategory.ACTIVITIES.toIconVector())
        }

        @Test
        fun `INSURANCE maps to Shield icon`() {
            assertSame(TablerIcons.Outline.Shield, ExpenseCategory.INSURANCE.toIconVector())
        }

        @Test
        fun `ENTERTAINMENT maps to MasksTheater icon`() {
            assertSame(TablerIcons.Outline.MasksTheater, ExpenseCategory.ENTERTAINMENT.toIconVector())
        }

        @Test
        fun `SHOPPING maps to ShoppingBag icon`() {
            assertSame(TablerIcons.Outline.ShoppingBag, ExpenseCategory.SHOPPING.toIconVector())
        }

        @Test
        fun `OTHER maps to Category icon`() {
            assertSame(TablerIcons.Outline.Category, ExpenseCategory.OTHER.toIconVector())
        }

        @Test
        fun `all categories map to distinct icon vectors`() {
            val icons = ExpenseCategory.entries.map { it.toIconVector() }
            assertEquals(icons.size, icons.toSet().size)
        }

        @Test
        fun `all categories return non-null icon vectors`() {
            ExpenseCategory.entries.forEach { category ->
                assertNotNull(category.toIconVector(), "Icon for $category should not be null")
            }
        }

        @Test
        fun `icon vectors are cached — same reference returned on repeated calls`() {
            assertSame(
                ExpenseCategory.TRANSPORT.toIconVector(),
                ExpenseCategory.TRANSPORT.toIconVector()
            )
        }
    }
}
