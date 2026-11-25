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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionFeatureStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionFeatureUiModel

private val FEATURE_ICON_SIZE = 18.dp

@Composable
fun PlanFeatureItem(
    feature: SubscriptionFeatureUiModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon = when (feature.status) {
        SubscriptionFeatureStatus.INCLUDED -> TablerIcons.Outline.Check
        SubscriptionFeatureStatus.EXCLUDED -> TablerIcons.Outline.X
        SubscriptionFeatureStatus.UNAVAILABLE_ON_DEVICE -> TablerIcons.Outline.InfoCircle
    }
    val iconColor = when (feature.status) {
        SubscriptionFeatureStatus.INCLUDED -> if (feature.isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        }
        SubscriptionFeatureStatus.EXCLUDED -> MaterialTheme.colorScheme.outline
        SubscriptionFeatureStatus.UNAVAILABLE_ON_DEVICE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textColor = when (feature.status) {
        SubscriptionFeatureStatus.INCLUDED -> MaterialTheme.colorScheme.onSurface
        SubscriptionFeatureStatus.EXCLUDED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        SubscriptionFeatureStatus.UNAVAILABLE_ON_DEVICE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val fontWeight = if (feature.status == SubscriptionFeatureStatus.INCLUDED && feature.isHighlighted) {
        FontWeight.SemiBold
    } else {
        FontWeight.Normal
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(FEATURE_ICON_SIZE)
        )
        Text(
            text = feature.label.asString(context),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}
