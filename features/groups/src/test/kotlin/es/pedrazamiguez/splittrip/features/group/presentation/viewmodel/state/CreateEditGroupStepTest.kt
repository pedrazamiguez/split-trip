package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.group.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateEditGroupStepTest {

    @Nested
    @DisplayName("StepInfoResources")
    inner class StepInfoResources {

        @Test
        fun `every step has non-zero infoTitleRes and infoDescriptionRes`() {
            CreateEditGroupStep.entries.forEach { step ->
                assertNotEquals(0, step.infoTitleRes, "infoTitleRes should not be 0 for $step")
                assertNotEquals(0, step.infoDescriptionRes, "infoDescriptionRes should not be 0 for $step")
            }
        }

        @Test
        fun `all 6 steps have unique infoTitleRes and infoDescriptionRes`() {
            val steps = CreateEditGroupStep.entries
            assertEquals(6, steps.size)
            assertEquals(6, steps.map { it.infoTitleRes }.toSet().size)
            assertEquals(6, steps.map { it.infoDescriptionRes }.toSet().size)
        }

        @Test
        fun `each step maps to its expected infoTitleRes and infoDescriptionRes`() {
            val expectedMappings = mapOf(
                CreateEditGroupStep.INFO to (
                    R.string.group_step_info_info_title to R.string.group_step_info_info_description
                    ),
                CreateEditGroupStep.CURRENCY to (
                    R.string.group_step_info_currency_title to R.string.group_step_info_currency_description
                    ),
                CreateEditGroupStep.MEMBERS to (
                    R.string.group_step_info_members_title to R.string.group_step_info_members_description
                    ),
                CreateEditGroupStep.UNREGISTERED_NAMES to (
                    R.string.group_step_info_unregistered_names_title to
                        R.string.group_step_info_unregistered_names_description
                    ),
                CreateEditGroupStep.IMAGE to (
                    R.string.group_step_info_image_title to R.string.group_step_info_image_description
                    ),
                CreateEditGroupStep.REVIEW to (
                    R.string.group_step_info_review_title to R.string.group_step_info_review_description
                    )
            )

            assertEquals(CreateEditGroupStep.entries.size, expectedMappings.size)
            expectedMappings.forEach { (step, expected) ->
                assertEquals(expected.first, step.infoTitleRes, "Title mismatch for $step")
                assertEquals(expected.second, step.infoDescriptionRes, "Description mismatch for $step")
            }
        }

        @Test
        fun `all steps have empty infoItems`() {
            CreateEditGroupStep.entries.forEach { step ->
                assertTrue(step.infoItems.isEmpty(), "infoItems should be empty for $step")
            }
        }
    }
}
