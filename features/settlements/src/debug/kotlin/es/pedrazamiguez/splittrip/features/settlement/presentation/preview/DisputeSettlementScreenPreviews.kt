package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.DisputeSettlementScreen
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.DisputeSettlementUiState

@PreviewComplete
@Composable
private fun DisputeSettlementScreenEmptyPreview() {
    PreviewThemeWrapper {
        DisputeSettlementScreen(
            uiState = DisputeSettlementUiState(),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DisputeSettlementScreenFilledPreview() {
    PreviewThemeWrapper {
        DisputeSettlementScreen(
            uiState = DisputeSettlementUiState(
                reason = "I already paid this in cash yesterday."
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DisputeSettlementScreenOfflinePreview() {
    PreviewThemeWrapper {
        DisputeSettlementScreen(
            uiState = DisputeSettlementUiState(
                reason = "Wrong amount",
                isOffline = true
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DisputeSettlementScreenWithAttachmentPreview() {
    PreviewThemeWrapper {
        DisputeSettlementScreen(
            uiState = DisputeSettlementUiState(
                reason = "Taxi fare was split evenly at the airport."
            ),
            onEvent = {},
            attachmentContent = {
                Text(text = "Attachment slot preview")
            }
        )
    }
}
