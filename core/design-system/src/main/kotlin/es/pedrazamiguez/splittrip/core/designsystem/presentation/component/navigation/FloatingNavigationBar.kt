package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.transition.fabSharedTransitionModifier

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
    mainAction: MainAction? = null,
    hazeState: HazeState? = null,
    applyWindowInsets: Boolean = true
) {
    val selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)

    // Lift the pill above the system navigation bar unless the parent already applies insets.
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NavBarDefaults.HorizontalPadding)
                .padding(bottom = NavBarDefaults.BottomPadding + navBarInset),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
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

            AnimatedVisibility(
                visible = mainAction != null,
                enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetX = { it }
                ) + expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = slideOutHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    ),
                    targetOffsetX = { it }
                ) + shrinkHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                if (mainAction != null) {
                    MainActionButton(mainAction = mainAction)
                }
            }
        }
    }
}

@Composable
private fun MainActionButton(
    mainAction: MainAction,
    modifier: Modifier = Modifier
) {
    val sharedModifier = mainAction.sharedTransitionKey?.let {
        fabSharedTransitionModifier(it)
    } ?: Modifier

    val startColor = MaterialTheme.colorScheme.primary
    val subtleEnd = lerp(
        startColor,
        MaterialTheme.colorScheme.primaryContainer,
        0.35f
    )

    val enabled = mainAction.enabled
    val containerColorModifier = if (enabled) {
        Modifier.background(
            brush = Brush.linearGradient(colors = listOf(startColor, subtleEnd)),
            shape = CircleShape
        )
    } else {
        Modifier.background(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            shape = CircleShape
        )
    }

    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(80.dp)
            .height(64.dp)
            .then(sharedModifier)
            .shadow(NavBarDefaults.ShadowElevation, CircleShape)
            .clip(CircleShape)
            .then(containerColorModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = contentColor),
                enabled = enabled,
                role = Role.Button,
                onClick = mainAction.onClick
            )
    ) {
        Icon(
            imageVector = mainAction.icon,
            contentDescription = mainAction.contentDescription,
            tint = contentColor
        )
    }
}
