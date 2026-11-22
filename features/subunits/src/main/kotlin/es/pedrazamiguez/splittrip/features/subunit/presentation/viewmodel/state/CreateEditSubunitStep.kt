package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoItem
import es.pedrazamiguez.splittrip.features.subunit.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Defines the wizard steps for the create/edit subunit flow.
 *
 * All four steps are always applicable (no conditional steps).
 * All steps are required — there are no optional steps in this wizard.
 *
 * NAME → MEMBERS → SHARES → REVIEW
 *
 * @property infoTitleRes String resource ID for the contextual info bottom sheet title.
 * @property infoDescriptionRes String resource ID for the contextual info bottom sheet description.
 * @property infoItems Optional structured list of key options explained in the info bottom sheet.
 * @property isOptional When `true` the step can be skipped via "Skip to Review".
 *                      Currently all subunit steps are required.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class CreateEditSubunitStep(
    @StringRes val infoTitleRes: Int,
    @StringRes val infoDescriptionRes: Int,
    val infoItems: ImmutableList<WizardStepInfoItem> = persistentListOf(),
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Subunit name input. */
    NAME(
        infoTitleRes = R.string.subunit_step_info_name_title,
        infoDescriptionRes = R.string.subunit_step_info_name_description
    ),

    /** Select members from the group. */
    MEMBERS(
        infoTitleRes = R.string.subunit_step_info_members_title,
        infoDescriptionRes = R.string.subunit_step_info_members_description
    ),

    /** Allocate percentage shares for selected members. */
    SHARES(
        infoTitleRes = R.string.subunit_step_info_shares_title,
        infoDescriptionRes = R.string.subunit_step_info_shares_description
    ),

    /** Read-only summary — final confirmation before saving. */
    REVIEW(
        infoTitleRes = R.string.subunit_step_info_review_title,
        infoDescriptionRes = R.string.subunit_step_info_review_description,
        isReview = true
    )
}
