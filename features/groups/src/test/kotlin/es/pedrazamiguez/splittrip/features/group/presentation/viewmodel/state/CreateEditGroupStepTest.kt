package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.group.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val TOTAL_STEP_COUNT = 7
private const val CONTEXT_INFO_ITEMS_COUNT = 2

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
        fun `all 7 steps have unique infoTitleRes and infoDescriptionRes`() {
            val steps = CreateEditGroupStep.entries
            assertEquals(TOTAL_STEP_COUNT, steps.size)
            assertEquals(TOTAL_STEP_COUNT, steps.map { it.infoTitleRes }.toSet().size)
            assertEquals(TOTAL_STEP_COUNT, steps.map { it.infoDescriptionRes }.toSet().size)
        }

        @Test
        fun `each step maps to its expected infoTitleRes and infoDescriptionRes`() {
            val expectedMappings = mapOf(
                CreateEditGroupStep.INFO to (
                    R.string.group_step_info_info_title to R.string.group_step_info_info_description
                    ),
                CreateEditGroupStep.CONTEXT to (
                    R.string.group_step_info_context_title to R.string.group_step_info_context_description
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
        fun `CONTEXT step has Trip and Everyday infoItems and other steps have empty infoItems`() {
            val contextItems = CreateEditGroupStep.CONTEXT.infoItems
            assertEquals(CONTEXT_INFO_ITEMS_COUNT, contextItems.size)
            assertEquals(R.string.group_step_info_context_item_travel_title, contextItems[0].titleRes)
            assertEquals(R.string.group_step_info_context_item_travel_description, contextItems[0].descriptionRes)
            assertEquals(R.string.group_step_info_context_item_everyday_title, contextItems[1].titleRes)
            assertEquals(R.string.group_step_info_context_item_everyday_description, contextItems[1].descriptionRes)

            CreateEditGroupStep.entries
                .filter { it != CreateEditGroupStep.CONTEXT }
                .forEach { step ->
                    assertTrue(step.infoItems.isEmpty(), "infoItems should be empty for $step")
                }
        }
    }
}
