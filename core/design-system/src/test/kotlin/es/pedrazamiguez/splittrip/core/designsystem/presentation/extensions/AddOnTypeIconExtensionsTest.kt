package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptEuro
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TipJarEuro
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AddOnTypeIconExtensionsTest {

    @Test
    fun `toIconVector returns ReceiptEuro icon for FEE`() {
        assertEquals(TablerIcons.Outline.ReceiptEuro, AddOnType.FEE.toIconVector())
    }

    @Test
    fun `toIconVector returns Receipt icon for SURCHARGE`() {
        assertEquals(TablerIcons.Outline.Receipt, AddOnType.SURCHARGE.toIconVector())
    }

    @Test
    fun `toIconVector returns TipJarEuro icon for TIP`() {
        assertEquals(TablerIcons.Outline.TipJarEuro, AddOnType.TIP.toIconVector())
    }

    @Test
    fun `toIconVector returns Receipt icon for DISCOUNT`() {
        assertEquals(TablerIcons.Outline.Receipt, AddOnType.DISCOUNT.toIconVector())
    }
}
