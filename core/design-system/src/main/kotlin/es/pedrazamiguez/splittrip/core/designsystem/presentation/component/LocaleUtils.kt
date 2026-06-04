package es.pedrazamiguez.splittrip.core.designsystem.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun rememberLocale(): Locale {
    val configuration = LocalConfiguration.current
    val locales = configuration.locales
    return remember(locales) {
        if (locales.isEmpty) {
            Locale.getDefault()
        } else {
            locales[0] ?: Locale.getDefault()
        }
    }
}
