package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.annotation.StringRes

/**
 * Represents a single structured option or concept row inside [WizardStepInfoBottomSheet].
 *
 * @property titleRes String resource ID for the option's title (matching the on-screen option label).
 * @property descriptionRes String resource ID for the concise explanation of the option.
 */
data class WizardStepInfoItem(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)
