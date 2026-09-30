package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeBlurStyleScope
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * Design-system constants and utilities for the **Horizon Narrative Glassmorphism** recipe.
 *
 * The Horizon Narrative uses a glass-blur effect for floating UI elements (navigation bars,
 * top bars, modal sheets) to evoke the atmospheric quality of travel — clouds, water, horizons.
 *
 * ## Recipes (§2 Light / §7 Dark — issue #877)
 *
 * | Mode  | Surface opacity | Blur radius |
 * |-------|-----------------|-------------|
 * | Light | 70 %            | 20 dp       |
 * | Dark  | 60 %            | 24 dp       |
 *
 * The slightly higher blur radius in dark mode compensates for the lower contrast between
 * surface hierarchy levels on dark backgrounds.
 *
 * ## Usage
 *
 * ```kotlin
 * // Any floating element in the app
 * Box(
 *     modifier = Modifier.horizonGlassEffect(hazeState = hazeState)
 * )
 *
 * // Bottom navigation bar — adds a layout-specific gradient mask on top of the base recipe
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .height(barHeight)
 *         .horizonGlassEffect(hazeState = hazeState) {
 *             mask(
 *                 Brush.verticalGradient(
 *                     colors = listOf(Color.Transparent, Color.Black, Color.Black),
 *                 )
 *             )
 *         }
 * )
 * ```
 *
 * @see horizonGlassEffect
 */
internal object GlassmorphismDefaults {

    /**
     * Light-mode tint: [HorizonSurface] at **70 % opacity**.
     *
     * Applied on top of the blurred content as a translucent surface wash.
     * Matches the Horizon Narrative Glass & Gradient rule (§2).
     */
    val LightTint: HazeColorEffect = HazeColorEffect.tint(HorizonSurface.copy(alpha = 0.70f))

    /**
     * Dark-mode tint: [HorizonSurfaceDark] at **60 % opacity**.
     *
     * A slightly lower opacity than light mode is intentional: the deeper blur radius (§7)
     * already provides enough visual separation between layers on dark surfaces.
     * Matches the Horizon Narrative Glass & Gradient rule (§7).
     */
    val DarkTint: HazeColorEffect = HazeColorEffect.tint(HorizonSurfaceDark.copy(alpha = 0.60f))

    /**
     * Light-mode backdrop blur radius: **20 dp**.
     *
     * Matches the Horizon Narrative Glass & Gradient rule (§2).
     */
    val LightBlurRadius: Dp = 20.dp

    /**
     * Dark-mode backdrop blur radius: **24 dp**.
     *
     * The extra 4 dp compensates for the reduced contrast between surface hierarchy levels
     * on dark backgrounds, ensuring a perceptibly distinct frosted-glass appearance (§7).
     */
    val DarkBlurRadius: Dp = 24.dp

    /**
     * Light-mode blur style combining [LightBlurRadius] and [LightTint].
     */
    val LightStyle: HazeBlurStyle = HazeBlurStyle {
        blurRadius(LightBlurRadius)
        colorEffects(listOf(LightTint))
    }

    /**
     * Dark-mode blur style combining [DarkBlurRadius] and [DarkTint].
     */
    val DarkStyle: HazeBlurStyle = HazeBlurStyle {
        blurRadius(DarkBlurRadius)
        colorEffects(listOf(DarkTint))
    }
}

/**
 * Applies the **Horizon Narrative glassmorphism recipe** to the receiver [Modifier].
 *
 * Selects blur radius and surface tint automatically based on [darkTheme]:
 * - **Light mode:** [GlassmorphismDefaults.LightStyle] ([GlassmorphismDefaults.LightTint] + [GlassmorphismDefaults.LightBlurRadius])
 * - **Dark mode:** [GlassmorphismDefaults.DarkStyle] ([GlassmorphismDefaults.DarkTint] + [GlassmorphismDefaults.DarkBlurRadius])
 *
 * An optional [block] parameter exposes the full [HazeBlurStyleScope] for callers that need
 * layout-specific customisation (e.g. a gradient [HazeBlurStyleScope.mask] for the bottom bar's
 * fade-in scrim) without requiring a separate `hazeBlur` call.
 *
 * @param hazeState The [HazeState] shared with the `hazeSource` content that sits behind
 *   this floating element. Must be created at the common ancestor composable.
 * @param darkTheme Whether to apply the dark-mode recipe. Defaults to [isSystemInDarkTheme].
 * @param block Optional lambda on [HazeBlurStyleScope] for additional per-site customisation
 *   (e.g. [HazeBlurStyleScope.mask]).
 * @return A [Modifier] with the glassmorphism blur effect applied.
 *
 * @see GlassmorphismDefaults
 */
@Composable
fun Modifier.horizonGlassEffect(
    hazeState: HazeState,
    darkTheme: Boolean = isSystemInDarkTheme(),
    block: (HazeBlurStyleScope.() -> Unit)? = null
): Modifier {
    val baseStyle = if (darkTheme) GlassmorphismDefaults.DarkStyle else GlassmorphismDefaults.LightStyle
    val finalStyle = if (block != null) baseStyle.then(block) else baseStyle
    return hazeBlur(
        input = HazeInput.Sources(hazeState),
        style = finalStyle
    )
}
