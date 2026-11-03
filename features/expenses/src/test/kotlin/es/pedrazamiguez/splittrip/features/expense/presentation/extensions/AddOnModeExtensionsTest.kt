package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.features.expense.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddOnModeExtensionsTest {

    @Nested
    inner class ToStringRes {

        @Test
        fun `maps ON_TOP to add_on_mode_on_top`() {
            assertEquals(R.string.add_on_mode_on_top, AddOnMode.ON_TOP.toStringRes())
        }

        @Test
        fun `maps INCLUDED to add_on_mode_included`() {
            assertEquals(R.string.add_on_mode_included, AddOnMode.INCLUDED.toStringRes())
        }
    }

    @Nested
    inner class ToDetailStringRes {

        @Test
        fun `maps ON_TOP to expense_detail_addon_mode_on_top`() {
            assertEquals(R.string.expense_detail_addon_mode_on_top, AddOnMode.ON_TOP.toDetailStringRes())
        }

        @Test
        fun `maps INCLUDED to expense_detail_addon_mode_included`() {
            assertEquals(R.string.expense_detail_addon_mode_included, AddOnMode.INCLUDED.toDetailStringRes())
        }
    }
}
