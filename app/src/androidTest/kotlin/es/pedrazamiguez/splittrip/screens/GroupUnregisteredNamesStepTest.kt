package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.step.GroupUnregisteredNamesStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupUnregisteredNamesStepTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun givenSingleUnregisteredMember_thenImeActionIsDone() {
        val state = CreateEditGroupUiState(
            selectedMembers = persistentListOf(
                User(userId = "1", email = "user1@test.com", isPending = true)
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                GroupUnregisteredNamesStep(
                    uiState = state,
                    onEvent = {}
                )
            }
        }

        val nameLabel = context.getString(R.string.group_review_name)

        composeRule.onAllNodesWithText(nameLabel)
            .onFirst()
            .assert(hasImeAction(ImeAction.Done))
    }

    @Test
    fun givenMultipleUnregisteredMembers_thenLastFieldIsDoneAndOthersAreNext() {
        val state = CreateEditGroupUiState(
            selectedMembers = persistentListOf(
                User(userId = "1", email = "user1@test.com", isPending = true),
                User(userId = "2", email = "user2@test.com", isPending = true),
                User(userId = "3", email = "user3@test.com", isPending = true)
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                GroupUnregisteredNamesStep(
                    uiState = state,
                    onEvent = {}
                )
            }
        }

        val nameLabel = context.getString(R.string.group_review_name)

        val nodes = composeRule.onAllNodesWithText(nameLabel)

        nodes[0].assert(hasImeAction(ImeAction.Next))
        nodes[1].assert(hasImeAction(ImeAction.Next))
        nodes[2].assert(hasImeAction(ImeAction.Done))
    }
}
