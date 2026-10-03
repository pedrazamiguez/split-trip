package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal providing the top padding value for screens inside a floating or translucent top bar layout.
 * This allows scrollable content (LazyColumn, LazyGrid) to flow behind the top app bar while keeping the initial
 * content visible below the top bar at rest.
 */
val LocalTopPadding = compositionLocalOf<Dp> { 0.dp }
