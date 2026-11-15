package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet

data class FundingSourceUiModel(
    val id: String,
    val displayText: String,
    val description: String = "",
    val icon: ImageVector = TablerIcons.Outline.Wallet
)
