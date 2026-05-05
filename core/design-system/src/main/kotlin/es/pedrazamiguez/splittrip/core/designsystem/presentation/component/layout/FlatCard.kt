package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val GHOST_BORDER_ALPHA_LIGHT = 0.15f
private const val GHOST_BORDER_ALPHA_DARK = 0.22f
private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f

/**
 * A flat card container with zero elevation that achieves visual depth through
 * tonal layering rather than explicit borders (Horizon Narrative §4.1 "No-Line Rule").
 *
 * The default [color] is `surfaceContainerLow`, which sits naturally **darker / more
 * tinted** than the off-white page background (`surface` / `background` token) — no
 * stroke needed (Horizon Narrative §4.2 "Layering Principle"). Cards feel grounded
 * and slightly inset rather than floating as bright white panels.
 *
 * Use this as the standard card wrapper across the entire app to guarantee
 * visual consistency. Override [shape] or [color] only when the design
 * explicitly calls for a variation (e.g., `shapes.medium` for nested cards,
 * or `primaryContainer` for a selected state).
 *
 * ## Ambient Shadow (Hero / Featured Cards)
 *
 * For hero cards that need to visually "float" above the list, pass a non-zero
 * [elevation]. The shadow is rendered via `Modifier.graphicsLayer` on an outer `Box`
 * that is **always present** in the composition tree, regardless of the current
 * elevation value. This stable tree structure prevents the squared-shadow artifact
 * that occurs when a conditional `if/else` branch destroys and recreates the inner
 * `Surface` node as elevation animates across `0.dp` — losing the shape-clip context
 * mid-animation and briefly rendering the shadow as a hard rectangle.
 *
 * The [modifier] (layout, clip, clickable) is forwarded to the inner `Surface`, not
 * to the outer wrapper, so the ripple clip context is correctly isolated from the
 * shadow layer.
 *
 * Per Horizon Narrative §4.4 "Ambient Shadows", the shadow is **silently suppressed
 * in dark mode** — tonal layering takes over and the [elevation] value is ignored.
 * The caller never needs to gate on `isSystemInDarkTheme()`.
 *
 * For hero cards inside a `LazyColumn` that use `animateItem()`, **always pass
 * `fadeInSpec = null, fadeOutSpec = null`** to `animateItem()`. The default alpha
 * fade-in/out creates a rectangular offscreen hardware buffer (Android alpha compositing
 * layer). `FlatCard`'s `graphicsLayer { clip = false }` shadow bleeds outside its own
 * bounds, but that bleed is silently clipped by the rectangular buffer edge — producing
 * a hard squared-shadow artifact. Disabling the alpha animations eliminates the buffer;
 * the spring placement animation is retained and unaffected.
 *
 * @param modifier    Applied to the inner [Surface] in all cases. Includes layout,
 *                    clip, and click modifiers.
 * @param shape       Card corner shape. Defaults to `MaterialTheme.shapes.large`.
 * @param color       Background color. Defaults to `surfaceContainerLow`
 *                    (Layering Principle inset tier — slightly tinted relative to
 *                    the off-white page background in light mode; lighter than the
 *                    near-black background in dark mode).
 * @param ghostBorder When `true`, draws an `outlineVariant` border at reduced opacity
 *                    (15% light / 22% dark). Reserved for edge cases where two adjacent
 *                    identical-colour surfaces make tonal contrast alone insufficient —
 *                    typically dark-mode scenarios. Defaults to `false`.
 * @param elevation   Ambient shadow elevation. Defaults to `0.dp` (flat — no breaking
 *                    change to existing callers). When `> 0.dp`, an ultra-diffused
 *                    ambient shadow is rendered in light mode only (Horizon Narrative
 *                    §4.4). In dark mode this value is always treated as `0.dp`.
 * @param content     The card content slot.
 */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    ghostBorder: Boolean = false,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD

    val border = if (ghostBorder) {
        val alpha = if (isDark) GHOST_BORDER_ALPHA_DARK else GHOST_BORDER_ALPHA_LIGHT
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
    } else {
        null
    }

    // §4.4: Ambient shadows are invisible in dark mode — tonal layering takes over.
    val effectiveElevation = if (isDark) 0.dp else elevation

    // The outer Box is ALWAYS present regardless of effectiveElevation — this is intentional.
    //
    // A conditional `if (effectiveElevation > 0.dp) Box + Surface else Surface` would
    // destroy and recreate the inner Surface node every time elevation crosses 0.dp during
    // animation. When that structural change occurs mid-flight (sharedBounds transition or
    // animateItem() placement), the Surface loses its shape-clip context for one frame and
    // the shadow renders as a hard rectangle behind the card (the squared artifact).
    //
    // Using Modifier.graphicsLayer keeps the tree stable: the lambda updates on each draw
    // frame without touching the composition tree, so the shadow transitions smoothly and
    // the shape-clip is never lost. `clip = false` lets the shadow bleed outside the Box
    // bounds (§4.4 ambient diffusion); the inner Surface + its own shape handle content
    // clipping independently.
    Box(
        modifier = Modifier.graphicsLayer {
            shadowElevation = effectiveElevation.toPx()
            this.shape = shape
            clip = false
        }
    ) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            border = border,
            content = content
        )
    }
}
