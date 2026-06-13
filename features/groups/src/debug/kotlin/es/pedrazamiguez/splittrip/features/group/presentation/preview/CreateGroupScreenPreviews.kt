package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CreateGroupScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun CreateGroupScreenPreview() {
    PreviewThemeWrapper {
        CreateGroupScreen(
            uiState = CreateGroupUiState(
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
