package es.pedrazamiguez.splittrip.screens

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TOP_PILL_DISMISS_BUTTON_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TOP_PILL_MESSAGE_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TOP_PILL_NOTIFICATION_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillNotification
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.rememberTopPillController
import es.pedrazamiguez.splittrip.features.group.R as GroupsR
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val HOST_WIDTH = 320.dp
private const val MIN_MULTI_LINE_COUNT = 2
private const val MIN_LARGE_FONT_LINE_COUNT = 3
private const val LARGE_FONT_SCALE = 1.5f

/**
 * UI instrumentation tests for [TopPillNotification].
 */
@RunWith(AndroidJUnit4::class)
class TopPillNotificationTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersShortMessage_andDismissesOnIconClick() {
        lateinit var controller: TopPillController

        composeRule.setContent {
            controller = rememberTopPillController()
            LaunchedEffect(Unit) {
                controller.showPill("Group deleted")
            }
            SplitTripTheme {
                TopPillNotification(controller = controller)
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TOP_PILL_NOTIFICATION_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Group deleted").assertIsDisplayed()
        composeRule.onNodeWithTag(TOP_PILL_DISMISS_BUTTON_TEST_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(TOP_PILL_DISMISS_BUTTON_TEST_TAG).performClick()
        composeRule.waitForIdle()

        assertNull(controller.pillState.value)
        composeRule.onNodeWithTag(TOP_PILL_NOTIFICATION_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun rendersMultiLineMessageExceedingTwoLines_withoutVisualOverflow() {
        val longMessage = "Las cuentas gratuitas permiten 1 grupo. " +
            "Mejora a SplitTrip Pro para crear más grupos y añadir más compañeros de viaje."

        composeRule.setContent {
            val controller = rememberTopPillController()
            LaunchedEffect(Unit) {
                controller.showPill(longMessage)
            }
            ConstrainedPillHost(controller = controller)
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(longMessage).assertIsDisplayed()
        val layoutResult = fetchMessageLayoutResult()
        assertTrue(
            "Expected lineCount > $MIN_MULTI_LINE_COUNT but was ${layoutResult.lineCount}",
            layoutResult.lineCount > MIN_MULTI_LINE_COUNT
        )
        assertFalse(layoutResult.hasVisualOverflow)

        composeRule.onNodeWithTag(TOP_PILL_DISMISS_BUTTON_TEST_TAG).assertIsDisplayed()
        val pillBounds = fetchNodeBounds(TOP_PILL_NOTIFICATION_TEST_TAG)
        val dismissBounds = fetchNodeBounds(TOP_PILL_DISMISS_BUTTON_TEST_TAG)
        assertFitsInside(inner = dismissBounds, outer = pillBounds)
    }

    @Test
    fun rendersGroupLimitExceededString_inEnglishAndSpanish_withoutVisualOverflow() {
        val englishMessage = getLocalizedString(GroupsR.string.group_error_limit_groups_exceeded, "en")
        val spanishMessage = getLocalizedString(GroupsR.string.group_error_limit_groups_exceeded, "es")
        lateinit var controller: TopPillController

        composeRule.setContent {
            controller = rememberTopPillController()
            LaunchedEffect(Unit) {
                controller.showPill(englishMessage)
            }
            ConstrainedPillHost(controller = controller)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(englishMessage).assertIsDisplayed()
        assertFalse(fetchMessageLayoutResult().hasVisualOverflow)

        composeRule.runOnIdle {
            controller.showPill(spanishMessage)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(spanishMessage).assertIsDisplayed()
        assertFalse(fetchMessageLayoutResult().hasVisualOverflow)
    }

    @Test
    fun rendersMultiLineMessage_atLargeFontScale_withoutClippingOrTruncation() {
        val spanishMessage = getLocalizedString(GroupsR.string.group_error_limit_groups_exceeded, "es")

        composeRule.setContent {
            val controller = rememberTopPillController()
            LaunchedEffect(Unit) {
                controller.showPill(spanishMessage)
            }
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = currentDensity.density, fontScale = LARGE_FONT_SCALE)
            ) {
                ConstrainedPillHost(controller = controller)
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(spanishMessage).assertIsDisplayed()
        val layoutResult = fetchMessageLayoutResult()
        assertTrue(
            "Expected lineCount >= $MIN_LARGE_FONT_LINE_COUNT but was ${layoutResult.lineCount}",
            layoutResult.lineCount >= MIN_LARGE_FONT_LINE_COUNT
        )
        assertFalse(layoutResult.hasVisualOverflow)

        val pillBounds = fetchNodeBounds(TOP_PILL_NOTIFICATION_TEST_TAG)
        val messageBounds = fetchNodeBounds(TOP_PILL_MESSAGE_TEST_TAG)
        assertFitsInside(inner = messageBounds, outer = pillBounds)

        composeRule.onNodeWithTag(TOP_PILL_DISMISS_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Composable
    private fun ConstrainedPillHost(controller: TopPillController) {
        SplitTripTheme {
            Box(modifier = Modifier.width(HOST_WIDTH)) {
                TopPillNotification(controller = controller)
            }
        }
    }

    private fun fetchMessageLayoutResult(): TextLayoutResult {
        val node = composeRule.onNodeWithTag(TOP_PILL_MESSAGE_TEST_TAG).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun fetchNodeBounds(testTag: String): Rect =
        composeRule.onNodeWithTag(testTag).fetchSemanticsNode().boundsInRoot

    private fun assertFitsInside(inner: Rect, outer: Rect) {
        assertTrue("Expected left ${inner.left} >= ${outer.left}", inner.left >= outer.left)
        assertTrue("Expected top ${inner.top} >= ${outer.top}", inner.top >= outer.top)
        assertTrue("Expected right ${inner.right} <= ${outer.right}", inner.right <= outer.right)
        assertTrue("Expected bottom ${inner.bottom} <= ${outer.bottom}", inner.bottom <= outer.bottom)
    }

    private fun getLocalizedString(@StringRes resId: Int, languageTag: String): String {
        val config = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(languageTag))
        }
        return context.createConfigurationContext(config).getString(resId)
    }
}
