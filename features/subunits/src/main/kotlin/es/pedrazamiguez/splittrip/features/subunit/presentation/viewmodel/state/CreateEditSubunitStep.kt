package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStep

/**
 * Defines the wizard steps for the create/edit subunit flow.
 *
 * All four steps are always applicable (no conditional steps).
 * All steps are required — there are no optional steps in this wizard.
 *
 * NAME → MEMBERS → SHARES → REVIEW
 *
 * @property isOptional When `true` the step can be skipped via "Skip to Review".
 *                      Currently all subunit steps are required.
 * @property isReview When `true` this is the final read-only review/confirmation step.
 */
enum class CreateEditSubunitStep(
    override val isOptional: Boolean = false,
    override val isReview: Boolean = false
) : WizardStep {
    /** Subunit name input. */
    NAME,

    /** Select members from the group. */
    MEMBERS,

    /** Allocate percentage shares for selected members. */
    SHARES,

    /** Read-only summary — final confirmation before saving. */
    REVIEW(isReview = true)
}
