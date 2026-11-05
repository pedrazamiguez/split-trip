package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptEuro
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TipJarEuro
import es.pedrazamiguez.splittrip.domain.enums.AddOnType

fun AddOnType.toIconVector(): ImageVector = when (this) {
    AddOnType.FEE -> TablerIcons.Outline.ReceiptEuro
    AddOnType.SURCHARGE -> TablerIcons.Outline.Receipt
    AddOnType.TIP -> TablerIcons.Outline.TipJarEuro
    AddOnType.DISCOUNT -> TablerIcons.Outline.Receipt
}
