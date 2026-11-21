package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.expense.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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
        fun `returns full 13-step sequence when all conditionals are true`() {
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
                AddExpenseStep.REVIEW
            )
            assertEquals(expected, steps)
        }

        @Test
        fun `returns 10-step sequence when no conditionals apply`() {
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
        fun `CONTRIBUTION_SCOPE is optional`() {
            assertTrue(AddExpenseStep.CONTRIBUTION_SCOPE.isOptional)
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
        fun `REVIEW is not optional`() {
            assertFalse(AddExpenseStep.REVIEW.isOptional)
        }
    }

    // ── Minimum viable path ──────────────────────────────────────────────────

    @Nested
    @DisplayName("MinimumViablePath")
    inner class MinimumViablePath {

        @Test
        fun `mandatory non-review steps are TITLE PAYMENT_METHOD and AMOUNT`() {
            // TITLE, PAYMENT_METHOD, and AMOUNT are always-shown mandatory steps.
            // CONTRIBUTION_SCOPE is optional and defaults to PayerType.USER ("For you").
            val mandatoryNonReview = AddExpenseStep.entries.filter { !it.isOptional && !it.isReview }
            assertEquals(3, mandatoryNonReview.size)
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.TITLE))
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.PAYMENT_METHOD))
            assertTrue(mandatoryNonReview.contains(AddExpenseStep.AMOUNT))
            assertFalse(mandatoryNonReview.contains(AddExpenseStep.CONTRIBUTION_SCOPE))
        }

        @Test
        fun `REVIEW is the only step with isReview true`() {
            val reviewSteps = AddExpenseStep.entries.filter { it.isReview }
            assertEquals(1, reviewSteps.size)
            assertEquals(AddExpenseStep.REVIEW, reviewSteps.first())
        }

        @Test
        fun `all steps after AMOUNT and before REVIEW are optional`() {
            // Every step strictly between AMOUNT and REVIEW must be optional
            val steps = AddExpenseStep.entries.toList()
            val amountIdx = steps.indexOf(AddExpenseStep.AMOUNT)
            val reviewIdx = steps.indexOf(AddExpenseStep.REVIEW)
            val middle = steps.subList(amountIdx + 1, reviewIdx)
            val mandatoryMiddle = middle.filter { !it.isOptional }
            assertTrue(mandatoryMiddle.isEmpty())
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

    // ── Step Info Resources ──────────────────────────────────────────────────

    @Nested
    @DisplayName("StepInfoResources")
    inner class StepInfoResources {

        @Test
        fun `every step has non-zero infoTitleRes and infoDescriptionRes`() {
            AddExpenseStep.entries.forEach { step ->
                assertNotEquals(0, step.infoTitleRes, "infoTitleRes should not be 0 for $step")
                assertNotEquals(0, step.infoDescriptionRes, "infoDescriptionRes should not be 0 for $step")
            }
        }

        @Test
        fun `all 13 steps have unique infoTitleRes and infoDescriptionRes`() {
            val steps = AddExpenseStep.entries
            assertEquals(13, steps.size)
            assertEquals(13, steps.map { it.infoTitleRes }.toSet().size)
            assertEquals(13, steps.map { it.infoDescriptionRes }.toSet().size)
        }

        @Test
        fun `each step maps to its expected infoTitleRes and infoDescriptionRes`() {
            val expectedMappings = mapOf(
                AddExpenseStep.TITLE to (
                    R.string.expense_step_info_title_title to R.string.expense_step_info_title_description
                    ),
                AddExpenseStep.PAYMENT_METHOD to (
                    R.string.expense_step_info_payment_method_title to
                        R.string.expense_step_info_payment_method_description
                    ),
                AddExpenseStep.AMOUNT to (
                    R.string.expense_step_info_amount_title to R.string.expense_step_info_amount_description
                    ),
                AddExpenseStep.EXCHANGE_RATE to (
                    R.string.expense_step_info_exchange_rate_title to
                        R.string.expense_step_info_exchange_rate_description
                    ),
                AddExpenseStep.SPLIT to (
                    R.string.expense_step_info_split_title to R.string.expense_step_info_split_description
                    ),
                AddExpenseStep.CATEGORY to (
                    R.string.expense_step_info_category_title to R.string.expense_step_info_category_description
                    ),
                AddExpenseStep.FUNDING_SOURCE to (
                    R.string.expense_step_info_funding_source_title to
                        R.string.expense_step_info_funding_source_description
                    ),
                AddExpenseStep.CONTRIBUTION_SCOPE to (
                    R.string.expense_step_info_contribution_scope_title to
                        R.string.expense_step_info_contribution_scope_description
                    ),
                AddExpenseStep.VENDOR_NOTES to (
                    R.string.expense_step_info_vendor_notes_title to
                        R.string.expense_step_info_vendor_notes_description
                    ),
                AddExpenseStep.PAYMENT_STATUS to (
                    R.string.expense_step_info_payment_status_title to
                        R.string.expense_step_info_payment_status_description
                    ),
                AddExpenseStep.RECEIPT to (
                    R.string.expense_step_info_receipt_title to R.string.expense_step_info_receipt_description
                    ),
                AddExpenseStep.ADD_ONS to (
                    R.string.expense_step_info_add_ons_title to R.string.expense_step_info_add_ons_description
                    ),
                AddExpenseStep.REVIEW to (
                    R.string.expense_step_info_review_title to R.string.expense_step_info_review_description
                    )
            )

            expectedMappings.forEach { (step, expectedPair) ->
                assertEquals(expectedPair.first, step.infoTitleRes, "Title mismatch for $step")
                assertEquals(expectedPair.second, step.infoDescriptionRes, "Description mismatch for $step")
            }
        }

        @Test
        fun `multi-option steps have non-empty infoItems with valid unique resources`() {
            val expectedCounts = mapOf(
                AddExpenseStep.SPLIT to 3,
                AddExpenseStep.FUNDING_SOURCE to 2,
                AddExpenseStep.CONTRIBUTION_SCOPE to 3,
                AddExpenseStep.PAYMENT_STATUS to 3,
                AddExpenseStep.ADD_ONS to 2
            )

            expectedCounts.forEach { (step, expectedSize) ->
                assertEquals(expectedSize, step.infoItems.size, "Unexpected infoItems size for $step")
                step.infoItems.forEach { item ->
                    assertNotEquals(0, item.titleRes, "titleRes should not be 0 for $step")
                    assertNotEquals(0, item.descriptionRes, "descriptionRes should not be 0 for $step")
                }
                assertEquals(
                    expectedSize,
                    step.infoItems.map { it.titleRes }.toSet().size,
                    "Duplicate titleRes in $step"
                )
                assertEquals(
                    expectedSize,
                    step.infoItems.map { it.descriptionRes }.toSet().size,
                    "Duplicate descriptionRes in $step"
                )
            }
        }

        @Test
        fun `single-paragraph steps have empty infoItems`() {
            val singleParagraphSteps = listOf(
                AddExpenseStep.TITLE,
                AddExpenseStep.PAYMENT_METHOD,
                AddExpenseStep.AMOUNT,
                AddExpenseStep.EXCHANGE_RATE,
                AddExpenseStep.CATEGORY,
                AddExpenseStep.VENDOR_NOTES,
                AddExpenseStep.RECEIPT,
                AddExpenseStep.REVIEW
            )

            singleParagraphSteps.forEach { step ->
                assertTrue(step.infoItems.isEmpty(), "Expected empty infoItems for $step")
            }
        }

        @Test
        fun `FUNDING_SOURCE infoItems orders Your Money before Group Pocket`() {
            val items = AddExpenseStep.FUNDING_SOURCE.infoItems
            assertEquals(2, items.size)
            assertEquals(R.string.expense_step_info_funding_source_item_your_money_title, items[0].titleRes)
            assertEquals(R.string.expense_step_info_funding_source_item_group_pocket_title, items[1].titleRes)
        }
    }
}
