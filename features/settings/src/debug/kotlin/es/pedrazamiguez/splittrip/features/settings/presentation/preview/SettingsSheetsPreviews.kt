package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.PasswordRequirementStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.component.LinkEmailBottomSheet
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@PreviewComplete
@Composable
fun LinkEmailBottomSheetPreview() {
    PreviewThemeWrapper {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            LinkEmailBottomSheet(
                uiState = AccountStatusUiState(
                    isAnonymous = true,
                    showLinkEmailSheet = true,
                    linkEmailInput = "user@example.com",
                    linkPasswordInput = "Secret123!",
                    linkConfirmPasswordInput = "Secret123!",
                    linkPasswordRequirementStatus = PasswordRequirementStatus(
                        isMinLengthValid = true,
                        hasUpperCase = true,
                        hasLowerCase = true,
                        hasDigit = true,
                        hasSpecialChar = true
                    )
                ),
                onEvent = {}
            )
        }
    }
}
