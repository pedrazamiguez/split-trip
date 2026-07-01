package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateEditGroupUiStateTest {

    private val currencyUiModel = CurrencyUiModel("EUR", "EUR - €", 2, "Euro")

    @Nested
    inner class Steps {

        @Test
        fun `steps in create mode without pending users do not contain unregistered names step`() {
            val state = CreateEditGroupUiState(
                isEditMode = false,
                selectedMembers = listOf(User(userId = "1", email = "test@test.com", isPending = false))
                    .toImmutableList()
            )
            assertFalse(state.steps.contains(CreateEditGroupStep.UNREGISTERED_NAMES))
            assertTrue(state.steps.contains(CreateEditGroupStep.MEMBERS))
        }

        @Test
        fun `steps in create mode with pending users contain unregistered names step`() {
            val state = CreateEditGroupUiState(
                isEditMode = false,
                selectedMembers = listOf(User(userId = "1", email = "test@test.com", isPending = true))
                    .toImmutableList()
            )
            assertTrue(state.steps.contains(CreateEditGroupStep.UNREGISTERED_NAMES))
            assertTrue(state.steps.contains(CreateEditGroupStep.MEMBERS))
        }

        @Test
        fun `steps in edit mode include members but exclude unregistered names`() {
            val state = CreateEditGroupUiState(isEditMode = true)
            assertTrue(state.steps.contains(CreateEditGroupStep.MEMBERS))
            assertFalse(state.steps.contains(CreateEditGroupStep.UNREGISTERED_NAMES))
            assertTrue(state.steps.contains(CreateEditGroupStep.INFO))
            assertTrue(state.steps.contains(CreateEditGroupStep.REVIEW))
        }

        @Test
        fun `create mode without pending members has 5 steps`() {
            val state = CreateEditGroupUiState(isEditMode = false)
            assertEquals(5, state.steps.size)
            assertFalse(state.steps.contains(CreateEditGroupStep.UNREGISTERED_NAMES))
        }

        @Test
        fun `create mode with pending members has all 6 steps`() {
            val state = CreateEditGroupUiState(
                isEditMode = false,
                selectedMembers = listOf(User(userId = "p1", email = "p@test.com", isPending = true))
                    .toImmutableList()
            )
            assertEquals(6, state.steps.size)
            assertEquals(CreateEditGroupStep.entries.size, state.steps.size)
        }

        @Test
        fun `edit mode without pending has exactly 5 steps`() {
            val state = CreateEditGroupUiState(isEditMode = true)
            assertEquals(5, state.steps.size)
        }
    }

    @Nested
    inner class StepIndex {

        @Test
        fun `currentStepIndex returns correct index for current step`() {
            val state = CreateEditGroupUiState(
                isEditMode = false,
                currentStep = CreateEditGroupStep.CURRENCY
            )
            assertEquals(1, state.currentStepIndex)
        }

        @Test
        fun `currentStepIndex coerces to 0 when step not in steps list`() {
            val state = CreateEditGroupUiState(
                isEditMode = true,
                // UNREGISTERED_NAMES is not in edit-mode steps without pending users
                currentStep = CreateEditGroupStep.UNREGISTERED_NAMES
            )
            assertEquals(0, state.currentStepIndex)
        }
    }

    @Nested
    inner class CanGoNext {

        @Test
        fun `canGoNext is true when not on last step`() {
            val state = CreateEditGroupUiState(
                isEditMode = false,
                currentStep = CreateEditGroupStep.INFO
            )
            assertTrue(state.canGoNext)
        }

        @Test
        fun `canGoNext is false when on last step`() {
            val state = CreateEditGroupUiState(
                isEditMode = false,
                currentStep = CreateEditGroupStep.REVIEW
            )
            assertFalse(state.canGoNext)
        }
    }

    @Nested
    inner class IsOnReviewStep {

        @Test
        fun `isOnReviewStep is true when on REVIEW step`() {
            val state = CreateEditGroupUiState(currentStep = CreateEditGroupStep.REVIEW)
            assertTrue(state.isOnReviewStep)
        }

        @Test
        fun `isOnReviewStep is false when not on REVIEW step`() {
            val state = CreateEditGroupUiState(currentStep = CreateEditGroupStep.INFO)
            assertFalse(state.isOnReviewStep)
        }
    }

    @Nested
    inner class IsCurrentStepValid {

        @Test
        fun `INFO step valid when name is not blank and isNameValid is true`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.INFO,
                groupName = "My Trip",
                isNameValid = true
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `INFO step invalid when name is blank`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.INFO,
                groupName = "",
                isNameValid = true
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `INFO step invalid when isNameValid is false`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.INFO,
                groupName = "My Trip",
                isNameValid = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `CURRENCY step valid when currency is selected`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.CURRENCY,
                selectedCurrency = currencyUiModel
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CURRENCY step invalid when no currency selected`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.CURRENCY,
                selectedCurrency = null
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `MEMBERS step is always valid`() {
            val state = CreateEditGroupUiState(currentStep = CreateEditGroupStep.MEMBERS)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `UNREGISTERED_NAMES step is always valid`() {
            val state = CreateEditGroupUiState(currentStep = CreateEditGroupStep.UNREGISTERED_NAMES)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `IMAGE step is always valid`() {
            val state = CreateEditGroupUiState(currentStep = CreateEditGroupStep.IMAGE)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step valid when name and currency are set`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.REVIEW,
                groupName = "My Trip",
                isNameValid = true,
                selectedCurrency = currencyUiModel,
                availableCurrencies = persistentListOf(currencyUiModel)
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step invalid when no currency selected`() {
            val state = CreateEditGroupUiState(
                currentStep = CreateEditGroupStep.REVIEW,
                groupName = "My Trip",
                isNameValid = true,
                selectedCurrency = null
            )
            assertFalse(state.isCurrentStepValid)
        }
    }
}
