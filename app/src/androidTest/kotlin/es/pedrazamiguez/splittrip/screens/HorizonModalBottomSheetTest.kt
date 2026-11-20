package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.HorizonModalBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.rememberHorizonBottomSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [HorizonModalBottomSheet] and [rememberHorizonBottomSheetState].
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class HorizonModalBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun horizonModalBottomSheet_opensDirectlyInExpandedState_withoutPartiallyExpanded() {
        lateinit var capturedSheetState: SheetState

        composeRule.setContent {
            SplitTripTheme {
                capturedSheetState = rememberHorizonBottomSheetState()
                HorizonModalBottomSheet(
                    onDismissRequest = {},
                    sheetState = capturedSheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        repeat(30) { index ->
                            Text(text = "Line $index")
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        assertEquals(SheetValue.Expanded, capturedSheetState.currentValue)
        assertEquals(SheetValue.Expanded, capturedSheetState.targetValue)
    }

    @Test
    fun horizonModalBottomSheet_rendersContent_andInvokesOnDismissRequestOnHide() {
        lateinit var capturedSheetState: SheetState
        lateinit var capturedScope: CoroutineScope

        composeRule.setContent {
            SplitTripTheme {
                capturedSheetState = rememberHorizonBottomSheetState()
                capturedScope = rememberCoroutineScope()
                HorizonModalBottomSheet(
                    onDismissRequest = {},
                    sheetState = capturedSheetState
                ) {
                    Text(text = "Sheet Content")
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sheet Content").assertIsDisplayed()

        composeRule.runOnIdle {
            capturedScope.launch {
                capturedSheetState.hide()
            }
        }
        composeRule.waitForIdle()

        assertEquals(SheetValue.Hidden, capturedSheetState.currentValue)
        assertFalse(capturedSheetState.isVisible)
    }

    @Test
    fun horizonModalBottomSheet_respectsCustomContentWindowInsets() {
        composeRule.setContent {
            SplitTripTheme {
                HorizonModalBottomSheet(
                    onDismissRequest = {},
                    contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                ) {
                    Text(text = "Custom Insets Content")
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Custom Insets Content").assertIsDisplayed()
    }
}
