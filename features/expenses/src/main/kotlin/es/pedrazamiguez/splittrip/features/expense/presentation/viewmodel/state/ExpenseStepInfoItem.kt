package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import androidx.annotation.StringRes

/**
 * Represents a single structured option or concept row inside [ExpenseStepInfoBottomSheet].
 *
 * @property titleRes String resource ID for the option's title (matching the on-screen option label).
 * @property descriptionRes String resource ID for the concise explanation of the option.
 */
data class ExpenseStepInfoItem(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)
