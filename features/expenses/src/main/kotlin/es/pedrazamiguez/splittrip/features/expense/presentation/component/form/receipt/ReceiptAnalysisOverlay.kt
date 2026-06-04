package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Test tag for the receipt analysis overlay container, used in UI tests. */
const val RECEIPT_ANALYSIS_OVERLAY_TEST_TAG = "receipt_analysis_overlay"

/**
 * Full-screen blocking overlay shown while on-device AI extracts receipt fields.
 * Blocks back navigation and all pointer input until [visible] turns false.
 */
@Composable
fun ReceiptAnalysisOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(visible) {
        if (visible) {
            focusManager.clearFocus()
        }
    }

    if (visible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            ReceiptAnalysisContent(
                modifier = modifier
                    .fillMaxSize()
                    .testTag(RECEIPT_ANALYSIS_OVERLAY_TEST_TAG)
            )
        }
    }
}
