package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel

internal data class SplitInputConfig(
    val value: String,
    val onValueChange: (String) -> Unit,
    @param:StringRes val unitRes: Int?,
    val unitLiteral: String?,
    @param:StringRes val fieldDescriptionRes: Int
)

internal fun resolveSplitInputConfig(
    split: SplitUiModel,
    isPercentMode: Boolean,
    currencySymbol: String?,
    onAmountChanged: (String) -> Unit,
    onPercentageChanged: (String) -> Unit
): SplitInputConfig = if (isPercentMode) {
    SplitInputConfig(
        value = split.percentageInput,
        onValueChange = onPercentageChanged,
        unitRes = R.string.add_expense_split_percentage_label,
        unitLiteral = null,
        fieldDescriptionRes = R.string.add_expense_split_percentage_label
    )
} else {
    SplitInputConfig(
        value = split.amountInput,
        onValueChange = onAmountChanged,
        unitRes = null,
        unitLiteral = currencySymbol,
        fieldDescriptionRes = R.string.add_expense_split_amount_label
    )
}

@StringRes
internal fun splitLockDescriptionRes(isLocked: Boolean): Int = if (isLocked) {
    R.string.add_expense_split_share_unlock
} else {
    R.string.add_expense_split_share_lock
}
