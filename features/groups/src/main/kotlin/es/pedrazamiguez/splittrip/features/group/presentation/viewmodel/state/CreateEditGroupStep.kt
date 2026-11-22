package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import androidx.annotation.StringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepInfoItem
import es.pedrazamiguez.splittrip.features.group.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Defines the wizard steps for the create/edit group flow.
 *
 * @property infoTitleRes String resource ID for the contextual step info bottom sheet title.
 * @property infoDescriptionRes String resource ID for the contextual step info bottom sheet body.
 * @property infoItems Optional structured option items displayed below the intro description.
 * @property isOptional When `true` the step can be skipped via "Skip to Review".
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class CreateEditGroupStep(
    @StringRes val infoTitleRes: Int,
    @StringRes val infoDescriptionRes: Int,
    val infoItems: ImmutableList<WizardStepInfoItem> = persistentListOf(),
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Group name + optional description. */
    INFO(
        infoTitleRes = R.string.group_step_info_info_title,
        infoDescriptionRes = R.string.group_step_info_info_description
    ),

    /** Primary currency selection + optional extra currencies. */
    CURRENCY(
        infoTitleRes = R.string.group_step_info_currency_title,
        infoDescriptionRes = R.string.group_step_info_currency_description
    ),

    /** Invite members by email (optional, only in create mode). */
    MEMBERS(
        infoTitleRes = R.string.group_step_info_members_title,
        infoDescriptionRes = R.string.group_step_info_members_description
    ),

    /** Optional display name assignment step for unregistered invitees (only in create mode). */
    UNREGISTERED_NAMES(
        infoTitleRes = R.string.group_step_info_unregistered_names_title,
        infoDescriptionRes = R.string.group_step_info_unregistered_names_description,
        isOptional = true
    ),

    /** Optional group cover image selection step. */
    IMAGE(
        infoTitleRes = R.string.group_step_info_image_title,
        infoDescriptionRes = R.string.group_step_info_image_description,
        isOptional = true
    ),

    /** Read-only summary — final confirmation before saving. */
    REVIEW(
        infoTitleRes = R.string.group_step_info_review_title,
        infoDescriptionRes = R.string.group_step_info_review_description,
        isReview = true
    )
}
