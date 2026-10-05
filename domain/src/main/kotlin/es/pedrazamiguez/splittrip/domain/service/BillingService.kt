package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BillingService {
    val subscriptionProducts: StateFlow<List<SubscriptionProduct>>
    val purchaseUpdates: Flow<PurchaseStatus>

    suspend fun querySubscriptionProducts(): Result<List<SubscriptionProduct>>
    fun launchBillingFlow(activity: Any, productId: String): Result<Unit>
    suspend fun restorePurchases(): Result<Boolean>
}
