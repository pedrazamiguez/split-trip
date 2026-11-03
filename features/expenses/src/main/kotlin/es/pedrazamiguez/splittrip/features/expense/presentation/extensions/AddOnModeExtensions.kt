package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.features.expense.R

@StringRes
fun AddOnMode.toStringRes(): Int = when (this) {
    AddOnMode.ON_TOP -> R.string.add_on_mode_on_top
    AddOnMode.INCLUDED -> R.string.add_on_mode_included
}

@StringRes
fun AddOnMode.toDetailStringRes(): Int = when (this) {
    AddOnMode.ON_TOP -> R.string.expense_detail_addon_mode_on_top
    AddOnMode.INCLUDED -> R.string.expense_detail_addon_mode_included
}
