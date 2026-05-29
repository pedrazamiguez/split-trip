package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab

/**
 * A floating pill-shaped bottom navigation bar with Material 3 Expressive styling.
 *
 * Accepts any list of [FloatingNavTab] items, making it reusable across the app — both for
 * the main bottom tab bar (which uses `NavigationProvider : FloatingNavTab`)
 * and for in-screen navigation bars in non-tab features.
 *
 * Features:
 * - Floating pill shape with fully rounded corners
 * - Sliding indicator animating between items
 * - Bouncy, expressive animations on selection
 * - Elevated shadow for depth
 * - Optional translucent glassmorphism scrim via [hazeState]
 */
@Suppress("LongMethod") // Compose UI builder DSL
@Composable
fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    selectedId: String = "",
    onTabSelected: (String) -> Unit = {},
    items: List<FloatingNavTab> = emptyList(),
    hazeState: HazeState? = null,
    applyWindowInsets: Boolean = true
) {
    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    // Lift the pill above the system navigation bar regardless of navigation mode.
    val navBarInset = if (applyWindowInsets) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glassmorphism scrim — fade-to-blur effect at the bottom of the screen.
        if (hazeState != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NavBarDefaults.BarHeight + NavBarDefaults.BottomPadding + navBarInset + 32.dp)
                    .horizonGlassEffect(hazeState = hazeState) {
                        mask = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black, Color.Black),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    }
            )
        }

        // Floating pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NavBarDefaults.HorizontalPadding)
                .padding(bottom = NavBarDefaults.BottomPadding + navBarInset)
                .shadow(NavBarDefaults.ShadowElevation, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NavBarDefaults.BarHeight)
                        .padding(
                            horizontal = NavBarDefaults.InnerHorizontalPadding,
                            vertical = NavBarDefaults.InnerVerticalPadding
                        )
                ) {
                    SlidingIndicator(
                        selectedIndex = selectedIndex,
                        itemCount = items.size,
                        itemWidth = NavBarDefaults.ItemWidth,
                        containerWidth = maxWidth
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEachIndexed { index, item ->
                            FloatingNavItem(
                                item = item,
                                isSelected = index == selectedIndex,
                                onClick = { onTabSelected(item.id) },
                                modifier = Modifier.width(NavBarDefaults.ItemWidth)
                            )
                        }
                    }
                }
            }
        }
    }
}
