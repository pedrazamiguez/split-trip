package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state

import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateEditSubunitUiStateTest {

    // ── steps / currentStepIndex ──────────────────────────────────────────

    @Nested
    inner class StepsAndIndex {

        @Test
        fun `steps contains all CreateEditSubunitStep entries`() {
            val state = CreateEditSubunitUiState()
            assertEquals(CreateEditSubunitStep.entries, state.steps)
        }

        @Test
        fun `currentStepIndex returns 0 for NAME (default)`() {
            val state = CreateEditSubunitUiState()
            assertEquals(0, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns 1 for MEMBERS`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.MEMBERS)
            assertEquals(1, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns 2 for SHARES`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.SHARES)
            assertEquals(2, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns 3 for REVIEW`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.REVIEW)
            assertEquals(3, state.currentStepIndex)
        }
    }

    // ── canGoNext ─────────────────────────────────────────────────────────

    @Nested
    inner class CanGoNext {

        @Test
        fun `canGoNext is true on NAME step`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.NAME)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is true on MEMBERS step`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.MEMBERS)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is true on SHARES step`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.SHARES)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is false on REVIEW step (last)`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.REVIEW)
            assertFalse(state.canGoNext)
        }
    }

    // ── isOnReviewStep ────────────────────────────────────────────────────

    @Nested
    inner class IsOnReviewStep {

        @Test
        fun `isOnReviewStep is true when on REVIEW`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.REVIEW)
            assertTrue(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on NAME`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.NAME)
            assertFalse(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on MEMBERS`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.MEMBERS)
            assertFalse(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on SHARES`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.SHARES)
            assertFalse(state.isOnReviewStep)
        }
    }

    // ── isCurrentStepValid ────────────────────────────────────────────────

    @Nested
    inner class IsCurrentStepValid {

        @Test
        fun `NAME step is valid when name is not blank`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.NAME,
                name = "Couple"
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `NAME step is invalid when name is blank`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.NAME,
                name = ""
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `NAME step is invalid when name is whitespace only`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.NAME,
                name = "   "
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `MEMBERS step is valid when at least one member is selected`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.MEMBERS,
                selectedMemberIds = persistentListOf("user-1")
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `MEMBERS step is invalid when no members selected`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.MEMBERS,
                selectedMemberIds = persistentListOf()
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `SHARES step is always valid`() {
            val state = CreateEditSubunitUiState(currentStep = CreateEditSubunitStep.SHARES)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is valid when name is not blank and members are selected`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.REVIEW,
                name = "Couple",
                selectedMemberIds = persistentListOf("user-1", "user-2")
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when name is blank`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.REVIEW,
                name = "",
                selectedMemberIds = persistentListOf("user-1")
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when no members selected`() {
            val state = CreateEditSubunitUiState(
                currentStep = CreateEditSubunitStep.REVIEW,
                name = "Couple",
                selectedMemberIds = persistentListOf()
            )
            assertFalse(state.isCurrentStepValid)
        }
    }
}
