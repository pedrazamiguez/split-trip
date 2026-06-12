package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

private val DESTRUCTIVE_BUTTON_HEIGHT = 56.dp
private val DESTRUCTIVE_BUTTON_ELEVATION = 6.dp
private val LOADING_INDICATOR_SIZE = 24.dp
private val LOADING_INDICATOR_STROKE_WIDTH = 2.dp
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * A destructive-action button following the Horizon Narrative design spec.
 *
 * Uses full `error` / `onError` colours in light mode for maximum visual salience — light mode's
 * `errorContainer` (`#FFDAD6`) is too pale against the off-white page surface to convey urgency.
 * Dark mode retains `errorContainer` / `onErrorContainer` because the dark `errorContainer`
 * (`#93000A`) is already high-contrast against the near-black background.
 *
 * Shares the same pill shape, height, and Box-based shadow pattern as [GradientButton] and
 * [SecondaryButton].
 *
 * **Loading behaviour:** keeps the destructive styling visible while showing a spinner and
 * suppressing tap events.
 *
 * @param text         The label displayed on the button.
 * @param onClick      Called when the user taps the button.
 * @param modifier     Optional [Modifier] for width/padding. Height is managed internally.
 * @param enabled      Whether the button is interactive and visually enabled.
 * @param isLoading    Replaces the label with a spinner and suppresses interaction.
 * @param leadingIcon  Optional icon to the left of [text].
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val visualEnabled = enabled
    val interactable = enabled && !isLoading
    val isDarkTheme = isSystemInDarkTheme()

    val containerColor = when {
        !visualEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
        // Dark mode: errorContainer (#93000A) is already high-contrast; preserve existing behaviour.
        // Light mode: errorContainer (#FFDAD6) is too pale against the off-white surface.
        isDarkTheme -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.error
    }

    val contentColor = when {
        !visualEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        isDarkTheme -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onError
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(DESTRUCTIVE_BUTTON_HEIGHT)
            .shadow(
                elevation = if (visualEnabled) DESTRUCTIVE_BUTTON_ELEVATION else 0.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
            .background(color = containerColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                enabled = interactable,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.ExtraLarge),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(LOADING_INDICATOR_SIZE),
                    color = contentColor,
                    strokeWidth = LOADING_INDICATOR_STROKE_WIDTH
                )
            } else {
                ButtonContentRow(text, contentColor, leadingIcon)
            }
        }
    }
}
