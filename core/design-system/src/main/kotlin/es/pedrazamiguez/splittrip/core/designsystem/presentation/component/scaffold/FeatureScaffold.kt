package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
                if (currentProvider?.topBar != null) {
                    currentProvider.topBar!!.invoke()
                } else {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            val topPadding = innerPadding.calculateTopPadding()
            val bottomPadding = innerPadding.calculateBottomPadding()

            if (scrollContentUnderTopBar) {
                CompositionLocalProvider(
                    LocalTopPadding provides topPadding,
                    LocalBottomPadding provides bottomPadding
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState)
                            .padding(bottom = bottomPadding)
                    ) {
                        content()
                    }
                }
            } else {
                CompositionLocalProvider(
                    LocalTopPadding provides 0.dp,
                    LocalBottomPadding provides bottomPadding
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState)
                            .padding(top = topPadding, bottom = bottomPadding)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
