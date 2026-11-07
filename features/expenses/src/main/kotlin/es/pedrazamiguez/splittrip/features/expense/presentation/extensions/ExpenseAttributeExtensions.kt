package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CirclePlus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.domain.enums.ExpenseAttribute
import es.pedrazamiguez.splittrip.features.expense.R

@StringRes
fun ExpenseAttribute.toStringRes(): Int = when (this) {
    ExpenseAttribute.HAS_RECEIPT -> R.string.expense_attribute_has_receipt
    ExpenseAttribute.HAS_NOTE -> R.string.expense_attribute_has_note
    ExpenseAttribute.HAS_EXTRAS -> R.string.expense_attribute_has_extras
}

fun ExpenseAttribute.toIconVector(): ImageVector = when (this) {
    ExpenseAttribute.HAS_RECEIPT -> TablerIcons.Outline.Receipt
    ExpenseAttribute.HAS_NOTE -> TablerIcons.Outline.AlignJustified
    ExpenseAttribute.HAS_EXTRAS -> TablerIcons.Outline.CirclePlus
}
