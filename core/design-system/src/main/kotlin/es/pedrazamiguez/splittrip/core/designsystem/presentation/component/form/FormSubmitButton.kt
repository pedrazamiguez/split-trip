package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding

/**
 * A reusable pinned submit button for forms.
 *
 * Always visible above the keyboard, adapts bottom padding based on keyboard
 * visibility and the bottom navigation bar height provided by [LocalBottomPadding].
 *
 * Delegates button rendering to [GradientButton], which provides the Horizon Narrative
 * gradient fill, full-pill shape, and loading state indicator.
 *
 * @param label The text shown on the button.
 * @param isEnabled Whether the button is in an interactive (enabled) state.
 *   Callers typically derive this from form-validity flags.
 * @param isLoading Whether an async operation is in-flight. Shows a spinner and
 *   disables the button while true.
 * @param onSubmit Called when the user taps the button (only fires when enabled).
 * @param modifier Optional modifier applied to the outer [Surface] container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormSubmitButton(
    label: String,
    isEnabled: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavPadding = LocalBottomPadding.current
    val isKeyboardVisible = WindowInsets.isImeVisible
    val effectiveBottomPadding = if (isKeyboardVisible) 12.dp else 12.dp + bottomNavPadding

    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        GradientButton(
            text = label,
            onClick = onSubmit,
            enabled = isEnabled,
            isLoading = isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.Large)
                .padding(top = MaterialTheme.spacing.Medium, bottom = effectiveBottomPadding)
        )
    }
}
