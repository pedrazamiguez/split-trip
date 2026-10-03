package es.pedrazamiguez.splittrip.core.designsystem.preview.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Eye
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Mail
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocales
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldDefaultPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "",
                onValueChange = {},
                label = stringResource(R.string.preview_label_group_name),
                placeholder = stringResource(R.string.preview_placeholder_enter_group_name)
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldFilledPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "Summer Trip 2026",
                onValueChange = {},
                label = stringResource(R.string.preview_label_group_name)
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldErrorPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "",
                onValueChange = {},
                label = stringResource(R.string.preview_label_group_name),
                isError = true,
                supportingText = stringResource(R.string.preview_error_group_name_required)
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldWithIconsPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "user@example.com",
                onValueChange = {},
                label = stringResource(R.string.preview_label_email),
                leadingIcon = {
                    Icon(
                        imageVector = TablerIcons.Outline.Mail,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = TablerIcons.Outline.Eye,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldDisabledPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = stringResource(R.string.preview_value_disabled),
                onValueChange = {},
                label = stringResource(R.string.preview_label_status),
                enabled = false
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldReadOnlyPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "EUR - Euro",
                onValueChange = {},
                label = stringResource(R.string.preview_label_main_currency),
                readOnly = true,
                onClick = {}
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldWithCounterPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "Short bio",
                onValueChange = {},
                label = "Bio",
                maxLength = 150
            )
        }
    }
}

@PreviewLocales
@Composable
private fun StyledOutlinedTextFieldWithCounterOverflowPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StyledOutlinedTextField(
                value = "A".repeat(155),
                onValueChange = {},
                label = "Bio",
                maxLength = 150,
                isError = true,
                supportingText = "Bio cannot exceed 150 characters"
            )
        }
    }
}
