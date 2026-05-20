package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.features.settings.R

private const val URI_TAKE_LAST_LENGTH = 30
private const val URI_PAD_START_LENGTH = 35

@Composable
fun SelectedAttachmentCard(
    selectedFileUri: String?,
    selectedFileName: String,
    selectedFileMimeType: String,
    onSelectClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.developer_services_selected_attachment),
        modifier = modifier
    ) {
        if (selectedFileUri == null) {
            Text(
                text = stringResource(R.string.developer_services_no_document_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                AttachmentMetadataRow(
                    label = stringResource(R.string.developer_services_name),
                    value = selectedFileName
                )
                AttachmentMetadataRow(
                    label = stringResource(R.string.developer_services_type),
                    value = selectedFileMimeType
                )
                AttachmentMetadataRow(
                    label = stringResource(R.string.developer_services_uri),
                    value = selectedFileUri.takeLast(URI_TAKE_LAST_LENGTH)
                        .padStart(URI_PAD_START_LENGTH, '.'),
                    isUri = true
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
        ) {
            SecondaryButton(
                text = stringResource(R.string.developer_services_select_document),
                onClick = onSelectClick,
                modifier = Modifier.weight(1f)
            )

            if (selectedFileUri != null) {
                SecondaryButton(
                    text = stringResource(R.string.developer_services_clear),
                    onClick = onClearClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttachmentMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isUri: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isUri) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (isUri) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
    }
}
