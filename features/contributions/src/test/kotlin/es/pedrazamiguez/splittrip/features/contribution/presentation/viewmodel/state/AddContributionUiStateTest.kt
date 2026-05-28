package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddContributionUiStateTest {

    // ── steps / currentStepIndex ──────────────────────────────────────────

    @Nested
    inner class StepsAndIndex {

        @Test
        fun `steps contains all AddContributionStep entries`() {
            val state = AddContributionUiState()
            assertEquals(AddContributionStep.entries, state.steps)
        }

        @Test
        fun `currentStepIndex returns 0 for AMOUNT (default)`() {
            val state = AddContributionUiState()
            assertEquals(0, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns 1 for SCOPE`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.SCOPE)
            assertEquals(1, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns 2 for REVIEW`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.REVIEW)
            assertEquals(2, state.currentStepIndex)
        }
    }

    // ── canGoNext ─────────────────────────────────────────────────────────

    @Nested
    inner class CanGoNext {

        @Test
        fun `canGoNext is true on AMOUNT step`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.AMOUNT)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is true on SCOPE step`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.SCOPE)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is false on REVIEW step (last)`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.REVIEW)
            assertFalse(state.canGoNext)
        }
    }

    // ── isOnReviewStep ────────────────────────────────────────────────────

    @Nested
    inner class IsOnReviewStep {

        @Test
        fun `isOnReviewStep is true when on REVIEW`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.REVIEW)
            assertTrue(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on AMOUNT`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.AMOUNT)
            assertFalse(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on SCOPE`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.SCOPE)
            assertFalse(state.isOnReviewStep)
        }
    }

    // ── isCurrentStepValid ────────────────────────────────────────────────

    @Nested
    inner class IsCurrentStepValid {

        @Test
        fun `AMOUNT step is valid when amountInput is not blank and no error`() {
            val state = AddContributionUiState(
                currentStep = AddContributionStep.AMOUNT,
                amountInput = "50.00",
                amountError = false
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `AMOUNT step is invalid when amountInput is blank`() {
            val state = AddContributionUiState(
                currentStep = AddContributionStep.AMOUNT,
                amountInput = "",
                amountError = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `AMOUNT step is invalid when amountError is true`() {
            val state = AddContributionUiState(
                currentStep = AddContributionStep.AMOUNT,
                amountInput = "abc",
                amountError = true
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `SCOPE step is always valid (has a default selection)`() {
            val state = AddContributionUiState(currentStep = AddContributionStep.SCOPE)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is valid when amountInput is not blank and no error`() {
            val state = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "100.00",
                amountError = false
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when amountInput is blank`() {
            val state = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "",
                amountError = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when amountError is true`() {
            val state = AddContributionUiState(
                currentStep = AddContributionStep.REVIEW,
                amountInput = "-5",
                amountError = true
            )
            assertFalse(state.isCurrentStepValid)
        }
    }
}
