package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import kotlinx.collections.immutable.ImmutableList

interface SubscriptionsUiMapper {
    fun mapPlans(
        currentTier: SubscriptionTier,
        selectedInterval: BillingInterval,
        products: List<SubscriptionProduct>
    ): ImmutableList<SubscriptionPlanUiModel>

    fun formatSavingsBadge(): UiText
    fun formatUpgradeSuccessMessage(tier: SubscriptionTier): UiText
    fun formatRestorePurchasesSuccessMessage(): UiText
    fun formatPurchasePendingMessage(): UiText
    fun formatAlreadyOwnedMessage(): UiText
    fun formatNoPurchasesToRestoreMessage(): UiText
    fun formatBillingError(errorMessage: String?): UiText
}
