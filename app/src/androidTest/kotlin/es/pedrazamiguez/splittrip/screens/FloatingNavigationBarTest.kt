package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartArcs
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.NavigationBarIcon
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NAV_BAR_TAG = "floating_navigation_bar"
private const val NARROW_WIDTH_DP = 320
private const val LARGE_FONT_SCALE = 1.3f

private data class TestFloatingNavTab(
    override val id: String,
    val label: String,
    val icon: ImageVector
) : FloatingNavTab {

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) {
        NavigationBarIcon(
            icon = icon,
            contentDescription = label,
            isSelected = isSelected
        )
    }

    @Composable
    override fun getLabel(): String = label
}

private val threeMainTabs = listOf(
    TestFloatingNavTab("groups", "Groups", TablerIcons.Outline.UsersGroup),
    TestFloatingNavTab("balances", "Balances", TablerIcons.Outline.UsersGroup),
    TestFloatingNavTab("expenses", "Expenses", TablerIcons.Outline.UsersGroup)
)

/**
 * UI bounds and interaction tests for [FloatingNavigationBar] on narrow screens and scaled fonts.
 */
@RunWith(AndroidJUnit4::class)
class FloatingNavigationBarTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Test
    fun floatingNavigationBar_narrowWidthWithoutMainAction_rendersAllLabelsWithoutVerticalClipping() {
        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(NARROW_WIDTH_DP.dp)) {
                    FloatingNavigationBar(
                        modifier = Modifier.testTag(NAV_BAR_TAG),
                        selectedId = "groups",
                        items = threeMainTabs,
                        mainAction = null,
                        applyWindowInsets = false
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(NAV_BAR_TAG).getUnclippedBoundsInRoot()
        threeMainTabs.forEach { tab ->
            val node = composeRule.onNodeWithText(tab.label)
            node.assertIsDisplayed()
            val labelBounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                "Label '${tab.label}' top (${labelBounds.top}) should be >= root top (${rootBounds.top})",
                labelBounds.top >= rootBounds.top
            )
            assertTrue(
                "Label '${tab.label}' bottom (${labelBounds.bottom}) should be <= root bottom (${rootBounds.bottom})",
                labelBounds.bottom <= rootBounds.bottom
            )
        }
    }

    @Test
    fun floatingNavigationBar_narrowWidthWithMainAction_rendersAllLabelsWithoutVerticalClipping() {
        var selectedTabId: String? = null

        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(NARROW_WIDTH_DP.dp)) {
                    FloatingNavigationBar(
                        modifier = Modifier.testTag(NAV_BAR_TAG),
                        selectedId = "groups",
                        onTabSelected = { selectedTabId = it },
                        items = threeMainTabs,
                        mainAction = MainAction(
                            icon = TablerIcons.Outline.ChartArcs,
                            contentDescription = "Chart",
                            onClick = {}
                        ),
                        applyWindowInsets = false
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(NAV_BAR_TAG).getUnclippedBoundsInRoot()
        threeMainTabs.forEach { tab ->
            val node = composeRule.onNodeWithText(tab.label)
            node.assertIsDisplayed()
            val labelBounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                "Label '${tab.label}' top (${labelBounds.top}) should be >= root top (${rootBounds.top})",
                labelBounds.top >= rootBounds.top
            )
            assertTrue(
                "Label '${tab.label}' bottom (${labelBounds.bottom}) should be <= root bottom (${rootBounds.bottom})",
                labelBounds.bottom <= rootBounds.bottom
            )
        }

        composeRule.onNodeWithText("Balances").performClick()
        composeRule.waitForIdle()
        assertEquals("balances", selectedTabId)
    }

    @Test
    fun floatingNavigationBar_scaledFontWithMainAction_doesNotClipLabelsVertically() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = LARGE_FONT_SCALE
                )
            ) {
                SplitTripTheme {
                    Box(modifier = Modifier.width(NARROW_WIDTH_DP.dp)) {
                        FloatingNavigationBar(
                            modifier = Modifier.testTag(NAV_BAR_TAG),
                            selectedId = "balances",
                            items = threeMainTabs,
                            mainAction = MainAction(
                                icon = TablerIcons.Outline.ChartArcs,
                                contentDescription = "Chart",
                                onClick = {}
                            ),
                            applyWindowInsets = false
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(NAV_BAR_TAG).getUnclippedBoundsInRoot()
        threeMainTabs.forEach { tab ->
            val node = composeRule.onNodeWithText(tab.label)
            node.assertIsDisplayed()
            val labelBounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                "Label '${tab.label}' top (${labelBounds.top}) should be >= root top (${rootBounds.top})",
                labelBounds.top >= rootBounds.top
            )
            assertTrue(
                "Label '${tab.label}' bottom (${labelBounds.bottom}) should be <= root bottom (${rootBounds.bottom})",
                labelBounds.bottom <= rootBounds.bottom
            )
        }
    }
}
