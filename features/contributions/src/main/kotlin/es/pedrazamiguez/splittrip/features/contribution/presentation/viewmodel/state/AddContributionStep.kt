package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoItem
import es.pedrazamiguez.splittrip.features.contribution.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wizard steps for the Add Contribution flow.
 *
 * All steps are required — there are no optional steps in this short wizard.
 *
 * @property infoTitleRes String resource ID for the contextual step info bottom sheet title.
 * @property infoDescriptionRes String resource ID for the contextual step info bottom sheet body.
 * @property infoItems Optional structured option items displayed below the intro description.
 * @property isOptional When `true` the step can be skipped via "Skip to Review".
 *                      Currently all contribution steps are required.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class AddContributionStep(
    @StringRes val infoTitleRes: Int,
    @StringRes val infoDescriptionRes: Int,
    val infoItems: ImmutableList<WizardStepInfoItem> = persistentListOf(),
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    AMOUNT(
        infoTitleRes = R.string.contribution_step_info_amount_title,
        infoDescriptionRes = R.string.contribution_step_info_amount_description
    ),
    SCOPE(
        infoTitleRes = R.string.contribution_step_info_scope_title,
        infoDescriptionRes = R.string.contribution_step_info_scope_description,
        infoItems = persistentListOf(
            WizardStepInfoItem(
                titleRes = R.string.contribution_step_info_scope_item_for_group_title,
                descriptionRes = R.string.contribution_step_info_scope_item_for_group_description
            ),
            WizardStepInfoItem(
                titleRes = R.string.contribution_step_info_scope_item_for_subunit_title,
                descriptionRes = R.string.contribution_step_info_scope_item_for_subunit_description
            ),
            WizardStepInfoItem(
                titleRes = R.string.contribution_step_info_scope_item_for_you_title,
                descriptionRes = R.string.contribution_step_info_scope_item_for_you_description
            )
        )
    ),
    DATE(
        infoTitleRes = R.string.contribution_step_info_date_title,
        infoDescriptionRes = R.string.contribution_step_info_date_description
    ),
    REVIEW(
        infoTitleRes = R.string.contribution_step_info_review_title,
        infoDescriptionRes = R.string.contribution_step_info_review_description,
        isReview = true
    )
}
