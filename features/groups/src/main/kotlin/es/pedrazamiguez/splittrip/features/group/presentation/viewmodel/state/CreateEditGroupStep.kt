package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep

/**
 * Defines the wizard steps for the create/edit group flow.
 *
 * @property isOptional When `true` the step can be skipped via "Skip to Review".
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class CreateEditGroupStep(
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Group name + optional description. */
    INFO,

    /** Primary currency selection + optional extra currencies. */
    CURRENCY,

    /** Invite members by email (optional, only in create mode). */
    MEMBERS,

    /** Optional display name assignment step for unregistered invitees (only in create mode). */
    UNREGISTERED_NAMES(isOptional = true),

    /** Optional group cover image selection step. */
    IMAGE(isOptional = true),

    /** Read-only summary — final confirmation before saving. */
    REVIEW(isReview = true)
}
