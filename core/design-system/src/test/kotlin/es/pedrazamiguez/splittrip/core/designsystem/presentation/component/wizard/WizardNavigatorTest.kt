package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WizardNavigator")
class WizardNavigatorTest {

    private lateinit var navigator: WizardNavigator

    @BeforeEach
    fun setUp() {
        navigator = WizardNavigator()
    }

    // ── Fake step enum for isolation ─────────────────────────────────────────

    /**
     * Minimal fake [WizardStep] implementation used across all tests.
     * Covers: required steps, an optional step, and a review step.
     */
    private enum class FakeStep(
        override val isOptional: Boolean = false,
        override val isReview: Boolean = false
    ) : WizardStep {
        FIRST,
        SECOND,
        OPTIONAL(isOptional = true),
        REVIEW(isReview = true)
    }

    /** A list without any step marked as [WizardStep.isReview]. */
    private val stepsWithoutReview: List<FakeStep> = listOf(FakeStep.FIRST, FakeStep.SECOND, FakeStep.OPTIONAL)

    private val allSteps: List<FakeStep> = FakeStep.entries

    // ── navigateNext ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("navigateNext()")
    inner class NavigateNext {

        @Test
        @DisplayName("returns the next step when not at the end")
        fun `returns the next step when not at the end`() {
            // When
            val result = navigator.navigateNext(FakeStep.FIRST, allSteps)

            // Then
            assertEquals(FakeStep.SECOND, result)
        }

        @Test
        @DisplayName("returns null when already at the last step")
        fun `returns null when already at the last step`() {
            // When
            val result = navigator.navigateNext(FakeStep.REVIEW, allSteps)

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null (not the first step) when currentStep is not found in applicableSteps")
        fun `returns null when currentStep is not found in applicableSteps`() {
            // Given — REVIEW is not in this subset
            val subset = listOf(FakeStep.FIRST, FakeStep.SECOND)

            // When — indexOf returns -1; guard must prevent getOrNull(0) returning FIRST incorrectly
            val result = navigator.navigateNext(FakeStep.REVIEW, subset)

            // Then
            assertNull(result)
        }
    }

    // ── navigatePrevious ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("navigatePrevious()")
    inner class NavigatePrevious {

        @Test
        @DisplayName("returns Step with the previous step when navigating sequentially")
        fun `returns Step with the previous step sequentially`() {
            // When
            val result = navigator.navigatePrevious(
                currentStep = FakeStep.SECOND,
                jumpedFromStep = null,
                applicableSteps = allSteps
            )

            // Then
            assertEquals(WizardNavigator.NavigationResult.Step(FakeStep.FIRST), result)
        }

        @Test
        @DisplayName("returns ExitWizard when on the first step")
        fun `returns ExitWizard when on the first step`() {
            // When
            val result = navigator.navigatePrevious(
                currentStep = FakeStep.FIRST,
                jumpedFromStep = null,
                applicableSteps = allSteps
            )

            // Then
            assertEquals(WizardNavigator.NavigationResult.ExitWizard, result)
        }

        @Test
        @DisplayName("returns JumpBack when jumpedFromStep is set and current step isReview")
        fun `returns JumpBack when jumpedFromStep is set and currentStep isReview`() {
            // When
            val result = navigator.navigatePrevious(
                currentStep = FakeStep.REVIEW,
                jumpedFromStep = FakeStep.OPTIONAL,
                applicableSteps = allSteps
            )

            // Then
            assertEquals(WizardNavigator.NavigationResult.JumpBack(FakeStep.OPTIONAL), result)
        }

        @Test
        @DisplayName(
            "returns sequential Step (not JumpBack) when jumpedFromStep is set but current step is not the review step"
        )
        fun `returns sequential Step when jumpedFromStep is set but currentStep is not review`() {
            // Given — user jumped from OPTIONAL but is now on a non-review step
            val result = navigator.navigatePrevious(
                currentStep = FakeStep.SECOND,
                jumpedFromStep = FakeStep.OPTIONAL,
                applicableSteps = allSteps
            )

            // Then — sequential back, not a jump-back
            assertEquals(WizardNavigator.NavigationResult.Step(FakeStep.FIRST), result)
        }

        @Test
        @DisplayName("returns ExitWizard when currentStep is not found in applicableSteps")
        fun `returns ExitWizard when currentStep is not found in applicableSteps`() {
            // Given — REVIEW is not in this subset
            val subset = listOf(FakeStep.FIRST, FakeStep.SECOND)

            // When — indexOf returns -1; guard must treat as first step (exit)
            val result = navigator.navigatePrevious(
                currentStep = FakeStep.REVIEW,
                jumpedFromStep = null,
                applicableSteps = subset
            )

            // Then
            assertEquals(WizardNavigator.NavigationResult.ExitWizard, result)
        }
    }

    // ── navigateToReview ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("navigateToReview()")
    inner class NavigateToReview {

        @Test
        @DisplayName("returns the review step when the current step is optional")
        fun `returns the review step when currentStep is optional`() {
            // When
            val result = navigator.navigateToReview(
                currentStep = FakeStep.OPTIONAL,
                applicableSteps = allSteps
            )

            // Then
            assertEquals(FakeStep.REVIEW, result)
        }

        @Test
        @DisplayName("returns null when the current step is not optional")
        fun `returns null when currentStep is not optional`() {
            // When
            val result = navigator.navigateToReview(
                currentStep = FakeStep.FIRST,
                applicableSteps = allSteps
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when no step with isReview true exists in applicableSteps")
        fun `returns null when no review step exists in applicableSteps`() {
            // Given — list that has no REVIEW step
            val result = navigator.navigateToReview(
                currentStep = FakeStep.OPTIONAL,
                applicableSteps = stepsWithoutReview
            )

            // Then
            assertNull(result)
        }
    }

    // ── jumpToStep ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("jumpToStep()")
    inner class JumpToStep {

        @Test
        @DisplayName("returns the step at the given valid index when strictly before currentStep")
        fun `returns the step at the given valid index`() {
            // Given — REVIEW is at index 3; target index 1 (SECOND) is strictly before it
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.REVIEW,
                targetIndex = 1,
                applicableSteps = allSteps
            )

            // Then
            assertEquals(FakeStep.SECOND, result)
        }

        @Test
        @DisplayName("returns the first step when index is 0 and currentStep is index 1")
        fun `returns the first step when index is 0`() {
            // Given — SECOND is at index 1; target index 0 (FIRST) is strictly before it
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.SECOND,
                targetIndex = 0,
                applicableSteps = allSteps
            )

            // Then
            assertEquals(FakeStep.FIRST, result)
        }

        @Test
        @DisplayName("returns null when index is out of bounds (too large)")
        fun `returns null when index is out of bounds`() {
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.REVIEW,
                targetIndex = 999,
                applicableSteps = allSteps
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when index is negative")
        fun `returns null when index is negative`() {
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.REVIEW,
                targetIndex = -1,
                applicableSteps = allSteps
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when applicableSteps is empty")
        fun `returns null when applicableSteps is empty`() {
            // When — currentStep not found in empty list → currentIndex = -1
            val result = navigator.jumpToStep(
                currentStep = FakeStep.FIRST,
                targetIndex = 0,
                applicableSteps = emptyList()
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when targetIndex equals the current step index")
        fun `returns null when targetIndex equals currentStep index`() {
            // Given — SECOND is at index 1; jumping to index 1 (current) is not allowed
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.SECOND,
                targetIndex = 1,
                applicableSteps = allSteps
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when targetIndex is after currentStep and allowForwardJumps is false")
        fun `returns null when targetIndex is after currentStep and allowForwardJumps is false`() {
            // Given — FIRST is at index 0; jumping to index 2 (future) is not allowed by default
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.FIRST,
                targetIndex = 2,
                applicableSteps = allSteps,
                allowForwardJumps = false
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns the future step when targetIndex is after currentStep and allowForwardJumps is true")
        fun `returns the future step when targetIndex is after currentStep and allowForwardJumps is true`() {
            // Given — FIRST is at index 0; jumping to index 2 (OPTIONAL) is allowed when allowForwardJumps is true
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.FIRST,
                targetIndex = 2,
                applicableSteps = allSteps,
                allowForwardJumps = true
            )

            // Then
            assertEquals(FakeStep.OPTIONAL, result)
        }

        @Test
        @DisplayName("returns the previous step when targetIndex is before currentStep and allowForwardJumps is true")
        fun `returns the previous step when targetIndex is before currentStep and allowForwardJumps is true`() {
            // Given — REVIEW is at index 3; jumping to index 1 (SECOND) is allowed when allowForwardJumps is true
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.REVIEW,
                targetIndex = 1,
                applicableSteps = allSteps,
                allowForwardJumps = true
            )

            // Then
            assertEquals(FakeStep.SECOND, result)
        }

        @Test
        @DisplayName("returns null when targetIndex equals currentStep index even if allowForwardJumps is true")
        fun `returns null when targetIndex equals currentStep index even if allowForwardJumps is true`() {
            // Given — SECOND is at index 1; jumping to index 1 (current) is not allowed even when allowForwardJumps is true
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.SECOND,
                targetIndex = 1,
                applicableSteps = allSteps,
                allowForwardJumps = true
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when targetIndex is out of bounds even if allowForwardJumps is true")
        fun `returns null when targetIndex is out of bounds even if allowForwardJumps is true`() {
            // When
            val result = navigator.jumpToStep(
                currentStep = FakeStep.FIRST,
                targetIndex = 999,
                applicableSteps = allSteps,
                allowForwardJumps = true
            )

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("returns null when currentStep is not in applicableSteps even if allowForwardJumps is true")
        fun `returns null when currentStep is not in applicableSteps even if allowForwardJumps is true`() {
            // Given — REVIEW is not in the subset
            val subset = listOf(FakeStep.FIRST, FakeStep.SECOND)

            // When — indexOf returns -1 for REVIEW
            val result = navigator.jumpToStep(
                currentStep = FakeStep.REVIEW,
                targetIndex = 0,
                applicableSteps = subset,
                allowForwardJumps = true
            )

            // Then
            assertNull(result)
        }
    }
}
