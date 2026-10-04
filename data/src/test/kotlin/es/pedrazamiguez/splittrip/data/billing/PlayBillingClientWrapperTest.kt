package es.pedrazamiguez.splittrip.data.billing

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayBillingClientWrapperTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var billingClient: BillingClient
    private lateinit var wrapper: PlayBillingClientWrapper

    private val okResult: BillingResult = mockk {
        every { responseCode } returns BillingClient.BillingResponseCode.OK
        every { debugMessage } returns ""
    }

    private val errorResult: BillingResult = mockk {
        every { responseCode } returns BillingClient.BillingResponseCode.ERROR
        every { debugMessage } returns "Billing error"
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers {
            firstArg<CharSequence?>().isNullOrEmpty()
        }
        context = mockk(relaxed = true)
        billingClient = mockk(relaxed = true)
        wrapper = PlayBillingClientWrapper(
            context = context,
            ioDispatcher = testDispatcher,
            isSimulationOverride = false,
            billingClientFactory = { billingClient }
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(TextUtils::class)
    }

    private fun mockSuccessfulConnection() {
        every { billingClient.isReady } returns false andThen true
        every { billingClient.startConnection(any()) } answers {
            val listener = firstArg<BillingClientStateListener>()
            listener.onBillingSetupFinished(okResult)
        }
    }

    private fun createMockProductDetails(
        productId: String,
        formattedPrice: String = "$4.99",
        priceMicros: Long = 4990000L,
        currencyCode: String = "USD",
        token: String = "token_123"
    ): ProductDetails {
        val details = mockk<ProductDetails>(relaxed = true)
        every { details.productId } returns productId
        val offer = mockk<ProductDetails.SubscriptionOfferDetails>()
        every { offer.offerToken } returns token
        val pricingPhase = mockk<ProductDetails.PricingPhase>()
        every { pricingPhase.formattedPrice } returns formattedPrice
        every { pricingPhase.priceAmountMicros } returns priceMicros
        every { pricingPhase.priceCurrencyCode } returns currencyCode
        val pricingPhases = mockk<ProductDetails.PricingPhases>()
        every { pricingPhases.pricingPhaseList } returns listOf(pricingPhase)
        every { offer.pricingPhases } returns pricingPhases
        every { details.subscriptionOfferDetails } returns listOf(offer)
        return details
    }

    private fun createMockQueryResult(
        products: List<ProductDetails> = emptyList()
    ): QueryProductDetailsResult = QueryProductDetailsResult.create(products, emptyList())

    @Test
    fun `querySubscriptionProducts starts connection and queries product details successfully`() = runTest(
        testDispatcher
    ) {
        mockSuccessfulConnection()
        val monthlyDetails = createMockProductDetails(BillingConstants.PRODUCT_ID_PRO_MONTHLY, "$4.99")
        val annualDetails = createMockProductDetails(BillingConstants.PRODUCT_ID_PRO_ANNUAL, "$39.99")

        every { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) } answers {
            val listener = secondArg<ProductDetailsResponseListener>()
            listener.onProductDetailsResponse(okResult, createMockQueryResult(listOf(monthlyDetails, annualDetails)))
        }

        val result = wrapper.querySubscriptionProducts()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val products = result.getOrThrow()
        assertEquals(2, products.size)
        assertEquals(BillingConstants.PRODUCT_ID_PRO_MONTHLY, products[0].productId)
        assertEquals(BillingInterval.MONTHLY, products[0].billingInterval)
        assertEquals(SubscriptionTier.PRO, products[0].tier)
        assertEquals("$4.99", products[0].formattedPrice)

        assertEquals(BillingConstants.PRODUCT_ID_PRO_ANNUAL, products[1].productId)
        assertEquals(BillingInterval.ANNUAL, products[1].billingInterval)
        assertEquals("$39.99", products[1].formattedPrice)

        assertEquals(products, wrapper.subscriptionProducts.value)
    }

    @Test
    fun `querySubscriptionProducts returns failure when connection fails`() = runTest(testDispatcher) {
        every { billingClient.isReady } returns false
        every { billingClient.startConnection(any()) } answers {
            val listener = firstArg<BillingClientStateListener>()
            listener.onBillingSetupFinished(errorResult)
        }

        val result = wrapper.querySubscriptionProducts()
        advanceUntilIdle()

        assertTrue(result.isFailure)
    }

    @Test
    fun `querySubscriptionProducts returns failure when billingResult is not OK`() = runTest(testDispatcher) {
        mockSuccessfulConnection()
        every { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) } answers {
            val listener = secondArg<ProductDetailsResponseListener>()
            listener.onProductDetailsResponse(errorResult, emptyList())
        }

        val result = wrapper.querySubscriptionProducts()
        advanceUntilIdle()

        assertTrue(result.isFailure)
    }

    @Test
    fun `launchBillingFlow with valid activity and cached product launches billing flow`() = runTest(testDispatcher) {
        mockSuccessfulConnection()
        val monthlyDetails = createMockProductDetails(BillingConstants.PRODUCT_ID_PRO_MONTHLY)
        every { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) } answers {
            val listener = secondArg<ProductDetailsResponseListener>()
            listener.onProductDetailsResponse(okResult, listOf(monthlyDetails))
        }
        wrapper.querySubscriptionProducts()
        advanceUntilIdle()

        val activity = mockk<Activity>()
        every { billingClient.launchBillingFlow(activity, any<BillingFlowParams>()) } returns okResult

        val result = wrapper.launchBillingFlow(activity, BillingConstants.PRODUCT_ID_PRO_MONTHLY)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { billingClient.launchBillingFlow(activity, any<BillingFlowParams>()) }
    }

    @Test
    fun `launchBillingFlow with non-activity argument returns failure`() {
        val result = wrapper.launchBillingFlow("NotAnActivity", BillingConstants.PRODUCT_ID_PRO_MONTHLY)
        assertTrue(result.isFailure)
    }

    @Test
    fun `launchBillingFlow with missing cached product returns failure`() {
        val activity = mockk<Activity>()
        val result = wrapper.launchBillingFlow(activity, "unknown_product")
        assertTrue(result.isFailure)
    }

    @Test
    fun `onPurchasesUpdated with OK and unacknowledged purchase acknowledges and emits Success`() = runTest(
        testDispatcher
    ) {
        val purchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns false
            every { purchaseToken } returns "token_abc"
            every { products } returns listOf(BillingConstants.PRODUCT_ID_PRO_MONTHLY)
        }
        every { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(okResult)
        }

        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            wrapper.purchaseUpdates.toList(emitted)
        }

        wrapper.onPurchasesUpdated(okResult, listOf(purchase))
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        val status = emitted.first() as PurchaseStatus.Success
        assertEquals(BillingConstants.PRODUCT_ID_PRO_MONTHLY, status.productId)
        assertEquals("token_abc", status.purchaseToken)
        verify(exactly = 1) { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any()) }
        job.cancel()
    }

    @Test
    fun `onPurchasesUpdated with OK and acknowledged purchase emits Success without re-acknowledging`() = runTest(
        testDispatcher
    ) {
        val purchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns true
            every { purchaseToken } returns "token_abc"
            every { products } returns listOf(BillingConstants.PRODUCT_ID_PRO_MONTHLY)
        }

        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            wrapper.purchaseUpdates.toList(emitted)
        }

        wrapper.onPurchasesUpdated(okResult, listOf(purchase))
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        assertTrue(emitted.first() is PurchaseStatus.Success)
        verify(exactly = 0) { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any()) }
        job.cancel()
    }

    @Test
    fun `onPurchasesUpdated with USER_CANCELED emits PurchaseStatus UserCanceled`() = runTest(testDispatcher) {
        val canceledResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.USER_CANCELED
            every { debugMessage } returns "Canceled"
        }

        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            wrapper.purchaseUpdates.toList(emitted)
        }

        wrapper.onPurchasesUpdated(canceledResult, null)
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        assertEquals(PurchaseStatus.UserCanceled, emitted.first())
        job.cancel()
    }

    @Test
    fun `onPurchasesUpdated with ITEM_ALREADY_OWNED emits PurchaseStatus AlreadyOwned`() = runTest(testDispatcher) {
        val alreadyOwnedResult = mockk<BillingResult> {
            every { responseCode } returns BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
            every { debugMessage } returns "Already owned"
        }

        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            wrapper.purchaseUpdates.toList(emitted)
        }

        wrapper.onPurchasesUpdated(alreadyOwnedResult, null)
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        assertEquals(PurchaseStatus.AlreadyOwned, emitted.first())
        job.cancel()
    }

    @Test
    fun `onPurchasesUpdated with error code emits PurchaseStatus Error`() = runTest(testDispatcher) {
        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            wrapper.purchaseUpdates.toList(emitted)
        }

        wrapper.onPurchasesUpdated(errorResult, null)
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        val status = emitted.first() as PurchaseStatus.Error
        assertEquals("Billing error", status.message)
        job.cancel()
    }

    @Test
    fun `onPurchasesUpdated with PENDING purchase state emits PurchaseStatus Pending`() = runTest(testDispatcher) {
        val purchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PENDING
        }

        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            wrapper.purchaseUpdates.toList(emitted)
        }

        wrapper.onPurchasesUpdated(okResult, listOf(purchase))
        advanceUntilIdle()

        assertEquals(1, emitted.size)
        assertEquals(PurchaseStatus.Pending, emitted.first())
        job.cancel()
    }

    @Test
    fun `restorePurchases queries purchases and returns true when active pro subscription exists`() = runTest(
        testDispatcher
    ) {
        mockSuccessfulConnection()
        val purchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns true
            every { purchaseToken } returns "token_restore"
            every { products } returns listOf(BillingConstants.PRODUCT_ID_PRO_ANNUAL)
        }

        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(okResult, listOf(purchase))
        }

        val result = wrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `restorePurchases acknowledges unacknowledged active pro subscription`() = runTest(testDispatcher) {
        mockSuccessfulConnection()
        val purchase = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns false
            every { purchaseToken } returns "token_unack"
            every { products } returns listOf(BillingConstants.PRODUCT_ID_PRO_MONTHLY)
        }

        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(okResult, listOf(purchase))
        }
        every { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any()) } answers {
            secondArg<AcknowledgePurchaseResponseListener>().onAcknowledgePurchaseResponse(okResult)
        }

        val result = wrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        verify(exactly = 1) { billingClient.acknowledgePurchase(any<AcknowledgePurchaseParams>(), any()) }
    }

    @Test
    fun `restorePurchases returns false when no active pro subscriptions exist`() = runTest(testDispatcher) {
        mockSuccessfulConnection()
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(okResult, emptyList())
        }

        val result = wrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `restorePurchases returns failure when queryPurchasesAsync fails`() = runTest(testDispatcher) {
        mockSuccessfulConnection()
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(errorResult, emptyList())
        }

        val result = wrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isFailure)
    }

    private fun createSimulationWrapper(): PlayBillingClientWrapper {
        return PlayBillingClientWrapper(
            context = context,
            ioDispatcher = testDispatcher,
            isSimulationOverride = true,
            billingClientFactory = { billingClient }
        )
    }

    @Test
    fun `querySubscriptionProducts returns mock products when simulation is active and connection fails`() = runTest(
        testDispatcher
    ) {
        val simWrapper = createSimulationWrapper()
        every { billingClient.isReady } returns false
        every { billingClient.startConnection(any()) } answers {
            val listener = firstArg<BillingClientStateListener>()
            listener.onBillingSetupFinished(errorResult)
        }

        val result = simWrapper.querySubscriptionProducts()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        val products = result.getOrThrow()
        assertEquals(2, products.size)
        assertEquals(BillingConstants.PRODUCT_ID_PRO_MONTHLY, products[0].productId)
        assertEquals(BillingConstants.PRODUCT_ID_PRO_ANNUAL, products[1].productId)
        assertEquals(products, simWrapper.subscriptionProducts.value)
    }

    @Test
    fun `querySubscriptionProducts returns mock products when simulation is active and query fails`() = runTest(
        testDispatcher
    ) {
        val simWrapper = createSimulationWrapper()
        mockSuccessfulConnection()
        every { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) } answers {
            val listener = secondArg<ProductDetailsResponseListener>()
            listener.onProductDetailsResponse(errorResult, emptyList())
        }

        val result = simWrapper.querySubscriptionProducts()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun `querySubscriptionProducts returns mock products when simulation is active and 0 products returned`() =
        runTest(testDispatcher) {
            val simWrapper = createSimulationWrapper()
            mockSuccessfulConnection()
            every { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) } answers {
                val listener = secondArg<ProductDetailsResponseListener>()
                listener.onProductDetailsResponse(okResult, emptyList())
            }

            val result = simWrapper.querySubscriptionProducts()
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
        }

    @Test
    fun `launchBillingFlow with missing cached product emits success when simulation is active`() = runTest(
        testDispatcher
    ) {
        val simWrapper = createSimulationWrapper()
        val activity = mockk<Activity>()
        val emitted = mutableListOf<PurchaseStatus>()
        val job = launch {
            simWrapper.purchaseUpdates.toList(emitted)
        }

        val result = simWrapper.launchBillingFlow(activity, BillingConstants.PRODUCT_ID_PRO_MONTHLY)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(1, emitted.size)
        val status = emitted.first() as PurchaseStatus.Success
        assertEquals(BillingConstants.PRODUCT_ID_PRO_MONTHLY, status.productId)
        job.cancel()
    }

    @Test
    fun `restorePurchases returns true when simulation is active and connection fails`() = runTest(testDispatcher) {
        val simWrapper = createSimulationWrapper()
        every { billingClient.isReady } returns false
        every { billingClient.startConnection(any()) } answers {
            val listener = firstArg<BillingClientStateListener>()
            listener.onBillingSetupFinished(errorResult)
        }

        val result = simWrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `restorePurchases returns true when simulation is active and query fails`() = runTest(testDispatcher) {
        val simWrapper = createSimulationWrapper()
        mockSuccessfulConnection()
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(errorResult, emptyList())
        }

        val result = simWrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `restorePurchases returns true when simulation is active and no pro purchase exists`() = runTest(
        testDispatcher
    ) {
        val simWrapper = createSimulationWrapper()
        mockSuccessfulConnection()
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) } answers {
            secondArg<PurchasesResponseListener>().onQueryPurchasesResponse(okResult, emptyList())
        }

        val result = simWrapper.restorePurchases()
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun `billingSimulationEnabled from appConfigRepository enables simulation when isSimulationOverride is null`() =
        runTest(testDispatcher) {
            val appConfigRepository = mockk<AppConfigRepository> {
                every { billingSimulationEnabled } returns MutableStateFlow(true)
            }
            val repoWrapper = PlayBillingClientWrapper(
                context = context,
                appConfigRepository = appConfigRepository,
                ioDispatcher = testDispatcher,
                billingClientFactory = { billingClient }
            )
            mockSuccessfulConnection()
            every { billingClient.queryProductDetailsAsync(any<QueryProductDetailsParams>(), any()) } answers {
                val listener = secondArg<ProductDetailsResponseListener>()
                listener.onProductDetailsResponse(errorResult, emptyList())
            }

            val result = repoWrapper.querySubscriptionProducts()
            advanceUntilIdle()

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
        }
}
