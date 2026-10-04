package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButtonDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel

private val CARD_ELEVATION_HIGHLIGHTED = 4.dp
private val BADGE_HORIZONTAL_PADDING = 10.dp
private val BADGE_VERTICAL_PADDING = 4.dp

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun PlanComparisonCard(
    plan: SubscriptionPlanUiModel,
    onCtaClick: (SubscriptionTier) -> Unit,
    modifier: Modifier = Modifier,
    isProcessingAction: Boolean = false
) {
    val context = LocalContext.current
    val elevation = if (plan.isHighlightedCard) CARD_ELEVATION_HIGHLIGHTED else 0.dp
    val cardColor = if (plan.isHighlightedCard) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    FlatCard(
        modifier = modifier.fillMaxWidth(),
        color = cardColor,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.ExtraLarge)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title.asString(context),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (plan.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                horizontal = BADGE_HORIZONTAL_PADDING,
                                vertical = BADGE_VERTICAL_PADDING
                            )
                    ) {
                        Text(
                            text = plan.badge.asString(context),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))

            Text(
                text = plan.description.asString(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                Text(
                    text = plan.price.asString(context),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = plan.period.asString(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.ExtraSmall)
                )
            }

            if (plan.billingDetail != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
                Text(
                    text = plan.billingDetail.asString(context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                plan.features.forEach { feature ->
                    PlanFeatureItem(feature = feature)
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

            val buttonColors = if (plan.isHighlightedCard) {
                GradientButtonDefaults.primaryColors()
            } else {
                GradientButtonDefaults.secondaryColors()
            }

            GradientButton(
                text = plan.ctaButtonText.asString(context),
                onClick = { onCtaClick(plan.tier) },
                enabled = plan.isCtaButtonEnabled && !isProcessingAction,
                isLoading = isProcessingAction && plan.isCtaButtonEnabled,
                colors = buttonColors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
