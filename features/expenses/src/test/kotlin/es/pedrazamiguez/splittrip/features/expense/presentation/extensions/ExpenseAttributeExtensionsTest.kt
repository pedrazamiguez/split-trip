package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CirclePlus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.domain.enums.ExpenseAttribute
import es.pedrazamiguez.splittrip.features.expense.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseAttributeExtensionsTest {

    @Nested
    inner class ToStringRes {

        @Test
        fun `HAS_RECEIPT maps to expense_attribute_has_receipt string resource`() {
            assertEquals(R.string.expense_attribute_has_receipt, ExpenseAttribute.HAS_RECEIPT.toStringRes())
        }

        @Test
        fun `HAS_NOTE maps to expense_attribute_has_note string resource`() {
            assertEquals(R.string.expense_attribute_has_note, ExpenseAttribute.HAS_NOTE.toStringRes())
        }

        @Test
        fun `HAS_EXTRAS maps to expense_attribute_has_extras string resource`() {
            assertEquals(R.string.expense_attribute_has_extras, ExpenseAttribute.HAS_EXTRAS.toStringRes())
        }

        @Test
        fun `all attributes map to distinct string resources`() {
            val ids = ExpenseAttribute.entries.map { it.toStringRes() }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Nested
    inner class ToIconVector {

        @Test
        fun `HAS_RECEIPT maps to Receipt icon`() {
            assertEquals(TablerIcons.Outline.Receipt, ExpenseAttribute.HAS_RECEIPT.toIconVector())
        }

        @Test
        fun `HAS_NOTE maps to AlignJustified icon`() {
            assertEquals(TablerIcons.Outline.AlignJustified, ExpenseAttribute.HAS_NOTE.toIconVector())
        }

        @Test
        fun `HAS_EXTRAS maps to CirclePlus icon`() {
            assertEquals(TablerIcons.Outline.CirclePlus, ExpenseAttribute.HAS_EXTRAS.toIconVector())
        }

        @Test
        fun `all attributes map to distinct icons`() {
            val icons = ExpenseAttribute.entries.map { it.toIconVector() }
            assertEquals(icons.size, icons.toSet().size)
        }
    }
}
