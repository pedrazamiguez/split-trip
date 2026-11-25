package es.pedrazamiguez.splittrip.features.settings.presentation.model

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class SubscriptionPlanUiModel(
    val tier: SubscriptionTier,
    val title: UiText,
    val description: UiText,
    val price: UiText,
    val period: UiText,
    val billingDetail: UiText? = null,
    val badge: UiText? = null,
    val features: ImmutableList<SubscriptionFeatureUiModel>,
    val compatibilityNotice: UiText? = null,
    val isCurrentPlan: Boolean,
    val ctaButtonText: UiText,
    val isCtaButtonEnabled: Boolean,
    val isHighlightedCard: Boolean
)
