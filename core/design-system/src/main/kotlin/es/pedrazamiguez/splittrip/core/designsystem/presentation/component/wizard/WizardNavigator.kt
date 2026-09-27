package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

/**
 * Pure, stateless utility for wizard step navigation.
 *
 * Encapsulates the finite-state machine logic shared across all wizard-enabled
 * ViewModels (`navigateNext`, `navigatePrevious`, `navigateToReview`, `jumpToStep`) so the
 * logic does not need to be copy-pasted per feature.
 *
 * **Usage:** Instantiate as a private `val` inside each ViewModel — no DI needed
 * since this class has no dependencies and no mutable state.
 *
 * ```kotlin
 * private val wizardNavigator = WizardNavigator()
 *
 * private fun navigateNext() {
 *     val next = wizardNavigator.navigateNext(state.currentStep, state.applicableSteps) ?: return
 *     _uiState.update { it.copy(currentStep = next) }
 * }
 * ```
 *
 * **`navigateToReview` integration note:** [navigateToReview] returns the target
 * review step only. The caller is responsible for also recording the departure
 * step in state (`jumpedFromStep = state.currentStep`).
 *
 * **`jumpToStep` integration note:** [jumpToStep] enforces that only a step strictly
 * before the caller's `currentStep` can be targeted — rejecting the current and future
 * steps even if events are dispatched programmatically. The caller is responsible for
 * clearing `jumpedFromStep` in state to prevent stale skip-to-review behaviour after a
 * direct jump-back.
 */
class WizardNavigator {

    /**
     * Returns the step that follows [currentStep] in [applicableSteps],
     * or `null` if [currentStep] is not found in the list or is already the last step.
     */
    fun <S : WizardStep> navigateNext(
        currentStep: S,
        applicableSteps: List<S>
    ): S? {
        val index = applicableSteps.indexOf(currentStep)
        if (index < 0 || index == applicableSteps.lastIndex) return null
        return applicableSteps[index + 1]
    }

    /**
     * Returns the appropriate [NavigationResult] for a back-press on [currentStep].
     *
     * Priority order:
     * 1. If [jumpedFromStep] is non-null **and** [currentStep] is the review step
     *    (`isReview == true`) → [NavigationResult.JumpBack] to the departure step.
     * 2. If a step before [currentStep] exists in [applicableSteps] →
     *    [NavigationResult.Step] to that step.
     * 3. If [currentStep] is already the first step → [NavigationResult.ExitWizard].
     *
     * **ViewModel integration note:** Both [NavigationResult.Step] and
     * [NavigationResult.JumpBack] result in the same state update — navigate to
     * the returned step and clear `jumpedFromStep`. The two subtypes exist for
     * semantic clarity and isolated unit-testability of this class.
     */
    fun <S : WizardStep> navigatePrevious(
        currentStep: S,
        jumpedFromStep: S?,
        applicableSteps: List<S>
    ): NavigationResult<S> {
        if (jumpedFromStep != null && currentStep.isReview) {
            return NavigationResult.JumpBack(jumpedFromStep)
        }
        val index = applicableSteps.indexOf(currentStep)
        if (index < 0) return NavigationResult.ExitWizard
        val prevStep = applicableSteps.getOrNull(index - 1)
        return if (prevStep != null) NavigationResult.Step(prevStep) else NavigationResult.ExitWizard
    }

    /**
     * Returns the review step from [applicableSteps] when [currentStep] is optional
     * (i.e. the user may skip ahead to review), or `null` when the jump is not allowed.
     *
     * Returns `null` when:
     * - [currentStep] is not optional (`isOptional == false`), or
     * - no step with `isReview == true` exists in [applicableSteps].
     *
     * **ViewModel integration note:** When a non-null step is returned the caller
     * must also record the departure step in state:
     * `_uiState.update { it.copy(currentStep = reviewStep, jumpedFromStep = state.currentStep) }`
     */
    fun <S : WizardStep> navigateToReview(
        currentStep: S,
        applicableSteps: List<S>
    ): S? {
        if (!currentStep.isOptional) return null
        return applicableSteps.firstOrNull { it.isReview }
    }

    /**
     * Returns the step at `targetIndex` in [applicableSteps], or `null` when:
     * - [currentStep] is not present in [applicableSteps],
     * - `targetIndex` is out of bounds (negative or ≥ list size),
     * - `targetIndex` equals the index of [currentStep], or
     * - `targetIndex` is strictly after the index of [currentStep] and [allowForwardJumps] is `false`.
     *
     * Intended for tapping a step circle in `WizardStepIndicator` to jump directly
     * to that step. In creation flows ([allowForwardJumps] = false, the default), jumping
     * to future steps is rejected centrally to preserve wizard progression invariants.
     * In edit flows ([allowForwardJumps] = true), arbitrary navigation across all valid steps
     * is permitted because existing entity data is already fully populated and valid.
     *
     * **ViewModel integration note:** When a non-null step is returned the caller
     * must clear any recorded departure step in state so that Back navigation is
     * not misrouted after the jump.
     */
    fun <S : WizardStep> jumpToStep(
        currentStep: S,
        targetIndex: Int,
        applicableSteps: List<S>,
        allowForwardJumps: Boolean = false
    ): S? {
        val currentIndex = applicableSteps.indexOf(currentStep)
        if (currentIndex < 0) return null
        if (targetIndex == currentIndex) return null
        if (!allowForwardJumps && targetIndex > currentIndex) return null
        return applicableSteps.getOrNull(targetIndex)
    }

    /** Typed outcome of a [navigatePrevious] call. */
    sealed interface NavigationResult<out S> {

        /**
         * Common supertype for results that carry a target step.
         *
         * Collapsing [Step] and [JumpBack] into a single `when` branch via this
         * interface avoids duplicate identical branch bodies in ViewModels, while
         * preserving the concrete subtypes for isolated unit-testing of navigator logic.
         */
        sealed interface WithStep<out S> : NavigationResult<S> {
            val step: S
        }

        /** Navigate sequentially to the previous [step]. */
        data class Step<S>(override val step: S) : WithStep<S>

        /** Return to the [step] the user jumped from after a "Skip to Review" action. */
        data class JumpBack<S>(override val step: S) : WithStep<S>

        /** No previous step exists — the wizard should be dismissed (back-stack pop). */
        data object ExitWizard : NavigationResult<Nothing>
    }
}
