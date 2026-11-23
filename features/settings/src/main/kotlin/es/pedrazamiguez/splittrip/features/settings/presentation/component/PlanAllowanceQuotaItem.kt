package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReportAnalytics
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.AnimatedProgressBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceUsageStatus

private val QUOTA_ICON_SIZE = 20.dp

@Composable
internal fun PlanAllowanceQuotaItem(
    quota: AllowanceQuotaUiModel,
    modifier: Modifier = Modifier
) {
    val icon = when (quota.type) {
        AllowanceQuotaType.AI_RECEIPT_SCANS -> TablerIcons.Outline.PhotoAi
        AllowanceQuotaType.MEDIA_UPLOADS -> TablerIcons.Outline.Photo
        AllowanceQuotaType.REPORT_EXPORTS -> TablerIcons.Outline.ReportAnalytics
        AllowanceQuotaType.OWNED_GROUPS -> TablerIcons.Outline.UsersGroup
    }

    val tagStyle = when (quota.status) {
        AllowanceUsageStatus.HEALTHY -> PassportTagStyle.SUCCESS
        AllowanceUsageStatus.APPROACHING_LIMIT -> PassportTagStyle.WARNING
        AllowanceUsageStatus.EXHAUSTED -> PassportTagStyle.ERROR
        AllowanceUsageStatus.UNLIMITED -> PassportTagStyle.INFO
    }

    val barColor = when (quota.status) {
        AllowanceUsageStatus.HEALTHY -> MaterialTheme.colorScheme.secondary
        AllowanceUsageStatus.APPROACHING_LIMIT -> MaterialTheme.colorScheme.tertiary
        AllowanceUsageStatus.EXHAUSTED -> MaterialTheme.colorScheme.error
        AllowanceUsageStatus.UNLIMITED -> MaterialTheme.colorScheme.secondary
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(QUOTA_ICON_SIZE)
            )
            Column(modifier = Modifier.weight(1f)) {
                CardTitleText(text = quota.title.asString())
                SecondaryBodyText(text = quota.subtitle.asString(), maxLines = 2)
            }
            PassportTag(
                text = quota.statusBadge.asString(),
                style = tagStyle
            )
        }

        LabelText(text = quota.usageText.asString())

        if (quota.showProgressBar) {
            AnimatedProgressBar(
                progress = quota.progressFraction,
                fillColor = barColor,
                animationKey = quota.type
            )
        }
    }
}
