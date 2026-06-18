package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
@Suppress("LongMethod", "CognitiveComplexMethod") // Compose UI builder DSL
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

    // Track the previously-selected tab index to compute slide direction for AnimatedContent.
    // Higher-index tab: icon enters from right; lower-index tab: icon enters from left.
    val previousSelectedIndex = remember { mutableIntStateOf(selectedIndex) }
    val slideDirection = if (selectedIndex >= previousSelectedIndex.intValue) 1 else -1
    SideEffect { previousSelectedIndex.intValue = selectedIndex }

    // Hoist shadow properties out of MainActionButton so the shadow wrapper can live OUTSIDE
    // AnimatedContent. AnimatedContent lifts composables into an offscreen GPU layer during
    // animation — identical to sharedBounds. Any shadow inside that layer is clipped to the
    // rectangular layer bounds regardless of clip = false. Keeping the shadow in the normal
    // render tree guarantees it is always rendered with the correct rounded shape.
    val isDarkModeForAction = isSystemInDarkTheme()
    val actionElevation = if (isDarkModeForAction) 0.dp else NavBarDefaults.ShadowElevation
    val actionButtonShape = RoundedCornerShape(NavBarDefaults.BarHeight / 2)

    val pillShape = RoundedCornerShape(NavBarDefaults.BarHeight / 2)

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

        val isDarkMode = isSystemInDarkTheme()
        val barElevation = if (isDarkMode) 0.dp else NavBarDefaults.ShadowElevation

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
                    .height(NavBarDefaults.BarHeight)
                    .graphicsLayer {
                        shadowElevation = barElevation.toPx()
                        shape = pillShape
                        clip = false
                    }
                    .animateContentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NavBarDefaults.BarHeight)
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    val innerHorizontalPadding = if (mainAction != null) 8.dp else NavBarDefaults.InnerHorizontalPadding
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
                                    horizontal = innerHorizontalPadding,
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
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
                ),
                exit = slideOutHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium
                    ),
                    targetOffsetX = { it }
                )
            ) {
                if (mainAction != null) {
                    // Shadow lives OUTSIDE AnimatedContent — stays in the normal render tree.
                    // AnimatedContent (like sharedBounds) lifts composables into an offscreen GPU
                    // compositing layer during animation. Any shadow inside that layer is clipped to
                    // the rectangular layer bounds, causing the squared-shadow artifact. Placing the
                    // shadow wrapper here ensures it is never lifted and always renders rounded.
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(64.dp)
                            .graphicsLayer {
                                shadowElevation = actionElevation.toPx()
                                shape = actionButtonShape
                                clip = false
                            }
                    ) {
                        // Direction-aware AnimatedContent fires on every mainAction change (tab switch).
                        // The outer AnimatedVisibility handles null ↔ non-null spring bounce separately.
                        AnimatedContent(
                            targetState = mainAction,
                            transitionSpec = {
                                slideInHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { fullWidth -> fullWidth * slideDirection } togetherWith
                                    slideOutHorizontally(
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) { fullWidth -> -fullWidth * slideDirection }
                            },
                            label = "MainActionIconTransition"
                        ) { targetAction ->
                            MainActionButton(mainAction = targetAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getActionButtonBackground(enabled: Boolean): Modifier {
    val startColor = MaterialTheme.colorScheme.primary
    val subtleEnd = lerp(
        startColor,
        MaterialTheme.colorScheme.primaryContainer,
        0.35f
    )
    return if (enabled) {
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
}

@Composable
private fun getActionButtonContentColor(enabled: Boolean): Color {
    return if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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

    val enabled = mainAction.enabled
    val containerColorModifier = getActionButtonBackground(enabled)
    val contentColor = getActionButtonContentColor(enabled)
    val buttonShape = RoundedCornerShape(NavBarDefaults.BarHeight / 2)

    // The outer Box retains graphicsLayer { clip = false } to preserve the three-layer
    // isolation pattern required for sharedBounds: the sharedBounds modifier must NOT be on
    // the same layer as the shadow. Shadow ownership has been lifted to the FloatingNavigationBar
    // call site (outside AnimatedContent) to prevent rectangular clipping during icon-slide
    // animations. See wiki/compose-shadow-and-clipping-guide.md §5.
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // shadowElevation intentionally 0: shadow is owned by the outer wrapper in
                // FloatingNavigationBar that lives outside the AnimatedContent compositing layer.
                shadowElevation = 0f
                shape = buttonShape
                clip = false
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(sharedModifier)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(buttonShape)
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
    }
}
