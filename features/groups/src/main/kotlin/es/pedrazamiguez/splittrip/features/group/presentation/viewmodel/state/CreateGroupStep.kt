package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep

/**
 * Defines the wizard steps for the create-group flow.
 *
 * All four steps are always applicable (no conditional steps).
 *
 * INFO → CURRENCY → MEMBERS → REVIEW
 *
 * @property isOptional When `true` the step can be skipped via "Skip to Review".
 *                      Currently all group creation steps are required.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class CreateGroupStep(
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Group name + optional description. */
    INFO,

    /** Primary currency selection + optional extra currencies. */
    CURRENCY,

    /** Invite members by email (optional). */
    MEMBERS,

    /** Optional display name assignment step for unregistered invitees. */
    UNREGISTERED_NAMES(isOptional = true),

    /** Optional group cover image selection step. */
    IMAGE(isOptional = true),

    /** Read-only summary — final confirmation before creation. */
    REVIEW(isReview = true)
}
