package es.pedrazamiguez.splittrip.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.LocalIsProUser
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.PROFILE_AVATAR_BUTTON_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.PROFILE_AVATAR_PRO_BADGE_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI instrumentation tests for [ProfileAvatarButton].
 *
 * Verifies that the subtle PRO indicator and [LocalIsProUser] behavior render
 * properly across free and pro states.
 */
@RunWith(AndroidJUnit4::class)
class ProfileAvatarButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun freeUser_rendersWithoutProBadge_andWithCorrectContentDescription() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileAvatarButton(
                    avatarUrl = null,
                    onClick = {},
                    isPro = false
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PROFILE_AVATAR_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun proUser_rendersProBadge_andDisplaysProText() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileAvatarButton(
                    avatarUrl = null,
                    onClick = {},
                    isPro = true
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PROFILE_AVATAR_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("PRO").assertIsDisplayed()
    }

    @Test
    fun proUser_clickingButton_triggersOnClickCallback() {
        var clicked = false

        composeRule.setContent {
            SplitTripTheme {
                ProfileAvatarButton(
                    avatarUrl = null,
                    onClick = { clicked = true },
                    isPro = true
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PROFILE_AVATAR_BUTTON_TEST_TAG).performClick()

        assertTrue(clicked)
    }

    @Test
    fun compositionLocal_providesIsProFalseByDefault() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileAvatarButton(
                    avatarUrl = null,
                    onClick = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PROFILE_AVATAR_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun compositionLocal_providesIsProTrueWhenProvided() {
        composeRule.setContent {
            CompositionLocalProvider(LocalIsProUser provides true) {
                SplitTripTheme {
                    ProfileAvatarButton(
                        avatarUrl = null,
                        onClick = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PROFILE_AVATAR_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("PRO").assertIsDisplayed()
    }
}
