package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settings.presentation.component.LinkEmailDialog
import es.pedrazamiguez.splittrip.features.settings.presentation.component.PasswordResetConfirmDialog
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ReminderTimePickerDialog
import es.pedrazamiguez.splittrip.features.settings.presentation.component.TimezoneSelectionBottomSheet
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@PreviewComplete
@Composable
fun TimezoneSelectionBottomSheetPreview() {
    PreviewThemeWrapper {
        TimezoneSelectionBottomSheet(
            timezones = listOf("Europe/Madrid", "Europe/London", "America/New_York", "Asia/Tokyo"),
            onTimezoneSelected = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
fun LinkEmailDialogPreview() {
    PreviewThemeWrapper {
        LinkEmailDialog(
            uiState = AccountStatusUiState(
                linkEmailInput = "user@example.com",
                linkPasswordInput = "password123",
                linkConfirmPasswordInput = "password123"
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
fun PasswordResetConfirmDialogPreview() {
    PreviewThemeWrapper {
        PasswordResetConfirmDialog(
            email = "user@example.com",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
fun ReminderTimePickerDialogPreview() {
    PreviewThemeWrapper {
        ReminderTimePickerDialog(
            preferredReminderTime = "09:00",
            onTimeConfirm = { _, _ -> },
            onDismiss = {}
        )
    }
}
