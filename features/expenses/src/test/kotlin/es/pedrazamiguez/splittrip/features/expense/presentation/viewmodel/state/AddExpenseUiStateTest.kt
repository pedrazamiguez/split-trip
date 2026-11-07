package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AddExpenseUiState")
class AddExpenseUiStateTest {

    // ── isReady ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isReady")
    inner class IsReady {

        @Test
        fun `returns true when config is loaded and not failed and not loading`() {
            val state = AddExpenseUiState(
                isConfigLoaded = true,
                configLoadFailed = false,
                isLoading = false
            )
            assertTrue(state.isReady)
        }

        @Test
        fun `returns false when config is not loaded`() {
            val state = AddExpenseUiState(isConfigLoaded = false)
            assertFalse(state.isReady)
        }

        @Test
        fun `returns false when config load failed`() {
            val state = AddExpenseUiState(isConfigLoaded = true, configLoadFailed = true)
            assertFalse(state.isReady)
        }

        @Test
        fun `returns false when still loading`() {
            val state = AddExpenseUiState(isConfigLoaded = true, isLoading = true)
            assertFalse(state.isReady)
        }
    }

    // ── isFormValid ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isFormValid")
    inner class IsFormValid {

        @Test
        fun `returns true when all fields are valid`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00"
            )
            assertTrue(state.isFormValid)
        }

        @Test
        fun `returns false when title is invalid`() {
            val state = AddExpenseUiState(
                isTitleValid = false,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when amount is invalid`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = false,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when due date is invalid`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = false,
                expenseTitle = "Dinner",
                sourceAmount = "50.00"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when title is blank`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "",
                sourceAmount = "50.00"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when amount is blank`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = ""
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when add-on has invalid amount`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00",
                addOns = persistentListOf(AddOnUiModel(id = "addon-1", isAmountValid = false))
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when showExchangeRateSection is true and rate is non-positive`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00",
                showExchangeRateSection = true,
                displayExchangeRate = "0",
                calculatedGroupAmount = "50.00"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when showExchangeRateSection is true and group amount is non-positive`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00",
                showExchangeRateSection = true,
                displayExchangeRate = "1.5",
                calculatedGroupAmount = "-1.0"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when add-on exchange rate or group amount is non-positive`() {
            val state = AddExpenseUiState(
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00",
                addOns = persistentListOf(AddOnUiModel(id = "addon-1", isAmountValid = false))
            )
            assertFalse(state.isFormValid)
        }
    }

    // ── showContributionScopeStep ────────────────────────────────────────────

    @Nested
    @DisplayName("showContributionScopeStep")
    inner class ShowContributionScopeStep {

        @Test
        fun `returns true when funding source is USER`() {
            val state = AddExpenseUiState(
                selectedFundingSource = FundingSourceUiModel(id = PayerType.USER.name, displayText = "My Money")
            )
            assertTrue(state.showContributionScopeStep)
        }

        @Test
        fun `returns false when funding source is GROUP`() {
            val state = AddExpenseUiState(
                selectedFundingSource = FundingSourceUiModel(id = PayerType.GROUP.name, displayText = "Group Pocket")
            )
            assertFalse(state.showContributionScopeStep)
        }

        @Test
        fun `returns false when no funding source selected`() {
            val state = AddExpenseUiState(selectedFundingSource = null)
            assertFalse(state.showContributionScopeStep)
        }
    }

    // ── applicableSteps ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("applicableSteps")
    inner class ApplicableSteps {

        @Test
        fun `excludes EXCHANGE_RATE and SPLIT when not applicable`() {
            val state = AddExpenseUiState(
                showExchangeRateSection = false,
                memberIds = persistentListOf("user1")
            )
            assertFalse(state.applicableSteps.contains(AddExpenseStep.EXCHANGE_RATE))
            assertFalse(state.applicableSteps.contains(AddExpenseStep.SPLIT))
        }

        @Test
        fun `includes EXCHANGE_RATE when foreign currency selected`() {
            val state = AddExpenseUiState(showExchangeRateSection = true)
            assertTrue(state.applicableSteps.contains(AddExpenseStep.EXCHANGE_RATE))
        }

        @Test
        fun `includes SPLIT when more than one member`() {
            val state = AddExpenseUiState(
                memberIds = persistentListOf("user1", "user2")
            )
            assertTrue(state.applicableSteps.contains(AddExpenseStep.SPLIT))
        }

        @Test
        fun `always includes FUNDING_SOURCE`() {
            val state = AddExpenseUiState()
            assertTrue(state.applicableSteps.contains(AddExpenseStep.FUNDING_SOURCE))
        }

        @Test
        fun `includes CONTRIBUTION_SCOPE when funding source is USER`() {
            val state = AddExpenseUiState(
                selectedFundingSource = FundingSourceUiModel(
                    id = PayerType.USER.name,
                    displayText = "My Money"
                )
            )
            assertTrue(state.applicableSteps.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `excludes CONTRIBUTION_SCOPE when funding source is GROUP`() {
            val state = AddExpenseUiState(
                selectedFundingSource = FundingSourceUiModel(
                    id = PayerType.GROUP.name,
                    displayText = "Group Pocket"
                )
            )
            assertFalse(state.applicableSteps.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `excludes CONTRIBUTION_SCOPE when no funding source selected`() {
            val state = AddExpenseUiState(selectedFundingSource = null)
            assertFalse(state.applicableSteps.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `CONTRIBUTION_SCOPE appears between FUNDING_SOURCE and VENDOR_NOTES`() {
            val state = AddExpenseUiState(
                selectedFundingSource = FundingSourceUiModel(
                    id = PayerType.USER.name,
                    displayText = "My Money"
                )
            )
            val steps = state.applicableSteps
            val fundingIndex = steps.indexOf(AddExpenseStep.FUNDING_SOURCE)
            val scopeIndex = steps.indexOf(AddExpenseStep.CONTRIBUTION_SCOPE)
            val vendorNotesIndex = steps.indexOf(AddExpenseStep.VENDOR_NOTES)
            assertTrue(fundingIndex < scopeIndex)
            assertTrue(scopeIndex < vendorNotesIndex)
        }

        @Test
        fun `FUNDING_SOURCE appears after CATEGORY and before VENDOR_NOTES`() {
            val state = AddExpenseUiState()
            val steps = state.applicableSteps
            val categoryIndex = steps.indexOf(AddExpenseStep.CATEGORY)
            val fundingIndex = steps.indexOf(AddExpenseStep.FUNDING_SOURCE)
            val vendorNotesIndex = steps.indexOf(AddExpenseStep.VENDOR_NOTES)
            assertTrue(categoryIndex < fundingIndex)
            assertTrue(fundingIndex < vendorNotesIndex)
        }
    }

    // ── currentStepIndex ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("currentStepIndex")
    inner class CurrentStepIndex {

        @Test
        fun `returns 0 for the first step`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.TITLE)
            assertEquals(0, state.currentStepIndex)
        }

        @Test
        fun `returns correct index for middle step`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.AMOUNT)
            val expectedIndex = state.applicableSteps.indexOf(AddExpenseStep.AMOUNT)
            assertEquals(expectedIndex, state.currentStepIndex)
        }

        @Test
        fun `coerces to 0 when step is not in applicable steps`() {
            // EXCHANGE_RATE is not applicable when showExchangeRateSection = false
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                showExchangeRateSection = false
            )
            assertEquals(0, state.currentStepIndex)
        }
    }

    // ── canGoNext ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canGoNext")
    inner class CanGoNext {

        @Test
        fun `returns true when not on last step`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.TITLE)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `returns false when on the last step (REVIEW)`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.REVIEW)
            assertFalse(state.canGoNext)
        }
    }

    // ── isOnReviewStep ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("isOnReviewStep")
    inner class IsOnReviewStep {

        @Test
        fun `returns true when on REVIEW step`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.REVIEW)
            assertTrue(state.isOnReviewStep)
        }

        @Test
        fun `returns false when not on REVIEW step`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.TITLE)
            assertFalse(state.isOnReviewStep)
        }
    }

    // ── withStepClamped ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("withStepClamped")
    inner class WithStepClamped {

        @Test
        fun `returns same state when current step is in applicable steps`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.TITLE)
            val result = state.withStepClamped()
            assertEquals(AddExpenseStep.TITLE, result.currentStep)
        }

        @Test
        fun `clamps to previous applicable step when current step is removed`() {
            // EXCHANGE_RATE is between AMOUNT and SPLIT/CATEGORY in the new ordering.
            // When it's removed, it should clamp to AMOUNT (previous applicable step).
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                showExchangeRateSection = false
            )
            val result = state.withStepClamped()
            assertEquals(AddExpenseStep.AMOUNT, result.currentStep)
        }

        @Test
        fun `clamps to first step when no previous applicable step exists`() {
            // TITLE is always the first applicable step
            val state = AddExpenseUiState(currentStep = AddExpenseStep.TITLE)
            val result = state.withStepClamped()
            assertEquals(AddExpenseStep.TITLE, result.currentStep)
        }

        @Test
        fun `clamps to FUNDING_SOURCE when CONTRIBUTION_SCOPE step is removed`() {
            // CONTRIBUTION_SCOPE is removed when switching funding source from USER to GROUP
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.CONTRIBUTION_SCOPE,
                selectedFundingSource = FundingSourceUiModel(
                    id = PayerType.GROUP.name,
                    displayText = "Group Pocket"
                )
            )
            val result = state.withStepClamped()
            assertEquals(AddExpenseStep.FUNDING_SOURCE, result.currentStep)
        }
    }

    // ── isOnOptionalStep ────────────────────────────────────────────────────

    @Nested
    @DisplayName("isOnOptionalStep")
    inner class IsOnOptionalStep {

        @Test
        fun `returns true when on an optional step (CATEGORY)`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.CATEGORY)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on VENDOR_NOTES`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.VENDOR_NOTES)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on RECEIPT`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.RECEIPT)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on ADD_ONS`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.ADD_ONS)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on PAYMENT_STATUS`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.PAYMENT_STATUS)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on FUNDING_SOURCE`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.FUNDING_SOURCE)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on EXCHANGE_RATE`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.EXCHANGE_RATE)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns true when on SPLIT`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.SPLIT)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on a required step (TITLE)`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.TITLE)
            assertFalse(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on REVIEW`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.REVIEW)
            assertFalse(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on AMOUNT`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.AMOUNT)
            assertFalse(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on CONTRIBUTION_SCOPE`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.CONTRIBUTION_SCOPE)
            assertFalse(state.isOnOptionalStep)
        }
    }

    // ── optionalStepIndices ───────────────────────────────────────────────────

    @Nested
    @DisplayName("optionalStepIndices")
    inner class OptionalStepIndices {

        @Test
        fun `returns indices of all optional steps`() {
            val state = AddExpenseUiState()
            val steps = state.applicableSteps
            val expectedIndices = steps
                .mapIndexedNotNull { index, step -> if (step.isOptional) index else null }
                .toSet()
            assertEquals(expectedIndices, state.optionalStepIndices)
            assertTrue(expectedIndices.isNotEmpty())
        }

        @Test
        fun `optional indices point to correct steps`() {
            val state = AddExpenseUiState()
            val steps = state.applicableSteps
            state.optionalStepIndices.forEach { index ->
                assertTrue(steps[index].isOptional, "Step at index $index should be optional")
            }
        }

        @Test
        fun `non-optional indices are excluded`() {
            val state = AddExpenseUiState()
            val steps = state.applicableSteps
            steps.forEachIndexed { index, step ->
                if (!step.isOptional) {
                    assertFalse(
                        index in state.optionalStepIndices,
                        "Non-optional step ${step.name} at index $index should not be in optionalStepIndices"
                    )
                }
            }
        }
    }

    // ── isCurrentStepValid ───────────────────────────────────────────────────

    @Nested
    @DisplayName("isCurrentStepValid")
    inner class IsCurrentStepValid {

        @Test
        fun `TITLE step valid when title is not blank and isTitleValid`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.TITLE,
                expenseTitle = "Dinner",
                isTitleValid = true
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `TITLE step invalid when title is blank`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.TITLE,
                expenseTitle = "",
                isTitleValid = true
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `TITLE step invalid when isTitleValid is false`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.TITLE,
                expenseTitle = "Dinner",
                isTitleValid = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `PAYMENT_METHOD step is always valid`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.PAYMENT_METHOD)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `FUNDING_SOURCE step is always valid`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.FUNDING_SOURCE)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CONTRIBUTION_SCOPE step valid when scope is USER`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.CONTRIBUTION_SCOPE,
                contributionScope = PayerType.USER
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CONTRIBUTION_SCOPE step valid when scope is GROUP`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.CONTRIBUTION_SCOPE,
                contributionScope = PayerType.GROUP
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CONTRIBUTION_SCOPE step valid when scope is SUBUNIT with subunit selected`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.CONTRIBUTION_SCOPE,
                contributionScope = PayerType.SUBUNIT,
                selectedContributionSubunitId = "subunit-1"
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CONTRIBUTION_SCOPE step invalid when scope is SUBUNIT without subunit selected`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.CONTRIBUTION_SCOPE,
                contributionScope = PayerType.SUBUNIT,
                selectedContributionSubunitId = null
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `AMOUNT step valid when amount is not blank and valid`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.AMOUNT,
                sourceAmount = "50.00",
                isAmountValid = true
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `AMOUNT step invalid when amount is blank`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.AMOUNT,
                sourceAmount = "",
                isAmountValid = true
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `AMOUNT step invalid when isAmountValid is false`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.AMOUNT,
                sourceAmount = "50.00",
                isAmountValid = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step valid when both fields are strictly positive`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "37.0",
                calculatedGroupAmount = "2.70"
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step invalid when displayExchangeRate is zero`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "0",
                calculatedGroupAmount = "2.70"
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step invalid when displayExchangeRate is negative`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "-37.0",
                calculatedGroupAmount = "2.70"
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step invalid when calculatedGroupAmount is zero`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "37.0",
                calculatedGroupAmount = "0"
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step invalid when calculatedGroupAmount is negative`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "37.0",
                calculatedGroupAmount = "-2.70"
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step invalid when displayExchangeRate is blank`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "",
                calculatedGroupAmount = "2.70"
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `EXCHANGE_RATE step invalid when calculatedGroupAmount is blank`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.EXCHANGE_RATE,
                displayExchangeRate = "37.0",
                calculatedGroupAmount = ""
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `SPLIT step valid when splitError is null`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.SPLIT,
                splitError = null
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `CATEGORY step is always valid`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.CATEGORY)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `VENDOR_NOTES step is always valid`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.VENDOR_NOTES)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `PAYMENT_STATUS step valid when showDueDateSection is false`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.PAYMENT_STATUS,
                showDueDateSection = false,
                isDueDateValid = false // irrelevant when section hidden
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `PAYMENT_STATUS step valid when showDueDateSection is true and due date is valid`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.PAYMENT_STATUS,
                showDueDateSection = true,
                isDueDateValid = true
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `PAYMENT_STATUS step invalid when showDueDateSection is true and due date is invalid`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.PAYMENT_STATUS,
                showDueDateSection = true,
                isDueDateValid = false
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `RECEIPT step is always valid`() {
            val state = AddExpenseUiState(currentStep = AddExpenseStep.RECEIPT)
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `ADD_ONS step valid when all add-ons valid and no error`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.ADD_ONS,
                addOns = persistentListOf(AddOnUiModel(id = "addon-1", isAmountValid = true)),
                addOnError = null
            )
            assertTrue(state.isCurrentStepValid)
        }

        @Test
        fun `ADD_ONS step invalid when add-on amount is invalid`() {
            val state = AddExpenseUiState(
                currentStep = AddExpenseStep.ADD_ONS,
                addOns = persistentListOf(AddOnUiModel(id = "addon-1", isAmountValid = false)),
                addOnError = null
            )
            assertFalse(state.isCurrentStepValid)
        }

        @Test
        fun `REVIEW step delegates to isFormValid`() {
            val validState = AddExpenseUiState(
                currentStep = AddExpenseStep.REVIEW,
                isTitleValid = true,
                isAmountValid = true,
                isDueDateValid = true,
                expenseTitle = "Dinner",
                sourceAmount = "50.00"
            )
            assertTrue(validState.isCurrentStepValid)

            val invalidState = validState.copy(expenseTitle = "")
            assertFalse(invalidState.isCurrentStepValid)
        }
    }

    @Nested
    @DisplayName("AI gating defaults")
    inner class AiGatingDefaults {

        @Test
        fun `defaults isAiGated and isAiProFeature to false`() {
            val state = AddExpenseUiState()
            assertFalse(state.isAiGated)
            assertFalse(state.isAiProFeature)
        }

        @Test
        fun `copies isAiGated and isAiProFeature correctly`() {
            val state = AddExpenseUiState(
                isAiGated = true,
                isAiProFeature = true
            )
            assertTrue(state.isAiGated)
            assertTrue(state.isAiProFeature)
        }
    }
}
