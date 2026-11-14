package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R

@StringRes
fun PayerType.toFundingSourceStringRes(): Int = when (this) {
    PayerType.GROUP -> R.string.funding_source_group_pocket
    PayerType.USER -> R.string.funding_source_your_money
    PayerType.SUBUNIT -> error("PayerType.SUBUNIT is not user-selectable")
}

@StringRes
fun PayerType.toFundingSourceDescriptionRes(): Int = when (this) {
    PayerType.GROUP -> R.string.funding_source_group_pocket_description
    PayerType.USER -> R.string.funding_source_your_money_description
    PayerType.SUBUNIT -> error("PayerType.SUBUNIT is not user-selectable")
}
