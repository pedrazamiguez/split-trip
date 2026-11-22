package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.withdrawal.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CashWithdrawalStepTest {

    @Nested
    @DisplayName("StepInfoResources")
    inner class StepInfoResources {

        @Test
        fun `every step has non-zero infoTitleRes and infoDescriptionRes`() {
            CashWithdrawalStep.entries.forEach { step ->
                assertNotEquals(0, step.infoTitleRes, "infoTitleRes should not be 0 for $step")
                assertNotEquals(0, step.infoDescriptionRes, "infoDescriptionRes should not be 0 for $step")
            }
        }

        @Test
        fun `all 7 steps have unique infoTitleRes and infoDescriptionRes`() {
            val steps = CashWithdrawalStep.entries
            assertEquals(7, steps.size)
            assertEquals(7, steps.map { it.infoTitleRes }.toSet().size)
            assertEquals(7, steps.map { it.infoDescriptionRes }.toSet().size)
        }

        @Test
        fun `each step maps to its expected infoTitleRes and infoDescriptionRes`() {
            val expectedMappings = mapOf(
                CashWithdrawalStep.AMOUNT to (
                    R.string.withdrawal_step_info_amount_title to
                        R.string.withdrawal_step_info_amount_description
                    ),
                CashWithdrawalStep.EXCHANGE_RATE to (
                    R.string.withdrawal_step_info_exchange_rate_title to
                        R.string.withdrawal_step_info_exchange_rate_description
                    ),
                CashWithdrawalStep.SCOPE to (
                    R.string.withdrawal_step_info_scope_title to
                        R.string.withdrawal_step_info_scope_description
                    ),
                CashWithdrawalStep.DETAILS to (
                    R.string.withdrawal_step_info_details_title to
                        R.string.withdrawal_step_info_details_description
                    ),
                CashWithdrawalStep.ATM_FEE to (
                    R.string.withdrawal_step_info_atm_fee_title to
                        R.string.withdrawal_step_info_atm_fee_description
                    ),
                CashWithdrawalStep.FEE_EXCHANGE_RATE to (
                    R.string.withdrawal_step_info_fee_exchange_rate_title to
                        R.string.withdrawal_step_info_fee_exchange_rate_description
                    ),
                CashWithdrawalStep.REVIEW to (
                    R.string.withdrawal_step_info_review_title to
                        R.string.withdrawal_step_info_review_description
                    )
            )

            assertEquals(CashWithdrawalStep.entries.size, expectedMappings.size)
            expectedMappings.forEach { (step, expected) ->
                assertEquals(expected.first, step.infoTitleRes, "Title mismatch for $step")
                assertEquals(expected.second, step.infoDescriptionRes, "Description mismatch for $step")
            }
        }

        @Test
        fun `SCOPE has 3 infoItems with valid unique resources ordered Group Subunit Personal`() {
            val items = CashWithdrawalStep.SCOPE.infoItems
            assertEquals(3, items.size)
            items.forEach { item ->
                assertNotEquals(0, item.titleRes)
                assertNotEquals(0, item.descriptionRes)
            }
            assertEquals(3, items.map { it.titleRes }.toSet().size)
            assertEquals(3, items.map { it.descriptionRes }.toSet().size)
            assertEquals(
                listOf(
                    R.string.withdrawal_step_info_scope_item_for_group_title,
                    R.string.withdrawal_step_info_scope_item_for_subunit_title,
                    R.string.withdrawal_step_info_scope_item_for_you_title
                ),
                items.map { it.titleRes }
            )
        }

        @Test
        fun `single-paragraph steps have empty infoItems`() {
            listOf(
                CashWithdrawalStep.AMOUNT,
                CashWithdrawalStep.EXCHANGE_RATE,
                CashWithdrawalStep.DETAILS,
                CashWithdrawalStep.ATM_FEE,
                CashWithdrawalStep.FEE_EXCHANGE_RATE,
                CashWithdrawalStep.REVIEW
            ).forEach { step ->
                assertTrue(step.infoItems.isEmpty(), "infoItems should be empty for $step")
            }
        }
    }
}
