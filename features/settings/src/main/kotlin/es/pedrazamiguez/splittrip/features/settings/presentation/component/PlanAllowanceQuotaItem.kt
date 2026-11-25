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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.AnimatedProgressBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceUsageStatus

private val QUOTA_ICON_SIZE = 20.dp

@Composable
internal fun PlanAllowanceQuotaItem(
    quota: AllowanceQuotaUiModel,
    modifier: Modifier = Modifier
) {
    val icon = quota.type.toIconVector()

    val tagStyle = when (quota.status) {
        AllowanceUsageStatus.HEALTHY -> PassportTagStyle.SUCCESS
        AllowanceUsageStatus.APPROACHING_LIMIT -> PassportTagStyle.WARNING
        AllowanceUsageStatus.EXHAUSTED -> PassportTagStyle.ERROR
        AllowanceUsageStatus.UNLIMITED -> PassportTagStyle.INFO
        AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE -> PassportTagStyle.NEUTRAL
    }

    val barColor = when (quota.status) {
        AllowanceUsageStatus.HEALTHY -> MaterialTheme.colorScheme.secondary
        AllowanceUsageStatus.APPROACHING_LIMIT -> MaterialTheme.colorScheme.tertiary
        AllowanceUsageStatus.EXHAUSTED -> MaterialTheme.colorScheme.error
        AllowanceUsageStatus.UNLIMITED -> MaterialTheme.colorScheme.secondary
        AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE -> MaterialTheme.colorScheme.outline
    }

    val iconTint = if (quota.status == AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.primary
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
                tint = iconTint,
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

        if (quota.usageText != null) {
            LabelText(text = quota.usageText.asString())
        }

        if (quota.showProgressBar) {
            AnimatedProgressBar(
                progress = quota.progressFraction,
                fillColor = barColor,
                animationKey = quota.type
            )
        }
    }
}
