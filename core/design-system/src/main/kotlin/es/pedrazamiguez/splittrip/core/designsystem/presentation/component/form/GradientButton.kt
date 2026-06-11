package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

private val GRADIENT_BUTTON_HEIGHT = 56.dp
private val GRADIENT_BUTTON_ELEVATION = 8.dp
private val LOADING_INDICATOR_SIZE = 24.dp
private val LOADING_INDICATOR_STROKE_WIDTH = 2.dp

private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * How far the gradient end colour drifts from the start colour.
 *
 * The M3 token gap between `primary` (tone-80) and `primaryContainer` (tone-30)
 * in dark mode is ~50 tones, which produces a jarring gradient. By lerping the
 * end stop only [GRADIENT_BLEND_FRACTION] of the way toward [GradientButtonColors.gradientEnd],
 * the gradient stays subtle regardless of the theme's tone spread.
 *
 * `0.35f` ≈ one-third shift — enough to be perceptible, never harsh.
 */
private const val GRADIENT_BLEND_FRACTION = 0.35f

/**
 * Colour configuration for [GradientButton].
 *
 * Each instance defines the two gradient stops and the foreground (content) colour
 * for a specific tier (primary, secondary, tertiary, …).
 * Use the factory methods in [GradientButtonDefaults] to create instances that
 * derive from the active [MaterialTheme.colorScheme].
 *
 * @property gradientStart The start colour of the linear gradient background.
 * @property gradientEnd   The end colour of the linear gradient background.
 * @property contentColor  The foreground colour applied to text, icons, and the
 *                         loading indicator.
 */
@Immutable
data class GradientButtonColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val contentColor: Color
)

/**
 * Defaults and factory methods for [GradientButtonColors].
 */
object GradientButtonDefaults {

    /** Primary tier — `primary → primaryContainer`, white/onPrimary content. */
    @Composable
    fun primaryColors() = GradientButtonColors(
        gradientStart = MaterialTheme.colorScheme.primary,
        gradientEnd = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )

    /** Secondary tier — `secondary → secondaryContainer`, onSecondary content. */
    @Composable
    fun secondaryColors() = GradientButtonColors(
        gradientStart = MaterialTheme.colorScheme.secondary,
        gradientEnd = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondary
    )

    /** Tertiary tier — `tertiary → tertiaryContainer`, onTertiary content. */
    @Composable
    fun tertiaryColors() = GradientButtonColors(
        gradientStart = MaterialTheme.colorScheme.tertiary,
        gradientEnd = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiary
    )
}

/**
 * A gradient CTA button implementing the Horizon Narrative style (§5 Buttons).
 *
 * Renders a full-pill button with a linear gradient fill derived from [colors].
 * By default uses the primary tier (`primary → primaryContainer`) but callers
 * can switch to any colour tier via [GradientButtonDefaults] factory methods
 * (e.g. [GradientButtonDefaults.secondaryColors]).
 *
 * Built as a plain [Box] with [clickable] and [Modifier.shadow] — follows the same proven
 * shadow + clip + background pattern used by the bottom navigation bar. Intentionally avoids
 * Material `Button`/`Surface` composables whose internal `graphicsLayer` can occlude the
 * platform shadow.
 *
 * **Loading behaviour:** When [isLoading] is `true`, the button keeps its gradient
 * background and shadow but replaces the label with a [CircularProgressIndicator]
 * in [GradientButtonColors.contentColor] and suppresses tap events.
 *
 * @param text         The label displayed on the button.
 * @param onClick      Called when the user taps the button.
 * @param modifier     Optional [Modifier] for width/padding. Height is managed internally.
 * @param colors       Gradient and content colours. Defaults to [GradientButtonDefaults.primaryColors].
 * @param enabled      Whether the button is interactive and visually enabled.
 * @param isLoading    Replaces the label with a spinner and suppresses interaction.
 * @param leadingIcon  Optional icon to the left of [text].
 * @param trailingIcon Optional icon to the right of [text].
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: GradientButtonColors = GradientButtonDefaults.primaryColors(),
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val visualEnabled = enabled
    val interactable = enabled && !isLoading

    val subtleEnd = lerp(colors.gradientStart, colors.gradientEnd, GRADIENT_BLEND_FRACTION)

    val backgroundModifier = if (visualEnabled) {
        Modifier.background(
            brush = Brush.linearGradient(colors = listOf(colors.gradientStart, subtleEnd)),
            shape = CircleShape
        )
    } else {
        Modifier.background(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA),
            shape = CircleShape
        )
    }

    val contentColor = if (visualEnabled) {
        colors.contentColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(GRADIENT_BUTTON_HEIGHT)
            .shadow(
                elevation = if (visualEnabled) GRADIENT_BUTTON_ELEVATION else 0.dp,
                shape = CircleShape
            )
            .clip(CircleShape)
            .then(backgroundModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = colors.contentColor),
                enabled = interactable,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(LOADING_INDICATOR_SIZE),
                color = contentColor,
                strokeWidth = LOADING_INDICATOR_STROKE_WIDTH
            )
        } else {
            ButtonContentRow(text, contentColor, leadingIcon, trailingIcon)
        }
    }
}
