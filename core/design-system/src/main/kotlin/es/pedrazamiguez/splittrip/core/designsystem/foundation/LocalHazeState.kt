package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * CompositionLocal providing the ambient [HazeState] for glassmorphism effects.
 *
 * Scaffolds and entry orchestrators (e.g., [MainScreen], [FeatureScaffold]) provide this local
 * so that top app bars, navigation bars, and overlays can automatically coordinate their frosted-glass
 * blur with the content registered in `Modifier.hazeSource`.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }
