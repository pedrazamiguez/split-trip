package es.pedrazamiguez.splittrip.core.designsystem.presentation.screen

import androidx.compose.runtime.Composable

interface ScreenUiProvider {

    val route: String

    val topBar: (@Composable () -> Unit)?
        get() = null

    val mainAction: MainAction?
        @Composable
        get() = null
}
