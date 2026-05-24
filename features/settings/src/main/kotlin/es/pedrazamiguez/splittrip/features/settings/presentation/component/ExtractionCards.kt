package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCardPay
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CurrencyEuro
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShoppingBag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.features.settings.R

private val FIELD_ICON_SIZE = 18.dp

@Suppress("LongParameterList")
@Composable
fun ExtractionResultsCard(
    amount: String?,
    currency: String?,
    date: String?,
    time: String?,
    title: String?,
    vendor: String?,
    paymentMethod: String?,
    source: ExtractionSource?,
    confidence: ExtractionConfidence?,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.developer_services_extraction_results),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
            ExtractionFieldRow(
                icon = TablerIcons.Outline.Cash,
                label = stringResource(R.string.developer_services_extraction_amount),
                value = amount
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionFieldRow(
                icon = TablerIcons.Outline.CurrencyEuro,
                label = stringResource(R.string.developer_services_extraction_currency),
                value = currency
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionFieldRow(
                icon = TablerIcons.Outline.Calendar,
                label = stringResource(R.string.developer_services_extraction_date),
                value = date
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionFieldRow(
                icon = TablerIcons.Outline.Clock,
                label = stringResource(R.string.developer_services_extraction_time),
                value = time
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionFieldRow(
                icon = TablerIcons.Outline.Receipt,
                label = stringResource(R.string.developer_services_extraction_title),
                value = title
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionFieldRow(
                icon = TablerIcons.Outline.ShoppingBag,
                label = stringResource(R.string.developer_services_extraction_vendor),
                value = vendor
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionFieldRow(
                icon = TablerIcons.Outline.CreditCardPay,
                label = stringResource(R.string.developer_services_extraction_payment_method),
                value = paymentMethod
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionSourceRow(source = source)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionConfidenceRow(confidence = confidence)
        }
    }
}

@Composable
private fun ExtractionFieldRow(
    icon: ImageVector,
    label: String,
    value: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(FIELD_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value ?: stringResource(R.string.developer_services_extraction_na),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ExtractionSourceRow(
    source: ExtractionSource?,
    modifier: Modifier = Modifier
) {
    val (label, isAiCore) = resolveSourceLabel(source)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Receipt,
            contentDescription = null,
            modifier = Modifier.size(FIELD_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.developer_services_extraction_source),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isAiCore) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ExtractionConfidenceRow(
    confidence: ExtractionConfidence?,
    modifier: Modifier = Modifier
) {
    val (label, color) = resolveConfidenceLabel(confidence)
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

@Composable
private fun resolveSourceLabel(source: ExtractionSource?): Pair<String, Boolean> = when (source) {
    ExtractionSource.AI_CORE -> stringResource(R.string.developer_services_extraction_source_ai_core) to true
    else -> stringResource(R.string.developer_services_extraction_source_no_op) to false
}

@Composable
private fun resolveConfidenceLabel(
    confidence: ExtractionConfidence?
): Pair<String, androidx.compose.ui.graphics.Color> = when (confidence) {
    ExtractionConfidence.HIGH -> stringResource(R.string.developer_services_extraction_confidence_high) to
        MaterialTheme.colorScheme.tertiary
    ExtractionConfidence.MEDIUM -> stringResource(R.string.developer_services_extraction_confidence_medium) to
        MaterialTheme.colorScheme.secondary
    else -> stringResource(R.string.developer_services_extraction_confidence_low) to MaterialTheme.colorScheme.error
}
