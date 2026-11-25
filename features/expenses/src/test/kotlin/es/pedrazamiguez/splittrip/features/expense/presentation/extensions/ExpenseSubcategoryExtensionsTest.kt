package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.core.common.R as CommonR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Car
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.DeviceTv
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Key
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Keyframes
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Palette
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Paw
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scissors
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Smoking
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Theater
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Tool
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
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
        fun `maps new everyday subcategories to expected string resources`() {
            assertEquals(
                CommonR.string.expense_subcategory_car_repair_maintenance,
                ExpenseSubcategory.CAR_REPAIR_MAINTENANCE.toStringRes()
            )
            assertEquals(
                CommonR.string.expense_subcategory_rent_mortgage,
                ExpenseSubcategory.RENT_MORTGAGE.toStringRes()
            )
            assertEquals(
                CommonR.string.expense_subcategory_hairdresser_barber,
                ExpenseSubcategory.HAIRDRESSER_BARBER.toStringRes()
            )
            assertEquals(
                CommonR.string.expense_subcategory_subscriptions_streaming,
                ExpenseSubcategory.SUBSCRIPTIONS_STREAMING.toStringRes()
            )
            assertEquals(
                CommonR.string.expense_subcategory_pet_care,
                ExpenseSubcategory.PET_CARE.toStringRes()
            )
            assertEquals(
                CommonR.string.expense_subcategory_tobacco_vaping,
                ExpenseSubcategory.TOBACCO_VAPING.toStringRes()
            )
            assertEquals(
                CommonR.string.expense_subcategory_hobbies,
                ExpenseSubcategory.HOBBIES.toStringRes()
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
        fun `maps taxi rideshare to car icon`() {
            assertSame(
                TablerIcons.Outline.Car,
                ExpenseSubcategory.TAXI_RIDESHARE.toIconVector()
            )
        }

        @Test
        fun `TRANSPORT category and TAXI_RIDESHARE subcategory map to distinct icons`() {
            assertNotSame(
                ExpenseCategory.TRANSPORT.toIconVector(),
                ExpenseSubcategory.TAXI_RIDESHARE.toIconVector()
            )
        }

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
        fun `maps new everyday subcategories to expected icon vectors`() {
            assertSame(TablerIcons.Outline.Tool, ExpenseSubcategory.CAR_REPAIR_MAINTENANCE.toIconVector())
            assertSame(TablerIcons.Outline.Key, ExpenseSubcategory.RENT_MORTGAGE.toIconVector())
            assertSame(TablerIcons.Outline.Scissors, ExpenseSubcategory.HAIRDRESSER_BARBER.toIconVector())
            assertSame(TablerIcons.Outline.DeviceTv, ExpenseSubcategory.SUBSCRIPTIONS_STREAMING.toIconVector())
            assertSame(TablerIcons.Outline.Paw, ExpenseSubcategory.PET_CARE.toIconVector())
            assertSame(TablerIcons.Outline.Smoking, ExpenseSubcategory.TOBACCO_VAPING.toIconVector())
            assertSame(TablerIcons.Outline.Palette, ExpenseSubcategory.HOBBIES.toIconVector())
        }

        @Test
        fun `all subcategories return non-null icon vectors`() {
            ExpenseSubcategory.entries.forEach { subcategory ->
                assertNotNull(subcategory.toIconVector(), "Icon for $subcategory should not be null")
            }
        }

        @Test
        fun `no subcategory shares the same icon vector as its parent category`() {
            ExpenseSubcategory.entries.forEach { subcategory ->
                assertNotSame(
                    subcategory.parentCategory.toIconVector(),
                    subcategory.toIconVector(),
                    "Subcategory $subcategory must not share icon with parent ${subcategory.parentCategory}"
                )
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
