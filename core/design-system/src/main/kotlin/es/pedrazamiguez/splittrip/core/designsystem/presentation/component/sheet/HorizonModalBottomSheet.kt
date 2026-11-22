package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Remembers a [SheetState] configured for Horizon bottom sheets, omitting the
 * [SheetValue.PartiallyExpanded] anchor so sheets open directly to [SheetValue.Expanded].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberHorizonBottomSheetState(
    confirmValueChange: (SheetValue) -> Boolean = { true }
): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    confirmValueChange = confirmValueChange,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
)

/**
 * Standardized Horizon [ModalBottomSheet] wrapper with consistent defaults:
 * no drag handle, [MaterialTheme.colorScheme.surface] container color,
 * bottom-only [WindowInsets.safeDrawing] insets to prevent two-pass Dialog inset jumps,
 * and [rememberHorizonBottomSheetState] to open directly to [SheetValue.Expanded].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizonModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberHorizonBottomSheetState(),
    dragHandle: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentWindowInsets: @Composable () -> WindowInsets = {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        dragHandle = dragHandle,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}
