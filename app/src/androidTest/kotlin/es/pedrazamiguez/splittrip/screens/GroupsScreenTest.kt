package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.features.group.R
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
}
