package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ReceiptViewerScreen
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [ReceiptViewerScreen].
 */
@RunWith(AndroidJUnit4::class)
class ReceiptViewerScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersReceiptViewer_andHandlesCloseClick() {
        var closeClicked = false
        val closeCd = context.getString(R.string.receipt_viewer_close_cd)
        val imageCd = context.getString(R.string.receipt_viewer_image_cd)

        composeRule.setContent {
            SplitTripTheme {
                ReceiptViewerScreen(
                    receiptUri = "android.resource://es.pedrazamiguez.splittrip/drawable/ic_launcher_foreground",
                    onClose = { closeClicked = true }
                )
            }
        }

        composeRule.waitForIdle()

        // Verify image and close button are displayed
        composeRule.onNodeWithContentDescription(imageCd).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(closeCd).assertIsDisplayed()

        // Perform click on close button
        composeRule.onNodeWithContentDescription(closeCd).performClick()
        composeRule.waitForIdle()

        assertTrue(closeClicked)
    }
}
