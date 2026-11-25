package es.pedrazamiguez.splittrip.core.designsystem.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

val LocalDarkTheme = compositionLocalOf<Boolean?> { null }

@Composable
fun isAppInDarkTheme(): Boolean = LocalDarkTheme.current ?: isSystemInDarkTheme()
