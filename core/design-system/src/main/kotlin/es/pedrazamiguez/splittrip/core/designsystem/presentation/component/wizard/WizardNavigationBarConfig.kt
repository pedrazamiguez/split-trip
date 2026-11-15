package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

/**
 * Bundles the state and labels for [WizardNavigationBar], keeping the composable
 * parameter count within the detekt `LongParameterList` threshold.
 *
 * @param canGoNext            Whether the Next button should be enabled.
 * @param isOnLastStep         When `true` the forward button shows [submitLabel].
 * @param isCurrentStepValid   Whether the current step's inputs pass validation.
 * @param isLoading            Shows a spinner on the submit button while in progress.
 * @param backLabel            Localised label for the back button.
 * @param nextLabel            Localised label for the next button.
 * @param submitLabel          Localised label for the submit button.
 * @param skipToReviewLabel    Optional localised label for the fast-track "Skip to Review"
 *                             pill rendered above the Back/Next row when non-null alongside
 *                             `onSkipToReview` and `!isOnLastStep`.
 * @param optionalDefaultsHint Optional supporting caption rendered inside the fast-track
 *                             pill explaining pre-applied smart defaults on skipped steps.
 */
data class WizardNavigationBarConfig(
    val canGoNext: Boolean,
    val isOnLastStep: Boolean,
    val isCurrentStepValid: Boolean,
    val isLoading: Boolean,
    val backLabel: String,
    val nextLabel: String,
    val submitLabel: String,
    val skipToReviewLabel: String? = null,
    val optionalDefaultsHint: String? = null
)
