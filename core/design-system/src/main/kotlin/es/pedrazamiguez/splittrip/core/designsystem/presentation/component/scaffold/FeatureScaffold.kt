package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import org.koin.compose.getKoin

@Composable
fun FeatureScaffold(currentRoute: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val koin = getKoin()
    val providers = remember(koin) { koin.getAll<ScreenUiProvider>() }

    val currentProvider = remember(
        currentRoute,
        providers
    ) {
        providers.find { it.route == currentRoute }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { currentProvider?.topBar?.invoke() }
    ) { innerPadding ->

        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            content()
        }
    }
}
