package es.pedrazamiguez.splittrip.core.designsystem.presentation.screen

import androidx.compose.ui.graphics.vector.ImageVector

data class MainAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val sharedTransitionKey: String? = null
)
