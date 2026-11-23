package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.screen.GroupDetailScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [GroupDetailScreen].
 */
@RunWith(AndroidJUnit4::class)
class GroupDetailScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersArchivedNonSelectedGroup_displaysSingleArchivedBadgeAndHidesActiveBadge() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                GroupDetailScreen(
                    uiState = GroupDetailUiState(
                        isLoading = false,
                        group = GroupUiModel(
                            id = "g1",
                            name = "Archived Trip",
                            currency = "EUR",
                            status = GroupStatus.ARCHIVED
                        )
                    ),
                    isActiveGroup = false
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(archivedLabel).assertCountEquals(1)
        composeRule.onNodeWithText(activeBadgeText).assertDoesNotExist()
    }

    @Test
    fun rendersArchivedSelectedGroup_displaysSingleArchivedBadgeAndHidesActiveBadge() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                GroupDetailScreen(
                    uiState = GroupDetailUiState(
                        isLoading = false,
                        group = GroupUiModel(
                            id = "g1",
                            name = "Archived Trip",
                            currency = "EUR",
                            status = GroupStatus.ARCHIVED
                        )
                    ),
                    isActiveGroup = true
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(archivedLabel).assertCountEquals(1)
        composeRule.onNodeWithText(activeBadgeText).assertDoesNotExist()
    }

    @Test
    fun rendersActiveSelectedGroup_displaysActiveBadgeAndHidesArchivedLabel() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                GroupDetailScreen(
                    uiState = GroupDetailUiState(
                        isLoading = false,
                        group = GroupUiModel(
                            id = "g1",
                            name = "Active Trip",
                            currency = "EUR",
                            status = GroupStatus.ACTIVE
                        )
                    ),
                    isActiveGroup = true
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(activeBadgeText).assertIsDisplayed()
        composeRule.onNodeWithText(archivedLabel).assertDoesNotExist()
    }

    @Test
    fun rendersActiveNonSelectedGroup_hidesBothActiveAndArchivedBadges() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                GroupDetailScreen(
                    uiState = GroupDetailUiState(
                        isLoading = false,
                        group = GroupUiModel(
                            id = "g1",
                            name = "Active Trip",
                            currency = "EUR",
                            status = GroupStatus.ACTIVE
                        )
                    ),
                    isActiveGroup = false
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(activeBadgeText).assertDoesNotExist()
        composeRule.onNodeWithText(archivedLabel).assertDoesNotExist()
    }
}
