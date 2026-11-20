package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewBottomSheet
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

@Suppress("LongParameterList")
@Composable
fun CreateEditGroupOverlays(
    state: CreateEditGroupUiState,
    showScanner: Boolean,
    showExitConfirmation: Boolean,
    onDismissScanner: () -> Unit,
    onMemberScanned: (userId: String, email: String) -> Unit,
    onConfirmExit: () -> Unit,
    onDismissExit: () -> Unit,
    onUpgrade: () -> Unit,
    onDismissUpgrade: () -> Unit
) {
    if (showScanner) {
        QrScannerDialog(
            onDismissRequest = onDismissScanner,
            onScanned = { payload -> onMemberScanned(payload.userId, payload.email) }
        )
    }

    if (showExitConfirmation) {
        DestructiveConfirmationDialog(
            title = stringResource(DesignSystemR.string.wizard_exit_dialog_title),
            text = stringResource(DesignSystemR.string.wizard_exit_dialog_message),
            confirmLabel = stringResource(DesignSystemR.string.wizard_exit_dialog_confirm),
            onConfirm = onConfirmExit,
            onDismiss = onDismissExit
        )
    }

    state.proPreviewPrompt?.let { prompt ->
        ProFeaturePreviewBottomSheet(
            previewType = prompt.previewType,
            onUpgrade = onUpgrade,
            onDismiss = onDismissUpgrade
        )
    }
}
