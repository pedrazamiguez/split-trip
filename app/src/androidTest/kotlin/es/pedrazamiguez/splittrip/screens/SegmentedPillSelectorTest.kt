package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelectorDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SELECTOR_TAG = "segmented_pill_selector"
private const val NARROW_WIDTH_DP = 320
private const val LARGE_FONT_SCALE = 1.3f

private val englishSplitOptions = persistentListOf("Equal", "Exact", "Percent")
private val spanishSplitOptions = persistentListOf("Partes iguales", "Cantidades", "Porcentaje")

/**
 * UI semantics, interaction, and narrow-width bounds tests for [SegmentedPillSelector].
 */
@RunWith(AndroidJUnit4::class)
class SegmentedPillSelectorTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Test
    fun segmentedPillSelector_rendersAllOptionsWithCorrectSelectionSemantics() {
        composeRule.setContent {
            SplitTripTheme {
                SegmentedPillSelector(
                    options = englishSplitOptions,
                    selectedOption = "Equal",
                    onOptionSelected = {},
                    optionLabel = { it },
                    modifier = Modifier.testTag(SELECTOR_TAG)
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Equal").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Exact").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("Percent").assertIsDisplayed().assertIsNotSelected()
    }

    @Test
    fun segmentedPillSelector_clickingUnselectedOption_updatesSelectionAndInvokesCallback() {
        var callbackSelection: String? = null

        composeRule.setContent {
            SplitTripTheme {
                var selected by remember { mutableStateOf("Equal") }
                SegmentedPillSelector(
                    options = englishSplitOptions,
                    selectedOption = selected,
                    onOptionSelected = { option ->
                        selected = option
                        callbackSelection = option
                    },
                    optionLabel = { it },
                    modifier = Modifier.testTag(SELECTOR_TAG)
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Percent").performClick()
        composeRule.waitForIdle()

        assertEquals("Percent", callbackSelection)
        composeRule.onNodeWithText("Percent").assertIsSelected()
        composeRule.onNodeWithText("Equal").assertIsNotSelected()
    }

    @Test
    fun segmentedPillSelector_disabled_doesNotInvokeCallbackOnClick() {
        var callbackCount = 0

        composeRule.setContent {
            SplitTripTheme {
                SegmentedPillSelector(
                    options = englishSplitOptions,
                    selectedOption = "Equal",
                    onOptionSelected = { callbackCount += 1 },
                    optionLabel = { it },
                    enabled = false,
                    modifier = Modifier.testTag(SELECTOR_TAG)
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Exact").performClick()
        composeRule.waitForIdle()

        assertEquals(0, callbackCount)
        composeRule.onNodeWithText("Equal").assertIsSelected()
    }

    @Test
    fun segmentedPillSelector_nullSelection_rendersOptionsWithNoneSelected() {
        composeRule.setContent {
            SplitTripTheme {
                SegmentedPillSelector(
                    options = englishSplitOptions,
                    selectedOption = null,
                    onOptionSelected = {},
                    optionLabel = { it },
                    modifier = Modifier.testTag(SELECTOR_TAG)
                )
            }
        }

        composeRule.waitForIdle()

        englishSplitOptions.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed().assertIsNotSelected()
        }
    }

    @Test
    fun segmentedPillSelector_emptyOptions_rendersWithoutCrash() {
        composeRule.setContent {
            SplitTripTheme {
                SegmentedPillSelector(
                    options = persistentListOf<String>(),
                    selectedOption = null,
                    onOptionSelected = {},
                    optionLabel = { it },
                    modifier = Modifier.testTag(SELECTOR_TAG)
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SELECTOR_TAG).assertDoesNotExist()
    }

    @Test
    fun segmentedPillSelector_narrowWidthWithSpanishLabelsInFlatCard_rendersWithoutClipping() {
        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(NARROW_WIDTH_DP.dp)) {
                    FlatCard(
                        modifier = Modifier.fillMaxWidth(),
                        ghostBorder = true
                    ) {
                        SegmentedPillSelector(
                            options = spanishSplitOptions,
                            selectedOption = "Partes iguales",
                            onOptionSelected = {},
                            optionLabel = { it },
                            colors = SegmentedPillSelectorDefaults.nestedColors(),
                            modifier = Modifier
                                .padding(12.dp)
                                .testTag(SELECTOR_TAG)
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(SELECTOR_TAG).getUnclippedBoundsInRoot()
        spanishSplitOptions.forEach { label ->
            val node = composeRule.onNodeWithText(label)
            node.assertIsDisplayed()
            val labelBounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                "Label '$label' top (${labelBounds.top}) should be >= root top (${rootBounds.top})",
                labelBounds.top >= rootBounds.top
            )
            assertTrue(
                "Label '$label' bottom (${labelBounds.bottom}) should be <= root bottom (${rootBounds.bottom})",
                labelBounds.bottom <= rootBounds.bottom
            )
            assertTrue(
                "Label '$label' left (${labelBounds.left}) should be >= root left (${rootBounds.left})",
                labelBounds.left >= rootBounds.left
            )
            assertTrue(
                "Label '$label' right (${labelBounds.right}) should be <= root right (${rootBounds.right})",
                labelBounds.right <= rootBounds.right
            )
        }
    }

    @Test
    fun segmentedPillSelector_narrowWidthScaledFont_doesNotClipLabelsVertically() {
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
                        SegmentedPillSelector(
                            options = spanishSplitOptions,
                            selectedOption = "Cantidades",
                            onOptionSelected = {},
                            optionLabel = { it },
                            modifier = Modifier.testTag(SELECTOR_TAG)
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(SELECTOR_TAG).getUnclippedBoundsInRoot()
        spanishSplitOptions.forEach { label ->
            val node = composeRule.onNodeWithText(label)
            node.assertIsDisplayed()
            val labelBounds = node.getUnclippedBoundsInRoot()
            assertTrue(
                "Label '$label' top (${labelBounds.top}) should be >= root top (${rootBounds.top})",
                labelBounds.top >= rootBounds.top
            )
            assertTrue(
                "Label '$label' bottom (${labelBounds.bottom}) should be <= root bottom (${rootBounds.bottom})",
                labelBounds.bottom <= rootBounds.bottom
            )
        }
    }
}
