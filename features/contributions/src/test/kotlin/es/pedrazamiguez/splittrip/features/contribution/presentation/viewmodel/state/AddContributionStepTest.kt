package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.contribution.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddContributionStepTest {

    @Nested
    @DisplayName("StepInfoResources")
    inner class StepInfoResources {

        @Test
        fun `every step has non-zero infoTitleRes and infoDescriptionRes`() {
            AddContributionStep.entries.forEach { step ->
                assertNotEquals(0, step.infoTitleRes, "infoTitleRes should not be 0 for $step")
                assertNotEquals(0, step.infoDescriptionRes, "infoDescriptionRes should not be 0 for $step")
            }
        }

        @Test
        fun `all 4 steps have unique infoTitleRes and infoDescriptionRes`() {
            val steps = AddContributionStep.entries
            assertEquals(4, steps.size)
            assertEquals(4, steps.map { it.infoTitleRes }.toSet().size)
            assertEquals(4, steps.map { it.infoDescriptionRes }.toSet().size)
        }

        @Test
        fun `each step maps to its expected infoTitleRes and infoDescriptionRes`() {
            val expectedMappings = mapOf(
                AddContributionStep.AMOUNT to (
                    R.string.contribution_step_info_amount_title to
                        R.string.contribution_step_info_amount_description
                    ),
                AddContributionStep.SCOPE to (
                    R.string.contribution_step_info_scope_title to
                        R.string.contribution_step_info_scope_description
                    ),
                AddContributionStep.DATE to (
                    R.string.contribution_step_info_date_title to
                        R.string.contribution_step_info_date_description
                    ),
                AddContributionStep.REVIEW to (
                    R.string.contribution_step_info_review_title to
                        R.string.contribution_step_info_review_description
                    )
            )

            assertEquals(AddContributionStep.entries.size, expectedMappings.size)
            expectedMappings.forEach { (step, expected) ->
                assertEquals(expected.first, step.infoTitleRes, "Title mismatch for $step")
                assertEquals(expected.second, step.infoDescriptionRes, "Description mismatch for $step")
            }
        }

        @Test
        fun `SCOPE has 3 infoItems with valid unique resources ordered Group Subunit Personal`() {
            val items = AddContributionStep.SCOPE.infoItems
            assertEquals(3, items.size)
            items.forEach { item ->
                assertNotEquals(0, item.titleRes)
                assertNotEquals(0, item.descriptionRes)
            }
            assertEquals(3, items.map { it.titleRes }.toSet().size)
            assertEquals(3, items.map { it.descriptionRes }.toSet().size)
            assertEquals(
                listOf(
                    R.string.contribution_step_info_scope_item_for_group_title,
                    R.string.contribution_step_info_scope_item_for_subunit_title,
                    R.string.contribution_step_info_scope_item_for_you_title
                ),
                items.map { it.titleRes }
            )
        }

        @Test
        fun `single-paragraph steps have empty infoItems`() {
            listOf(
                AddContributionStep.AMOUNT,
                AddContributionStep.DATE,
                AddContributionStep.REVIEW
            ).forEach { step ->
                assertTrue(step.infoItems.isEmpty(), "infoItems should be empty for $step")
            }
        }
    }
}
