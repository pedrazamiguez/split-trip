package es.pedrazamiguez.splittrip.features.settings.presentation.component

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.ExtractionFieldItem
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo

@Suppress("LongParameterList")
@Composable
internal fun ExtractionResultsCard(
    amount: String?,
    currency: String?,
    date: String?,
    time: String?,
    title: String?,
    vendor: String?,
    paymentMethod: String?,
    category: String?,
    subcategory: String?,
    notes: String?,
    source: ExtractionSource?,
    confidence: ExtractionConfidence?,
    modifier: Modifier = Modifier,
    installedLocalAiPack: InstalledLocalAiPackInfo? = null
) {
    val context = LocalContext.current
    SectionCard(
        title = stringResource(R.string.developer_services_extraction_results),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
            val fields = buildExtractionFields(
                context = context,
                amount = amount,
                currency = currency,
                date = date,
                time = time,
                title = title,
                vendor = vendor,
                paymentMethod = paymentMethod,
                category = category,
                subcategory = subcategory,
                notes = notes
            )

            fields.forEach { (icon, label, value) ->
                ExtractionFieldRow(icon = icon, label = label, value = value)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            ExtractionSourceRow(
                source = source,
                installedLocalAiPack = installedLocalAiPack
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExtractionConfidenceRow(confidence = confidence)
        }
    }
}

@Suppress("LongParameterList")
private fun buildExtractionFields(
    context: Context,
    amount: String?,
    currency: String?,
    date: String?,
    time: String?,
    title: String?,
    vendor: String?,
    paymentMethod: String?,
    category: String?,
    subcategory: String?,
    notes: String?
): List<ExtractionFieldItem> = listOf(
    ExtractionFieldItem(
        TablerIcons.Outline.Cash,
        context.getString(R.string.developer_services_extraction_amount),
        amount
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.CurrencyEuro,
        context.getString(R.string.developer_services_extraction_currency),
        currency
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.Calendar,
        context.getString(R.string.developer_services_extraction_date),
        date
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.Clock,
        context.getString(R.string.developer_services_extraction_time),
        time
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.Receipt,
        context.getString(R.string.developer_services_extraction_title),
        title
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.ShoppingBag,
        context.getString(R.string.developer_services_extraction_vendor),
        vendor
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.CreditCardPay,
        context.getString(R.string.developer_services_extraction_payment_method),
        paymentMethod
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.Category,
        context.getString(R.string.developer_services_extraction_category),
        category
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.Sitemap,
        context.getString(R.string.developer_services_extraction_subcategory),
        subcategory
    ),
    ExtractionFieldItem(
        TablerIcons.Outline.InfoCircle,
        context.getString(R.string.developer_services_extraction_notes),
        notes
    )
)
