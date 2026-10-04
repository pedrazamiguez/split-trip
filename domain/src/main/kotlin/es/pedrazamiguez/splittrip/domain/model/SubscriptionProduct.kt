package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier

data class SubscriptionProduct(
    val productId: String,
    val tier: SubscriptionTier,
    val billingInterval: BillingInterval,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val offerToken: String = ""
)
