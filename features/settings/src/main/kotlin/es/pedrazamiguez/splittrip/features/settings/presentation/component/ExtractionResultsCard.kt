package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Category
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
    category: String?,
    notes: String?,
    source: ExtractionSource?,
    confidence: ExtractionConfidence?,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.developer_services_extraction_results),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
            val fields = listOf(
                Triple(TablerIcons.Outline.Cash, stringResource(R.string.developer_services_extraction_amount), amount),
                Triple(
                    TablerIcons.Outline.CurrencyEuro,
                    stringResource(R.string.developer_services_extraction_currency),
                    currency
                ),
                Triple(TablerIcons.Outline.Calendar, stringResource(R.string.developer_services_extraction_date), date),
                Triple(TablerIcons.Outline.Clock, stringResource(R.string.developer_services_extraction_time), time),
                Triple(
                    TablerIcons.Outline.Receipt,
                    stringResource(R.string.developer_services_extraction_title),
                    title
                ),
                Triple(
                    TablerIcons.Outline.ShoppingBag,
                    stringResource(R.string.developer_services_extraction_vendor),
                    vendor
                ),
                Triple(
                    TablerIcons.Outline.CreditCardPay,
                    stringResource(R.string.developer_services_extraction_payment_method),
                    paymentMethod
                ),
                Triple(
                    TablerIcons.Outline.Category,
                    stringResource(R.string.developer_services_extraction_category),
                    category
                ),
                Triple(
                    TablerIcons.Outline.InfoCircle,
                    stringResource(R.string.developer_services_extraction_notes),
                    notes
                )
            )

            fields.forEach { (icon, label, value) ->
                ExtractionFieldRow(icon = icon, label = label, value = value)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            ExtractionSourceRow(source = source)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionConfidenceRow(confidence = confidence)
        }
    }
}
