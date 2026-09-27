package es.pedrazamiguez.splittrip.core.designsystem.preview.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.PasswordRequirementsIndicator
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocales
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import es.pedrazamiguez.splittrip.domain.model.PasswordRequirementStatus

@PreviewLocales
@PreviewThemes
@Composable
private fun PasswordRequirementsIndicatorEmptyPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PasswordRequirementsIndicator(
                status = PasswordRequirementStatus()
            )
        }
    }
}

@PreviewLocales
@PreviewThemes
@Composable
private fun PasswordRequirementsIndicatorPartialPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PasswordRequirementsIndicator(
                status = PasswordRequirementStatus(
                    isMinLengthValid = true,
                    hasLowerCase = true
                )
            )
        }
    }
}

@PreviewLocales
@PreviewThemes
@Composable
private fun PasswordRequirementsIndicatorCompletePreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PasswordRequirementsIndicator(
                status = PasswordRequirementStatus(
                    isMinLengthValid = true,
                    hasUpperCase = true,
                    hasLowerCase = true,
                    hasDigit = true,
                    hasSpecialChar = true
                )
            )
        }
    }
}
