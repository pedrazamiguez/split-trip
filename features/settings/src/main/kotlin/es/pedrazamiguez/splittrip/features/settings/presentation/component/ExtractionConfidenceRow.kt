package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.features.settings.R

private val FIELD_ICON_SIZE = 18.dp

@Composable
internal fun ExtractionConfidenceRow(
    confidence: ExtractionConfidence?,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (confidence) {
        ExtractionConfidence.HIGH -> stringResource(R.string.developer_services_extraction_confidence_high) to
            MaterialTheme.colorScheme.tertiary
        ExtractionConfidence.MEDIUM -> stringResource(R.string.developer_services_extraction_confidence_medium) to
            MaterialTheme.colorScheme.secondary
        else -> stringResource(R.string.developer_services_extraction_confidence_low) to MaterialTheme.colorScheme.error
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = TablerIcons.Outline.InfoCircle,
            contentDescription = null,
            modifier = Modifier.size(FIELD_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.developer_services_extraction_confidence),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
