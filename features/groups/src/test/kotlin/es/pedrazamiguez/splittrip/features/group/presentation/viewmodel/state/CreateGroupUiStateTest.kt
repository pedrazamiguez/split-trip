package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateGroupUiStateTest {

    private val eurCurrency = CurrencyUiModel(
        code = "EUR",
        displayText = "EUR (€)",
        decimalDigits = 2
    )

    // ── steps / currentStepIndex ──────────────────────────────────────────

    @Nested
    inner class StepsAndIndex {

        @Test
        fun `steps contains all CreateGroupStep entries when there is at least one unregistered member`() {
            val state = CreateGroupUiState(
                selectedMembers = kotlinx.collections.immutable.persistentListOf(
                    User(userId = "1", email = "test@example.com", isPending = true)
                )
            )
            assertEquals(CreateGroupStep.entries, state.steps)
        }

        @Test
        fun `steps filters out UNREGISTERED_NAMES when there are no unregistered members`() {
            val state = CreateGroupUiState(
                selectedMembers = kotlinx.collections.immutable.persistentListOf(
                    User(userId = "1", email = "test@example.com", isPending = false)
                )
            )
            val expected = CreateGroupStep.entries.filter { it != CreateGroupStep.UNREGISTERED_NAMES }
            assertEquals(expected, state.steps)
        }

        @Test
        fun `currentStepIndex returns 0 for INFO (default)`() {
            val state = CreateGroupUiState()
            assertEquals(0, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for CURRENCY`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.CURRENCY)
            assertEquals(1, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for MEMBERS`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.MEMBERS)
            assertEquals(2, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for UNREGISTERED_NAMES when pending member exists`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.UNREGISTERED_NAMES,
                selectedMembers = kotlinx.collections.immutable.persistentListOf(
                    User(userId = "1", email = "test@example.com", isPending = true)
                )
            )
            assertEquals(3, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for IMAGE`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.IMAGE)
            assertEquals(3, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for IMAGE when pending member exists`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.IMAGE,
                selectedMembers = kotlinx.collections.immutable.persistentListOf(
                    User(userId = "1", email = "test@example.com", isPending = true)
                )
            )
            assertEquals(4, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for REVIEW`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.REVIEW)
            assertEquals(4, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex returns correct index for REVIEW when pending member exists`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.REVIEW,
                selectedMembers = kotlinx.collections.immutable.persistentListOf(
                    User(userId = "1", email = "test@example.com", isPending = true)
                )
            )
            assertEquals(5, state.currentStepIndex)
        }
    }

    // ── canGoNext ─────────────────────────────────────────────────────────

    @Nested
    inner class CanGoNext {

        @Test
        fun `canGoNext is true on INFO step`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.INFO)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is true on CURRENCY step`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.CURRENCY)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is true on MEMBERS step`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.MEMBERS)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is true on IMAGE step`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.IMAGE)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is false on REVIEW step (last)`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.REVIEW)
            assertFalse(state.canGoNext)
        }
    }

    // ── isOnReviewStep ────────────────────────────────────────────────────

    @Nested
    inner class IsOnReviewStep {

        @Test
        fun `isOnReviewStep is true when on REVIEW`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.REVIEW)
            assertTrue(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on INFO`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.INFO)
            assertFalse(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on CURRENCY`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.CURRENCY)
            assertFalse(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when on IMAGE`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.IMAGE)
            assertFalse(state.isOnReviewStep)
        }
    }

    // ── isCurrentStepValid ────────────────────────────────────────────────

    @Nested
    inner class IsCurrentStepValid {

        @Test
        fun `INFO step is valid when groupName is not blank and isNameValid`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.INFO,
                groupName = "My Trip",
                isNameValid = true
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `INFO step is invalid when groupName is blank`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.INFO,
                groupName = "",
                isNameValid = true
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `INFO step is invalid when isNameValid is false`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.INFO,
                groupName = "Taken Name",
                isNameValid = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `CURRENCY step is valid when currency is selected`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.CURRENCY,
                selectedCurrency = eurCurrency
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CURRENCY step is invalid when no currency selected`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.CURRENCY,
                selectedCurrency = null
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `MEMBERS step is always valid`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.MEMBERS)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `UNREGISTERED_NAMES step is always valid`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.UNREGISTERED_NAMES)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `IMAGE step is always valid`() {
            val state = CreateGroupUiState(currentStep = CreateGroupStep.IMAGE)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is valid when name is not blank, isNameValid, and currency selected`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.REVIEW,
                groupName = "Berlin Trip",
                isNameValid = true,
                selectedCurrency = eurCurrency
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when groupName is blank`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.REVIEW,
                groupName = "",
                isNameValid = true,
                selectedCurrency = eurCurrency
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when no currency selected`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.REVIEW,
                groupName = "My Trip",
                isNameValid = true,
                selectedCurrency = null
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step is invalid when isNameValid is false`() {
            val state = CreateGroupUiState(
                currentStep = CreateGroupStep.REVIEW,
                groupName = "Taken",
                isNameValid = false,
                selectedCurrency = eurCurrency
            )
            assertFalse(state.isCurrentStepValid)
        }
    }
}
