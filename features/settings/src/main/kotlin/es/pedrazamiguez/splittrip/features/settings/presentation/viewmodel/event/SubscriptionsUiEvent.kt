package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier

sealed interface SubscriptionsUiEvent {
    data object LoadSubscriptions : SubscriptionsUiEvent
    data class SelectBillingInterval(val interval: BillingInterval) : SubscriptionsUiEvent
    data class UpgradePlan(val tier: SubscriptionTier) : SubscriptionsUiEvent
    data object ManageSubscription : SubscriptionsUiEvent
    data object DismissManageSubscriptionDialog : SubscriptionsUiEvent
    data object ConfirmManageSubscription : SubscriptionsUiEvent
    data object RestorePurchases : SubscriptionsUiEvent
}
