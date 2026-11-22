package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.subunit.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateEditSubunitStepTest {

    @Nested
    @DisplayName("StepInfoResources")
    inner class StepInfoResources {

        @Test
        fun `every step has non-zero infoTitleRes and infoDescriptionRes`() {
            CreateEditSubunitStep.entries.forEach { step ->
                assertNotEquals(0, step.infoTitleRes, "infoTitleRes should not be 0 for $step")
                assertNotEquals(0, step.infoDescriptionRes, "infoDescriptionRes should not be 0 for $step")
            }
        }

        @Test
        fun `all 4 steps have unique infoTitleRes and infoDescriptionRes`() {
            val steps = CreateEditSubunitStep.entries
            assertEquals(4, steps.size)
            assertEquals(4, steps.map { it.infoTitleRes }.toSet().size)
            assertEquals(4, steps.map { it.infoDescriptionRes }.toSet().size)
        }

        @Test
        fun `each step maps to its expected infoTitleRes and infoDescriptionRes`() {
            val expectedMappings = mapOf(
                CreateEditSubunitStep.NAME to (
                    R.string.subunit_step_info_name_title to R.string.subunit_step_info_name_description
                    ),
                CreateEditSubunitStep.MEMBERS to (
                    R.string.subunit_step_info_members_title to R.string.subunit_step_info_members_description
                    ),
                CreateEditSubunitStep.SHARES to (
                    R.string.subunit_step_info_shares_title to R.string.subunit_step_info_shares_description
                    ),
                CreateEditSubunitStep.REVIEW to (
                    R.string.subunit_step_info_review_title to R.string.subunit_step_info_review_description
                    )
            )

            assertEquals(CreateEditSubunitStep.entries.size, expectedMappings.size)
            expectedMappings.forEach { (step, expected) ->
                assertEquals(expected.first, step.infoTitleRes, "Title mismatch for $step")
                assertEquals(expected.second, step.infoDescriptionRes, "Description mismatch for $step")
            }
        }

        @Test
        fun `all steps have empty infoItems`() {
            CreateEditSubunitStep.entries.forEach { step ->
                assertTrue(step.infoItems.isEmpty(), "infoItems should be empty for $step")
            }
        }
    }
}
