package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.LocalHazeState
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import org.koin.compose.getKoin

@Composable
fun FeatureScaffold(
    currentRoute: String,
    modifier: Modifier = Modifier,
    scrollContentUnderTopBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val hostTopPadding = LocalTopPadding.current
    val hostBottomPadding = LocalBottomPadding.current

    if (hostTopPadding > 0.dp) {
        TabHostedFeatureContent(
            hostTopPadding = hostTopPadding,
            hostBottomPadding = hostBottomPadding,
            scrollContentUnderTopBar = scrollContentUnderTopBar,
            modifier = modifier,
            content = content
        )
    } else {
        StandaloneFeatureScaffold(
            currentRoute = currentRoute,
            scrollContentUnderTopBar = scrollContentUnderTopBar,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
private fun TabHostedFeatureContent(
    hostTopPadding: Dp,
    hostBottomPadding: Dp,
    scrollContentUnderTopBar: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val navBarsBottomPadding = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateBottomPadding()
    val effectiveBottomPadding = if (hostBottomPadding > 0.dp) hostBottomPadding else navBarsBottomPadding
    val contentTopPadding = if (scrollContentUnderTopBar) 0.dp else hostTopPadding
    val providedTopPadding = if (scrollContentUnderTopBar) hostTopPadding else 0.dp

    // Inside MainScreen's tab Scaffold, the host already renders AnimatedTopBar and
    // provides LocalHazeState; re-apply only the effective host/system insets here.
    CompositionLocalProvider(
        LocalTopPadding provides providedTopPadding,
        LocalBottomPadding provides 0.dp
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(top = contentTopPadding, bottom = effectiveBottomPadding)
                .consumeWindowInsets(
                    PaddingValues(top = contentTopPadding, bottom = effectiveBottomPadding)
                )
        ) {
            content()
        }
    }
}

@Composable
private fun StandaloneFeatureScaffold(
    currentRoute: String,
    scrollContentUnderTopBar: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val koin = getKoin()
    val providers = remember(koin) { koin.getAll<ScreenUiProvider>() }
    val currentProvider = remember(currentRoute, providers) {
        providers.find { it.route == currentRoute }
    }

    val hazeState = remember { HazeState() }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                val topBar = currentProvider?.topBar
                if (topBar != null) {
                    topBar()
                } else {
                    Spacer(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        )
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime)
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val startPadding = innerPadding.calculateStartPadding(layoutDirection)
            val endPadding = innerPadding.calculateEndPadding(layoutDirection)
            val topPadding = innerPadding.calculateTopPadding()
            val bottomPadding = innerPadding.calculateBottomPadding()
            val contentTopPadding = if (scrollContentUnderTopBar) 0.dp else topPadding
            val providedTopPadding = if (scrollContentUnderTopBar) topPadding else 0.dp
            val appliedPadding = PaddingValues(
                start = startPadding,
                top = contentTopPadding,
                end = endPadding,
                bottom = bottomPadding
            )

            // Bottom inset is applied directly to the root content Box so every standalone
            // screen (including 3-button nav devices) clears the system bar; pass 0.dp via
            // LocalBottomPadding to avoid double-padding screens that also read it.
            CompositionLocalProvider(
                LocalTopPadding provides providedTopPadding,
                LocalBottomPadding provides 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState)
                        .padding(appliedPadding)
                        .consumeWindowInsets(appliedPadding)
                ) {
                    content()
                }
            }
        }
    }
}
