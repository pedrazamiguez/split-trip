package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AddExpenseStep")
class AddExpenseStepTest {

    // ── applicableSteps ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("applicableSteps()")
    inner class ApplicableSteps {

        @Test
        fun `returns full 14-step sequence when all conditionals are true`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = true,
                showExchangeRateSection = true,
                hasSplit = true
            )
            val expected = listOf(
                AddExpenseStep.TITLE,
                AddExpenseStep.PAYMENT_METHOD,
                AddExpenseStep.AMOUNT,
                AddExpenseStep.EXCHANGE_RATE,
                AddExpenseStep.SPLIT,
                AddExpenseStep.CATEGORY,
                AddExpenseStep.FUNDING_SOURCE,
                AddExpenseStep.CONTRIBUTION_SCOPE,
                AddExpenseStep.VENDOR_NOTES,
                AddExpenseStep.PAYMENT_STATUS,
                AddExpenseStep.RECEIPT,
                AddExpenseStep.ADD_ONS,
                AddExpenseStep.SUB_EXPENSES,
                AddExpenseStep.REVIEW
            )
            assertEquals(expected, steps)
        }

        @Test
        fun `returns 11-step sequence when no conditionals apply`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            val expected = listOf(
                AddExpenseStep.TITLE,
                AddExpenseStep.PAYMENT_METHOD,
                AddExpenseStep.AMOUNT,
                AddExpenseStep.CATEGORY,
                AddExpenseStep.FUNDING_SOURCE,
                AddExpenseStep.VENDOR_NOTES,
                AddExpenseStep.PAYMENT_STATUS,
                AddExpenseStep.RECEIPT,
                AddExpenseStep.ADD_ONS,
                AddExpenseStep.SUB_EXPENSES,
                AddExpenseStep.REVIEW
            )
            assertEquals(expected, steps)
        }

        @Test
        fun `EXCHANGE_RATE is included when showExchangeRateSection is true`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = true,
                hasSplit = false
            )
            assertTrue(steps.contains(AddExpenseStep.EXCHANGE_RATE))
        }

        @Test
        fun `EXCHANGE_RATE is excluded when showExchangeRateSection is false`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            assertFalse(steps.contains(AddExpenseStep.EXCHANGE_RATE))
        }

        @Test
        fun `SPLIT is included when hasSplit is true`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = true
            )
            assertTrue(steps.contains(AddExpenseStep.SPLIT))
        }

        @Test
        fun `SPLIT is excluded when hasSplit is false`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            assertFalse(steps.contains(AddExpenseStep.SPLIT))
        }

        @Test
        fun `CONTRIBUTION_SCOPE is included when showContributionScopeStep is true`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = true,
                showExchangeRateSection = false,
                hasSplit = false
            )
            assertTrue(steps.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `CONTRIBUTION_SCOPE is excluded when showContributionScopeStep is false`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            assertFalse(steps.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `FUNDING_SOURCE is always included`() {
            val withNoConditionals = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            val withAllConditionals = AddExpenseStep.applicableSteps(
                showContributionScopeStep = true,
                showExchangeRateSection = true,
                hasSplit = true
            )
            assertTrue(withNoConditionals.contains(AddExpenseStep.FUNDING_SOURCE))
            assertTrue(withAllConditionals.contains(AddExpenseStep.FUNDING_SOURCE))
        }

        @Test
        fun `FUNDING_SOURCE appears after CATEGORY and before VENDOR_NOTES`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            val categoryIndex = steps.indexOf(AddExpenseStep.CATEGORY)
            val fundingIndex = steps.indexOf(AddExpenseStep.FUNDING_SOURCE)
            val vendorNotesIndex = steps.indexOf(AddExpenseStep.VENDOR_NOTES)
            assertTrue(categoryIndex < fundingIndex)
            assertTrue(fundingIndex < vendorNotesIndex)
        }

        @Test
        fun `CONTRIBUTION_SCOPE appears between FUNDING_SOURCE and VENDOR_NOTES`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = true,
                showExchangeRateSection = false,
                hasSplit = false
            )
            val fundingIndex = steps.indexOf(AddExpenseStep.FUNDING_SOURCE)
            val scopeIndex = steps.indexOf(AddExpenseStep.CONTRIBUTION_SCOPE)
            val vendorNotesIndex = steps.indexOf(AddExpenseStep.VENDOR_NOTES)
            assertTrue(fundingIndex < scopeIndex)
            assertTrue(scopeIndex < vendorNotesIndex)
        }

        @Test
        fun `FUNDING_SOURCE appears after SPLIT when split is applicable`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = true
            )
            val splitIndex = steps.indexOf(AddExpenseStep.SPLIT)
            val fundingIndex = steps.indexOf(AddExpenseStep.FUNDING_SOURCE)
            assertTrue(splitIndex < fundingIndex)
        }

        @Test
        fun `REVIEW is always the last step`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = true,
                showExchangeRateSection = true,
                hasSplit = true
            )
            assertEquals(AddExpenseStep.REVIEW, steps.last())
        }

        @Test
        fun `TITLE is always the first step`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = true,
                showExchangeRateSection = true,
                hasSplit = true
            )
            assertEquals(AddExpenseStep.TITLE, steps.first())
        }
    }

    // ── isOptional ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isOptional")
    inner class IsOptional {

        @Test
        fun `TITLE is not optional`() {
            assertFalse(AddExpenseStep.TITLE.isOptional)
        }

        @Test
        fun `PAYMENT_METHOD is not optional`() {
            assertFalse(AddExpenseStep.PAYMENT_METHOD.isOptional)
        }

        @Test
        fun `AMOUNT is not optional`() {
            assertFalse(AddExpenseStep.AMOUNT.isOptional)
        }

        @Test
        fun `EXCHANGE_RATE is optional`() {
            assertTrue(AddExpenseStep.EXCHANGE_RATE.isOptional)
        }

        @Test
        fun `SPLIT is optional`() {
            assertTrue(AddExpenseStep.SPLIT.isOptional)
        }

        @Test
        fun `CATEGORY is optional`() {
            assertTrue(AddExpenseStep.CATEGORY.isOptional)
        }

        @Test
        fun `FUNDING_SOURCE is optional`() {
            assertTrue(AddExpenseStep.FUNDING_SOURCE.isOptional)
        }

        @Test
        fun `CONTRIBUTION_SCOPE is not optional`() {
            assertFalse(AddExpenseStep.CONTRIBUTION_SCOPE.isOptional)
        }

        @Test
        fun `VENDOR_NOTES is optional`() {
            assertTrue(AddExpenseStep.VENDOR_NOTES.isOptional)
        }

        @Test
        fun `PAYMENT_STATUS is optional`() {
            assertTrue(AddExpenseStep.PAYMENT_STATUS.isOptional)
        }

        @Test
        fun `RECEIPT is optional`() {
            assertTrue(AddExpenseStep.RECEIPT.isOptional)
        }

        @Test
        fun `ADD_ONS is optional`() {
            assertTrue(AddExpenseStep.ADD_ONS.isOptional)
        }

        @Test
        fun `SUB_EXPENSES is optional`() {
            assertTrue(AddExpenseStep.SUB_EXPENSES.isOptional)
        }

        @Test
        fun `REVIEW is not optional`() {
            assertFalse(AddExpenseStep.REVIEW.isOptional)
        }
    }

    // ── Minimum viable path ──────────────────────────────────────────────────

    @Nested
    @DisplayName("MinimumViablePath")
    inner class MinimumViablePath {

        @Test
        fun `mandatory non-review steps are TITLE PAYMENT_METHOD AMOUNT and CONTRIBUTION_SCOPE`() {
            // TITLE, PAYMENT_METHOD, AMOUNT are always-shown mandatory steps.
            // CONTRIBUTION_SCOPE is also mandatory when shown (conditional on FUNDING_SOURCE=MY_MONEY).
            val mandatoryNonReview = AddExpenseStep.entries.filter { !it.isOptional && !it.isReview }
            assertEquals(4, mandatoryNonReview.size)
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.TITLE))
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.PAYMENT_METHOD))
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.AMOUNT))
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `REVIEW is the only step with isReview true`() {
            val reviewSteps = AddExpenseStep.entries.filter { it.isReview }
            assertEquals(1, reviewSteps.size)
            assertEquals(AddExpenseStep.REVIEW, reviewSteps.first())
        }

        @Test
        fun `all steps after AMOUNT and before REVIEW are optional except CONTRIBUTION_SCOPE`() {
            // Every step strictly between AMOUNT and REVIEW must be optional
            // unless it is the conditional CONTRIBUTION_SCOPE step
            val steps = AddExpenseStep.entries.toList()
            val amountIdx = steps.indexOf(AddExpenseStep.AMOUNT)
            val reviewIdx = steps.indexOf(AddExpenseStep.REVIEW)
            val middle = steps.subList(amountIdx + 1, reviewIdx)
            val mandatoryMiddle = middle.filter { !it.isOptional }
            // Only CONTRIBUTION_SCOPE should be mandatory in this range
            assertEquals(listOf(AddExpenseStep.CONTRIBUTION_SCOPE), mandatoryMiddle)
        }

        @Test
        fun `minimum path from applicableSteps contains exactly TITLE PAYMENT_METHOD AMOUNT and REVIEW`() {
            val steps = AddExpenseStep.applicableSteps(
                showContributionScopeStep = false,
                showExchangeRateSection = false,
                hasSplit = false
            )
            val mandatorySteps = steps.filter { !it.isOptional }
            val expected = listOf(
                AddExpenseStep.TITLE,
                AddExpenseStep.PAYMENT_METHOD,
                AddExpenseStep.AMOUNT,
                AddExpenseStep.REVIEW
            )
            assertEquals(expected, mandatorySteps)
        }
    }
}
