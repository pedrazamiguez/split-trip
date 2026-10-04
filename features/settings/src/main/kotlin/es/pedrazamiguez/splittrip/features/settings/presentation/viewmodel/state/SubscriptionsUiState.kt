package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SubscriptionsUiState(
    val selectedInterval: BillingInterval = BillingInterval.ANNUAL,
    val plans: ImmutableList<SubscriptionPlanUiModel> = persistentListOf(),
    val currentTier: SubscriptionTier = SubscriptionTier.FREE,
    val isAnonymous: Boolean = false,
    val isLoading: Boolean = false,
    val isProcessingAction: Boolean = false
)
