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
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupItem
import es.pedrazamiguez.splittrip.features.group.presentation.component.SelectedGroupCard
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.screen.GroupsScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [GroupsScreen].
 */
@RunWith(AndroidJUnit4::class)
class GroupsScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersEmptyState_whenGroupsListIsEmpty() {
        val emptyTitle = context.getString(R.string.groups_not_found)
        val bannerText = context.getString(R.string.groups_anonymous_warning)

        composeRule.setContent {
            SplitTripTheme {
                GroupsScreen(
                    uiState = GroupsUiState(
                        isLoading = false,
                        groups = persistentListOf(),
                        isAnonymous = false
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(bannerText).assertDoesNotExist()
    }

    @Test
    fun rendersAnonymousBannerAndEmptyState_whenAnonymousAndGroupsListIsEmpty() {
        val emptyTitle = context.getString(R.string.groups_not_found)
        val bannerText = context.getString(R.string.groups_anonymous_warning)

        composeRule.setContent {
            SplitTripTheme {
                GroupsScreen(
                    uiState = GroupsUiState(
                        isLoading = false,
                        groups = persistentListOf(),
                        isAnonymous = true
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(bannerText).assertIsDisplayed()
        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
    }

    @Test
    fun rendersSelectedGroupCard_whenActive_displaysActiveNowBadgeAndHidesArchivedLabel() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                SelectedGroupCard(
                    groupUiModel = GroupUiModel(
                        id = "g1",
                        name = "Active Trip",
                        currency = "EUR",
                        status = GroupStatus.ACTIVE
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(activeBadgeText).assertIsDisplayed()
        composeRule.onNodeWithText(archivedLabel).assertDoesNotExist()
    }

    @Test
    fun rendersSelectedGroupCard_whenArchived_displaysArchivedLabelAndHidesActiveNowBadge() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                SelectedGroupCard(
                    groupUiModel = GroupUiModel(
                        id = "g1",
                        name = "Archived Trip",
                        currency = "EUR",
                        status = GroupStatus.ARCHIVED
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(archivedLabel).assertIsDisplayed()
        composeRule.onNodeWithText(activeBadgeText).assertDoesNotExist()
    }

    @Test
    fun rendersGroupItem_whenActive_hidesArchivedLabel() {
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                GroupItem(
                    groupUiModel = GroupUiModel(
                        id = "g2",
                        name = "Active Group",
                        currency = "USD",
                        status = GroupStatus.ACTIVE
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(archivedLabel).assertDoesNotExist()
    }

    @Test
    fun rendersGroupItem_whenArchived_displaysArchivedLabel() {
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)

        composeRule.setContent {
            SplitTripTheme {
                GroupItem(
                    groupUiModel = GroupUiModel(
                        id = "g2",
                        name = "Archived Group",
                        currency = "USD",
                        status = GroupStatus.ARCHIVED
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(archivedLabel).assertIsDisplayed()
    }

    @Test
    fun rendersGroupsScreen_whenSelectedAndUnselectedGroupsArchived_displaysArchivedLabelsWithoutActiveNow() {
        val activeBadgeText = context.getString(R.string.group_active_badge)
        val archivedLabel = context.getString(DesignR.string.group_detail_archived_label)
        val selectedArchived = GroupUiModel(
            id = "g1",
            name = "Selected Archived",
            currency = "EUR",
            status = GroupStatus.ARCHIVED
        )
        val unselectedArchived = GroupUiModel(
            id = "g2",
            name = "Unselected Archived",
            currency = "USD",
            status = GroupStatus.ARCHIVED
        )
        val unselectedActive = GroupUiModel(
            id = "g3",
            name = "Unselected Active",
            currency = "GBP",
            status = GroupStatus.ACTIVE
        )

        composeRule.setContent {
            SplitTripTheme {
                GroupsScreen(
                    uiState = GroupsUiState(
                        isLoading = false,
                        groups = persistentListOf(selectedArchived, unselectedArchived, unselectedActive)
                    ),
                    selectedGroupId = selectedArchived.id
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(activeBadgeText).assertDoesNotExist()
        composeRule.onAllNodesWithText(archivedLabel).assertCountEquals(2)
    }
}
