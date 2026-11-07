package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddCashWithdrawalUiStateTest {

    // ── isReady ────────────────────────────────────────────────────────────────

    @Nested
    inner class IsReady {

        @Test
        fun `returns false when config is not loaded`() {
            val state = AddCashWithdrawalUiState(isConfigLoaded = false)
            assertFalse(state.isReady)
        }

        @Test
        fun `returns false when config load failed`() {
            val state = AddCashWithdrawalUiState(isConfigLoaded = true, configLoadFailed = true)
            assertFalse(state.isReady)
        }

        @Test
        fun `returns false when loading`() {
            val state = AddCashWithdrawalUiState(isConfigLoaded = true, isLoading = true)
            assertFalse(state.isReady)
        }

        @Test
        fun `returns true when config loaded with no errors and not loading`() {
            val state = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                configLoadFailed = false,
                isLoading = false
            )
            assertTrue(state.isReady)
        }
    }

    // ── isFormValid ─────────────────────────────────────────────────────────────

    @Nested
    inner class IsFormValid {

        @Test
        fun `returns false when withdrawal amount is blank`() {
            val state = AddCashWithdrawalUiState(withdrawalAmount = "", isAmountValid = true)
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when amount is present but isAmountValid is false`() {
            val state = AddCashWithdrawalUiState(withdrawalAmount = "100", isAmountValid = false)
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns true when same currency (no exchange rate section)`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = false
            )
            assertTrue(state.isFormValid)
        }

        @Test
        fun `returns false when foreign currency but deducted amount is blank`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = true,
                deductedAmount = ""
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns true when foreign currency and deducted amount is filled`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = true,
                deductedAmount = "95"
            )
            assertTrue(state.isFormValid)
        }

        @Test
        fun `returns false when foreign currency and exchange rate is non-positive`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = true,
                deductedAmount = "95",
                displayExchangeRate = "0"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when foreign currency and deducted amount is non-positive`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = true,
                deductedAmount = "-10",
                displayExchangeRate = "1.05"
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when foreign currency and isExchangeRateValid is false`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = true,
                deductedAmount = "95",
                displayExchangeRate = "1.05",
                isExchangeRateValid = false
            )
            assertFalse(state.isFormValid)
        }

        @Test
        fun `returns false when foreign currency and isDeductedAmountValid is false`() {
            val state = AddCashWithdrawalUiState(
                withdrawalAmount = "100",
                isAmountValid = true,
                showExchangeRateSection = true,
                deductedAmount = "95",
                displayExchangeRate = "1.05",
                isDeductedAmountValid = false
            )
            assertFalse(state.isFormValid)
        }
    }

    // ── CashWithdrawalStep.applicableSteps ─────────────────────────────────────

    @Nested
    inner class ApplicableSteps {

        @Test
        fun `minimal flow - no exchange rate, no fee`() {
            val steps = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = false,
                hasFee = false,
                showFeeExchangeRateSection = false
            )
            assertEquals(
                listOf(
                    CashWithdrawalStep.AMOUNT,
                    CashWithdrawalStep.SCOPE,
                    CashWithdrawalStep.DETAILS,
                    CashWithdrawalStep.REVIEW
                ),
                steps
            )
        }

        @Test
        fun `foreign currency - exchange rate added, no fee`() {
            val steps = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = true,
                hasFee = false,
                showFeeExchangeRateSection = false
            )
            assertEquals(
                listOf(
                    CashWithdrawalStep.AMOUNT,
                    CashWithdrawalStep.EXCHANGE_RATE,
                    CashWithdrawalStep.SCOPE,
                    CashWithdrawalStep.DETAILS,
                    CashWithdrawalStep.REVIEW
                ),
                steps
            )
        }

        @Test
        fun `same currency with fee in same currency`() {
            val steps = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = false,
                hasFee = true,
                showFeeExchangeRateSection = false
            )
            assertEquals(
                listOf(
                    CashWithdrawalStep.AMOUNT,
                    CashWithdrawalStep.SCOPE,
                    CashWithdrawalStep.DETAILS,
                    CashWithdrawalStep.ATM_FEE,
                    CashWithdrawalStep.REVIEW
                ),
                steps
            )
        }

        @Test
        fun `same currency with fee in foreign currency`() {
            val steps = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = false,
                hasFee = true,
                showFeeExchangeRateSection = true
            )
            assertEquals(
                listOf(
                    CashWithdrawalStep.AMOUNT,
                    CashWithdrawalStep.SCOPE,
                    CashWithdrawalStep.DETAILS,
                    CashWithdrawalStep.ATM_FEE,
                    CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    CashWithdrawalStep.REVIEW
                ),
                steps
            )
        }

        @Test
        fun `foreign currency with fee in same currency`() {
            val steps = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = true,
                hasFee = true,
                showFeeExchangeRateSection = false
            )
            assertEquals(
                listOf(
                    CashWithdrawalStep.AMOUNT,
                    CashWithdrawalStep.EXCHANGE_RATE,
                    CashWithdrawalStep.SCOPE,
                    CashWithdrawalStep.DETAILS,
                    CashWithdrawalStep.ATM_FEE,
                    CashWithdrawalStep.REVIEW
                ),
                steps
            )
        }

        @Test
        fun `full flow - foreign currency with fee in foreign currency`() {
            val steps = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = true,
                hasFee = true,
                showFeeExchangeRateSection = true
            )
            assertEquals(
                listOf(
                    CashWithdrawalStep.AMOUNT,
                    CashWithdrawalStep.EXCHANGE_RATE,
                    CashWithdrawalStep.SCOPE,
                    CashWithdrawalStep.DETAILS,
                    CashWithdrawalStep.ATM_FEE,
                    CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    CashWithdrawalStep.REVIEW
                ),
                steps
            )
        }

        @Test
        fun `showFeeExchangeRateSection is ignored when hasFee is false`() {
            val stepsWithFlag = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = false,
                hasFee = false,
                showFeeExchangeRateSection = true
            )
            val stepsWithoutFlag = CashWithdrawalStep.applicableSteps(
                showExchangeRateSection = false,
                hasFee = false,
                showFeeExchangeRateSection = false
            )
            assertEquals(stepsWithoutFlag, stepsWithFlag)
            assertFalse(stepsWithFlag.contains(CashWithdrawalStep.FEE_EXCHANGE_RATE))
        }
    }

    // ── currentStepIndex ──────────────────────────────────────────────────────

    @Nested
    inner class CurrentStepIndex {

        @Test
        fun `AMOUNT step is at index 0 in default state`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.AMOUNT)
            assertEquals(0, state.currentStepIndex)
        }

        @Test
        fun `EXCHANGE_RATE step is at index 1 when exchange rate section is shown`() {
            val state = AddCashWithdrawalUiState(
                showExchangeRateSection = true,
                currentStep = CashWithdrawalStep.EXCHANGE_RATE
            )
            assertEquals(1, state.currentStepIndex)
        }

        @Test
        fun `step not in applicable steps is clamped to index 0`() {
            // EXCHANGE_RATE step is not applicable when showExchangeRateSection = false
            val state = AddCashWithdrawalUiState(
                showExchangeRateSection = false,
                currentStep = CashWithdrawalStep.EXCHANGE_RATE
            )
            assertEquals(0, state.currentStepIndex)
        }

        @Test
        fun `REVIEW step is at last index`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.REVIEW)
            val lastIndex = state.applicableSteps.lastIndex
            assertEquals(lastIndex, state.currentStepIndex)
        }
    }

    // ── canGoNext ─────────────────────────────────────────────────────────────

    @Nested
    inner class CanGoNext {

        @Test
        fun `returns true when on first step`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.AMOUNT)
            assertTrue(state.canGoNext)
        }

        @Test
        fun `returns false when on last step (REVIEW)`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.REVIEW)
            assertFalse(state.canGoNext)
        }

        @Test
        fun `returns true for any non-last step`() {
            val state = AddCashWithdrawalUiState(
                showExchangeRateSection = true,
                currentStep = CashWithdrawalStep.EXCHANGE_RATE
            )
            assertTrue(state.canGoNext)
        }
    }

    // ── isOnReviewStep ────────────────────────────────────────────────────────

    @Nested
    inner class IsOnReviewStep {

        @Test
        fun `returns false for default AMOUNT step`() {
            val state = AddCashWithdrawalUiState()
            assertFalse(state.isOnReviewStep)
        }

        @Test
        fun `returns true when current step is REVIEW`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.REVIEW)
            assertTrue(state.isOnReviewStep)
        }
    }

    // ── withStepClamped ───────────────────────────────────────────────────────

    @Nested
    inner class WithStepClamped {

        @Test
        fun `returns same instance when current step is still applicable`() {
            val state = AddCashWithdrawalUiState(
                showExchangeRateSection = true,
                currentStep = CashWithdrawalStep.EXCHANGE_RATE
            )
            assertSame(state, state.withStepClamped())
        }

        @Test
        fun `clamps to nearest prior step when current step is removed`() {
            // Simulate: user was on EXCHANGE_RATE, then switched back to group currency
            val state = AddCashWithdrawalUiState(
                showExchangeRateSection = false, // EXCHANGE_RATE no longer applicable
                currentStep = CashWithdrawalStep.EXCHANGE_RATE
            )
            val clamped = state.withStepClamped()
            assertEquals(CashWithdrawalStep.AMOUNT, clamped.currentStep)
        }

        @Test
        fun `clamps ATM_FEE to DETAILS when fee is disabled`() {
            // Simulate: user was on ATM_FEE, then toggled fee off
            val state = AddCashWithdrawalUiState(
                hasFee = false,
                currentStep = CashWithdrawalStep.ATM_FEE
            )
            val clamped = state.withStepClamped()
            assertEquals(CashWithdrawalStep.DETAILS, clamped.currentStep)
        }

        @Test
        fun `clamps FEE_EXCHANGE_RATE to ATM_FEE when fee currency becomes group currency`() {
            val state = AddCashWithdrawalUiState(
                hasFee = true,
                showFeeExchangeRateSection = false, // fee exchange rate no longer needed
                currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE
            )
            val clamped = state.withStepClamped()
            assertEquals(CashWithdrawalStep.ATM_FEE, clamped.currentStep)
        }

        @Test
        fun `clamps to first step when no prior applicable step exists`() {
            // AMOUNT is always first — no step can be "before" it
            // Construct an impossible state to verify fallback to first
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.AMOUNT)
            // AMOUNT is always in applicableSteps, so this returns same instance
            assertSame(state, state.withStepClamped())
        }
    }

    // ── isOnOptionalStep ──────────────────────────────────────────────────────

    @Nested
    inner class IsOnOptionalStep {

        @Test
        fun `returns true when on DETAILS (optional step)`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.DETAILS)
            assertTrue(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on AMOUNT (required step)`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.AMOUNT)
            assertFalse(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on SCOPE (required step)`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.SCOPE)
            assertFalse(state.isOnOptionalStep)
        }

        @Test
        fun `returns false when on REVIEW`() {
            val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.REVIEW)
            assertFalse(state.isOnOptionalStep)
        }
    }

    // ── optionalStepIndices ─────────────────────────────────────────────────

    @Nested
    inner class OptionalStepIndices {

        @Test
        fun `returns index of DETAILS step only (minimal flow)`() {
            val state = AddCashWithdrawalUiState()
            val steps = state.applicableSteps
            val detailsIndex = steps.indexOf(CashWithdrawalStep.DETAILS)
            assertEquals(setOf(detailsIndex), state.optionalStepIndices)
        }

        @Test
        fun `optional indices point to correct steps`() {
            val state = AddCashWithdrawalUiState(
                showExchangeRateSection = true,
                hasFee = true,
                showFeeExchangeRateSection = true
            )
            val steps = state.applicableSteps
            state.optionalStepIndices.forEach { index ->
                assertTrue(steps[index].isOptional, "Step at index $index should be optional")
            }
        }
    }

    // ── isCurrentStepValid ────────────────────────────────────────────────────

    @Nested
    inner class IsCurrentStepValid {

        @Nested
        inner class AmountStep {

            @Test
            fun `returns false when amount is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.AMOUNT,
                    withdrawalAmount = "",
                    isAmountValid = true
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when amount format is invalid`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.AMOUNT,
                    withdrawalAmount = "100",
                    isAmountValid = false
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns true when amount is filled and valid`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.AMOUNT,
                    withdrawalAmount = "100",
                    isAmountValid = true
                )
                assertTrue(state.isCurrentStepValid)
            }
        }

        @Nested
        inner class ExchangeRateStep {

            @Test
            fun `returns false when exchange rate is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "",
                    deductedAmount = "95"
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when deducted amount is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "1.05",
                    deductedAmount = ""
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns true when both rate and deducted amount are filled`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "1.05",
                    deductedAmount = "95"
                )
                assertTrue(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when exchange rate is zero or negative`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "0",
                    deductedAmount = "95"
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when deducted amount is zero or negative`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "1.05",
                    deductedAmount = "-10"
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when isExchangeRateValid is false`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "1.05",
                    deductedAmount = "95",
                    isExchangeRateValid = false
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when isDeductedAmountValid is false`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.EXCHANGE_RATE,
                    displayExchangeRate = "1.05",
                    deductedAmount = "95",
                    isDeductedAmountValid = false
                )
                assertFalse(state.isCurrentStepValid)
            }
        }

        @Nested
        inner class ScopeStep {

            @Test
            fun `always returns true (radio always has a value)`() {
                val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.SCOPE)
                assertTrue(state.isCurrentStepValid)
            }
        }

        @Nested
        inner class DetailsStep {

            @Test
            fun `always returns true (all fields optional)`() {
                val state = AddCashWithdrawalUiState(currentStep = CashWithdrawalStep.DETAILS)
                assertTrue(state.isCurrentStepValid)
            }
        }

        @Nested
        inner class AtmFeeStep {

            @Test
            fun `returns false when fee amount is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.ATM_FEE,
                    feeAmount = "",
                    isFeeAmountValid = true
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when fee amount format is invalid`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.ATM_FEE,
                    feeAmount = "5",
                    isFeeAmountValid = false
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns true when fee amount is filled and valid`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.ATM_FEE,
                    feeAmount = "5",
                    isFeeAmountValid = true
                )
                assertTrue(state.isCurrentStepValid)
            }
        }

        @Nested
        inner class FeeExchangeRateStep {

            @Test
            fun `returns false when fee exchange rate is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "",
                    feeConvertedAmount = "4.75"
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when fee converted amount is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "0.95",
                    feeConvertedAmount = ""
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns true when both fee rate and converted amount are filled`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "0.95",
                    feeConvertedAmount = "4.75"
                )
                assertTrue(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when fee exchange rate is zero or negative`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "0",
                    feeConvertedAmount = "4.75"
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when fee converted amount is zero or negative`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "0.95",
                    feeConvertedAmount = "-5"
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when isFeeExchangeRateValid is false`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "0.95",
                    feeConvertedAmount = "4.75",
                    isFeeExchangeRateValid = false
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `returns false when isFeeConvertedAmountValid is false`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE,
                    feeExchangeRate = "0.95",
                    feeConvertedAmount = "4.75",
                    isFeeConvertedAmountValid = false
                )
                assertFalse(state.isCurrentStepValid)
            }
        }

        @Nested
        inner class ReviewStep {

            @Test
            fun `delegates to isFormValid - returns false when amount is blank`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.REVIEW,
                    withdrawalAmount = ""
                )
                assertFalse(state.isCurrentStepValid)
            }

            @Test
            fun `delegates to isFormValid - returns true when form is valid`() {
                val state = AddCashWithdrawalUiState(
                    currentStep = CashWithdrawalStep.REVIEW,
                    withdrawalAmount = "200",
                    isAmountValid = true,
                    showExchangeRateSection = false
                )
                assertTrue(state.isCurrentStepValid)
            }
        }
    }
}
