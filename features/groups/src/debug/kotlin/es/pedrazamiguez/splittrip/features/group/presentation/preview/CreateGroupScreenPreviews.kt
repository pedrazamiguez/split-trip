package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CreateEditGroupScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun CreateGroupScreenPreview() {
    PreviewThemeWrapper {
        CreateEditGroupScreen(
            uiState = CreateEditGroupUiState(
                isEditMode = false,
                availableCurrencies = persistentListOf(
                    CURRENCY_UI_EUR,
                    CURRENCY_UI_USD,
                    CURRENCY_UI_MXN
                )
            ),
            onScannerClick = {}
        )
    }
}

@PreviewComplete
@Composable
private fun EditGroupScreenPreview() {
    PreviewThemeWrapper {
        CreateEditGroupScreen(
            uiState = CreateEditGroupUiState(
                isEditMode = true,
                groupName = "Japan Trip",
                groupDescription = "Our trip to Japan",
                availableCurrencies = persistentListOf(
                    CURRENCY_UI_EUR,
                    CURRENCY_UI_USD,
                    CURRENCY_UI_MXN
                )
            ),
            onScannerClick = {}
        )
    }
}
