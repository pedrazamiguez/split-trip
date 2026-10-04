package es.pedrazamiguez.splittrip.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import es.pedrazamiguez.splittrip.domain.service.BillingService
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlayBillingClientWrapper(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    billingClientFactory: (PurchasesUpdatedListener) -> BillingClient = { listener ->
        BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()
    }
) : BillingService, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val connectionMutex = Mutex()
    private val cachedProductDetails = ConcurrentHashMap<String, ProductDetails>()

    private val billingClient: BillingClient by lazy {
        billingClientFactory(this)
    }

    private val _subscriptionProducts = MutableStateFlow<List<SubscriptionProduct>>(emptyList())
    override val subscriptionProducts: StateFlow<List<SubscriptionProduct>> = _subscriptionProducts.asStateFlow()

    private val _purchaseUpdates = MutableSharedFlow<PurchaseStatus>()
    override val purchaseUpdates: Flow<PurchaseStatus> = _purchaseUpdates.asSharedFlow()

    private suspend fun ensureConnected(): Result<Unit> = withContext(ioDispatcher) {
        if (billingClient.isReady) {
            return@withContext Result.success(Unit)
        }
        connectionMutex.withLock {
            if (billingClient.isReady) {
                Result.success(Unit)
            } else {
                connectBillingClient()
            }
        }
    }

    private suspend fun connectBillingClient(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (!continuation.isActive) return
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        val code = billingResult.responseCode
                        val msg = billingResult.debugMessage
                        continuation.resume(
                            Result.failure(
                                IllegalStateException("Billing setup failed: $msg (code: $code)")
                            )
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Timber.w("Billing service disconnected")
                }
            })
        }

    override suspend fun querySubscriptionProducts(): Result<List<SubscriptionProduct>> = withContext(ioDispatcher) {
        val connectionResult = ensureConnected()
        if (connectionResult.isFailure) {
            val error = connectionResult.exceptionOrNull()
                ?: IllegalStateException("Failed to connect to billing service")
            return@withContext Result.failure(error)
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_ID_PRO_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val (billingResult, productDetailsList) = billingClient.queryProductDetails(params)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Query product details failed: ${billingResult.debugMessage} (code: ${billingResult.responseCode})"
                )
            )
        }

        val mappedProducts = productDetailsList.orEmpty().map { details ->
            cachedProductDetails[details.productId] = details
            val offer = details.subscriptionOfferDetails?.firstOrNull()
            val pricingPhase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            val interval = if (details.productId == BillingConstants.PRODUCT_ID_PRO_MONTHLY) {
                BillingInterval.MONTHLY
            } else {
                BillingInterval.ANNUAL
            }

            SubscriptionProduct(
                productId = details.productId,
                tier = SubscriptionTier.PRO,
                billingInterval = interval,
                formattedPrice = pricingPhase?.formattedPrice.orEmpty(),
                priceAmountMicros = pricingPhase?.priceAmountMicros ?: 0L,
                priceCurrencyCode = pricingPhase?.priceCurrencyCode.orEmpty(),
                offerToken = offer?.offerToken.orEmpty()
            )
        }

        _subscriptionProducts.value = mappedProducts
        Result.success(mappedProducts)
    }

    override fun launchBillingFlow(activity: Any, productId: String): Result<Unit> {
        val androidActivity = activity as? Activity
            ?: return Result.failure(IllegalArgumentException("Activity parameter must be an android.app.Activity"))
        val productDetails = cachedProductDetails[productId]
            ?: return Result.failure(
                IllegalStateException(
                    "Product details not cached for $productId. Call querySubscriptionProducts() first."
                )
            )
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return Result.failure(IllegalStateException("No offer token found for product $productId"))

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(androidActivity, flowParams)
        return if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException(
                    "Launch billing flow failed: ${result.debugMessage} (code: ${result.responseCode})"
                )
            )
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        scope.launch {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases.orEmpty().forEach { purchase ->
                        when (purchase.purchaseState) {
                            Purchase.PurchaseState.PURCHASED -> {
                                if (!purchase.isAcknowledged) {
                                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                    billingClient.acknowledgePurchase(ackParams)
                                }
                                purchase.products.forEach { productId ->
                                    _purchaseUpdates.emit(
                                        PurchaseStatus.Success(
                                            productId = productId,
                                            purchaseToken = purchase.purchaseToken
                                        )
                                    )
                                }
                            }
                            Purchase.PurchaseState.PENDING -> {
                                _purchaseUpdates.emit(PurchaseStatus.Pending)
                            }
                        }
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _purchaseUpdates.emit(PurchaseStatus.UserCanceled)
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    _purchaseUpdates.emit(PurchaseStatus.AlreadyOwned)
                }
                else -> {
                    _purchaseUpdates.emit(PurchaseStatus.Error(billingResult.debugMessage))
                }
            }
        }
    }

    override suspend fun restorePurchases(): Result<Boolean> = withContext(ioDispatcher) {
        val connectionResult = ensureConnected()
        if (connectionResult.isFailure) {
            val error = connectionResult.exceptionOrNull()
                ?: IllegalStateException("Failed to connect to billing service")
            return@withContext Result.failure(error)
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val (billingResult, purchasesList) = billingClient.queryPurchasesAsync(params)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Query purchases failed: ${billingResult.debugMessage} (code: ${billingResult.responseCode})"
                )
            )
        }

        val proProductIds = setOf(BillingConstants.PRODUCT_ID_PRO_MONTHLY, BillingConstants.PRODUCT_ID_PRO_ANNUAL)
        val activeProPurchase = purchasesList.orEmpty().firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it in proProductIds }
        }

        if (activeProPurchase != null) {
            if (!activeProPurchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(activeProPurchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams)
            }
            Result.success(true)
        } else {
            Result.success(false)
        }
    }
}
