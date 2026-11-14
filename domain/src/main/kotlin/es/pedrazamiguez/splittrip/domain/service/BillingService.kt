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

    /**
     * Queries Google Play for currently active Pro subscription product IDs.
     *
     * Acknowledges any unacknowledged active Pro purchases found.
     * Returns [Result.success] with an empty list only when Google Play explicitly
     * responds with OK and zero active subscriptions. Returns [Result.failure] on
     * connection/timeout/service errors so callers preserve cached entitlements.
     */
    suspend fun queryActivePurchases(): Result<List<String>>
}
