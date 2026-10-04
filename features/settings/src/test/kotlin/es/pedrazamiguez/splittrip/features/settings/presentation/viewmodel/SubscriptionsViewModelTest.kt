package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.SubscriptionsUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.SubscriptionsUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.SubscriptionsUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SubscriptionsViewModel")
class SubscriptionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase
    private lateinit var updateUserTierUseCase: UpdateUserTierUseCase
    private lateinit var isUserAnonymousUseCase: IsUserAnonymousUseCase
    private lateinit var subscriptionsUiMapper: SubscriptionsUiMapper
    private lateinit var billingService: BillingService

    private val userProfileFlow = MutableSharedFlow<User?>(replay = 1)
    private val subscriptionProductsFlow = MutableStateFlow<List<SubscriptionProduct>>(emptyList())
    private val purchaseUpdatesFlow = MutableSharedFlow<PurchaseStatus>()

    private val testUser = User(
        userId = "user_123",
        email = "test@example.com",
        displayName = "Test User"
    )

    private val mockFreePlan = SubscriptionPlanUiModel(
        tier = SubscriptionTier.FREE,
        title = UiText.StringResource(R.string.subscriptions_tier_free_title),
        description = UiText.StringResource(R.string.subscriptions_tier_free_description),
        price = UiText.StringResource(R.string.subscriptions_tier_free_price),
        period = UiText.StringResource(R.string.subscriptions_tier_free_period),
        badge = null,
        features = persistentListOf(),
        isCurrentPlan = true,
        ctaButtonText = UiText.StringResource(R.string.subscriptions_cta_current_plan),
        isCtaButtonEnabled = false,
        isHighlightedCard = false
    )

    private val mockProPlan = SubscriptionPlanUiModel(
        tier = SubscriptionTier.PRO,
        title = UiText.StringResource(R.string.subscriptions_tier_pro_title),
        description = UiText.StringResource(R.string.subscriptions_tier_pro_description),
        price = UiText.StringResource(R.string.subscriptions_tier_pro_price_annual),
        period = UiText.StringResource(R.string.subscriptions_period_month),
        badge = UiText.StringResource(R.string.subscriptions_badge_popular),
        features = persistentListOf(),
        isCurrentPlan = false,
        ctaButtonText = UiText.StringResource(R.string.subscriptions_cta_upgrade_pro),
        isCtaButtonEnabled = true,
        isHighlightedCard = true
    )

    private val defaultTestProducts = listOf(
        SubscriptionProduct(
            productId = BillingConstants.PRODUCT_ID_PRO_MONTHLY,
            tier = SubscriptionTier.PRO,
            billingInterval = BillingInterval.MONTHLY,
            formattedPrice = "$4.99",
            priceAmountMicros = 4990000L,
            priceCurrencyCode = "USD"
        ),
        SubscriptionProduct(
            productId = BillingConstants.PRODUCT_ID_PRO_ANNUAL,
            tier = SubscriptionTier.PRO,
            billingInterval = BillingInterval.ANNUAL,
            formattedPrice = "$39.99",
            priceAmountMicros = 39990000L,
            priceCurrencyCode = "USD"
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getCurrentUserProfileUseCase = mockk()
        observeCurrentUserProfileUseCase = mockk()
        updateUserTierUseCase = mockk(relaxed = true)
        isUserAnonymousUseCase = mockk()
        subscriptionsUiMapper = mockk()
        billingService = mockk(relaxed = true)

        userProfileFlow.tryEmit(testUser)
        subscriptionProductsFlow.value = defaultTestProducts

        coEvery { getCurrentUserProfileUseCase() } returns testUser
        every { observeCurrentUserProfileUseCase() } returns userProfileFlow
        every { isUserAnonymousUseCase() } returns flowOf(false)
        coEvery { updateUserTierUseCase(any(), any()) } returns Result.success(Unit)

        every { billingService.subscriptionProducts } returns subscriptionProductsFlow
        every { billingService.purchaseUpdates } returns purchaseUpdatesFlow
        coEvery { billingService.querySubscriptionProducts() } returns Result.success(emptyList())

        setupUiMapperMocks()
    }

    private fun setupUiMapperMocks() {
        every {
            subscriptionsUiMapper.mapPlans(any(), any(), any())
        } returns persistentListOf(mockFreePlan, mockProPlan)
        every { subscriptionsUiMapper.formatUpgradeSuccessMessage(any()) } returns UiText.StringResource(
            R.string.subscriptions_upgrade_success,
            UiText.StringResource(R.string.subscriptions_tier_pro_title)
        )
        every { subscriptionsUiMapper.formatRestorePurchasesSuccessMessage() } returns UiText.StringResource(
            R.string.subscriptions_restore_success
        )
        every { subscriptionsUiMapper.formatPurchasePendingMessage() } returns UiText.StringResource(
            R.string.subscriptions_purchase_pending
        )
        every { subscriptionsUiMapper.formatAlreadyOwnedMessage() } returns UiText.StringResource(
            R.string.subscriptions_purchase_already_owned
        )
        every { subscriptionsUiMapper.formatNoPurchasesToRestoreMessage() } returns UiText.StringResource(
            R.string.subscriptions_restore_none_found
        )
        every { subscriptionsUiMapper.formatBillingError() } returns UiText.StringResource(
            R.string.subscriptions_billing_error
        )
        every { subscriptionsUiMapper.formatSubscriptionsUnavailableMessage() } returns UiText.StringResource(
            R.string.subscriptions_service_unavailable
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SubscriptionsViewModel = SubscriptionsViewModel(
        getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
        observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
        updateUserTierUseCase = updateUserTierUseCase,
        isUserAnonymousUseCase = isUserAnonymousUseCase,
        subscriptionsUiMapper = subscriptionsUiMapper,
        billingService = billingService
    )

    @Nested
    @DisplayName("Initial state loading")
    inner class InitialState {

        @Test
        fun `initial state loads profile, checks anonymous status, and maps plans`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isAnonymous)
            assertEquals(BillingInterval.ANNUAL, state.selectedInterval)
            assertEquals(SubscriptionTier.FREE, state.currentTier)
            assertEquals(2, state.plans.size)
            assertFalse(state.isProcessingAction)
            coVerify(atLeast = 1) { billingService.querySubscriptionProducts() }
        }

        @Test
        fun `guest user state sets isAnonymous to true`() = runTest(testDispatcher) {
            every { isUserAnonymousUseCase() } returns flowOf(true)
            coEvery { getCurrentUserProfileUseCase() } returns null
            userProfileFlow.tryEmit(null)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isAnonymous)
        }

        @Test
        fun `LoadSubscriptions event triggers reload`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(SubscriptionsUiEvent.LoadSubscriptions)
            advanceUntilIdle()

            coVerify(atLeast = 2) { getCurrentUserProfileUseCase() }
            coVerify(atLeast = 2) { billingService.querySubscriptionProducts() }
        }

        @Test
        fun `loadSubscriptions handles exception gracefully`() = runTest(testDispatcher) {
            coEvery { getCurrentUserProfileUseCase() } throws RuntimeException("Network error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `user profile observation updates currentTier and re-maps plans`() = runTest(testDispatcher) {
            val proUser = testUser.copy(tier = SubscriptionTier.PRO)
            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.currentTier)

            userProfileFlow.emit(proUser)
            advanceUntilIdle()

            assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
            coVerify {
                subscriptionsUiMapper.mapPlans(
                    currentTier = SubscriptionTier.PRO,
                    selectedInterval = BillingInterval.ANNUAL,
                    products = any()
                )
            }
        }

        @Test
        fun `Observing subscriptionProducts updates uiState plans with dynamic pricing`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val product = SubscriptionProduct(
                productId = BillingConstants.PRODUCT_ID_PRO_MONTHLY,
                tier = SubscriptionTier.PRO,
                billingInterval = BillingInterval.MONTHLY,
                formattedPrice = "$4.99",
                priceAmountMicros = 4990000L,
                priceCurrencyCode = "USD"
            )
            subscriptionProductsFlow.value = listOf(product)
            advanceUntilIdle()

            coVerify {
                subscriptionsUiMapper.mapPlans(
                    currentTier = SubscriptionTier.FREE,
                    selectedInterval = BillingInterval.ANNUAL,
                    products = listOf(product)
                )
            }
        }
    }

    @Nested
    @DisplayName("SelectBillingInterval")
    inner class SelectBillingInterval {

        @Test
        fun `SelectBillingInterval updates interval and re-maps plans`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(SubscriptionsUiEvent.SelectBillingInterval(BillingInterval.MONTHLY))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(BillingInterval.MONTHLY, state.selectedInterval)
            coVerify {
                subscriptionsUiMapper.mapPlans(
                    currentTier = SubscriptionTier.FREE,
                    selectedInterval = BillingInterval.MONTHLY,
                    products = any()
                )
            }
        }
    }

    @Nested
    @DisplayName("UpgradePlan")
    inner class UpgradePlan {

        @Test
        fun `UpgradePlan for Pro with Monthly selected emits LaunchBillingFlow with monthly id`() = runTest(
            testDispatcher
        ) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(SubscriptionsUiEvent.SelectBillingInterval(BillingInterval.MONTHLY))
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.PRO))
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.LaunchBillingFlow::class.java, actions.first())
            assertEquals(BillingConstants.PRODUCT_ID_PRO_MONTHLY, action.productId)

            job.cancel()
        }

        @Test
        fun `UpgradePlan for Pro with Annual selected emits LaunchBillingFlow with annual id`() = runTest(
            testDispatcher
        ) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.PRO))
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.LaunchBillingFlow::class.java, actions.first())
            assertEquals(BillingConstants.PRODUCT_ID_PRO_ANNUAL, action.productId)

            job.cancel()
        }

        @Test
        fun `UpgradePlan for Pro when product is not available emits ShowTopPill with service unavailable message`() =
            runTest(testDispatcher) {
                subscriptionProductsFlow.value = emptyList()
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<SubscriptionsUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.PRO))
                advanceUntilIdle()

                assertEquals(1, actions.size)
                val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.subscriptions_service_unavailable, message.resId)

                job.cancel()
            }

        @Test
        fun `UpgradePlan for Free invokes UpdateUserTierUseCase`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.FREE))
            advanceUntilIdle()

            coVerify(exactly = 1) { updateUserTierUseCase("user_123", SubscriptionTier.FREE) }
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            job.cancel()
        }

        @Test
        fun `launchBillingFlow delegates to BillingService and emits ShowTopPill on failure`() = runTest(
            testDispatcher
        ) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            every {
                billingService.launchBillingFlow(any(), any())
            } returns Result.failure(IllegalStateException("Flow error"))

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.launchBillingFlow("activity", BillingConstants.PRODUCT_ID_PRO_MONTHLY)
            advanceUntilIdle()

            assertEquals(1, actions.size)
            assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            job.cancel()
        }
    }

    @Nested
    @DisplayName("purchaseUpdates observation")
    inner class PurchaseUpdatesObservation {

        @Test
        fun `purchaseUpdates Success updates user tier to PRO and emits upgrade success top pill`() = runTest(
            testDispatcher
        ) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            purchaseUpdatesFlow.emit(
                PurchaseStatus.Success(BillingConstants.PRODUCT_ID_PRO_ANNUAL, "token")
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { updateUserTierUseCase("user_123", SubscriptionTier.PRO) }
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_upgrade_success, message.resId)

            job.cancel()
        }

        @Test
        fun `purchaseUpdates Pending emits pending message top pill`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            purchaseUpdatesFlow.emit(PurchaseStatus.Pending)
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_purchase_pending, message.resId)

            job.cancel()
        }

        @Test
        fun `purchaseUpdates AlreadyOwned updates user tier to PRO and emits already owned top pill`() = runTest(
            testDispatcher
        ) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            purchaseUpdatesFlow.emit(PurchaseStatus.AlreadyOwned)
            advanceUntilIdle()

            coVerify(exactly = 1) { updateUserTierUseCase("user_123", SubscriptionTier.PRO) }
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_purchase_already_owned, message.resId)

            job.cancel()
        }

        @Test
        fun `purchaseUpdates UserCanceled emits no action`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            purchaseUpdatesFlow.emit(PurchaseStatus.UserCanceled)
            advanceUntilIdle()

            assertEquals(0, actions.size)
            job.cancel()
        }

        @Test
        fun `purchaseUpdates Error emits ShowTopPill with error message`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            purchaseUpdatesFlow.emit(PurchaseStatus.Error("Failed"))
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_billing_error, message.resId)

            job.cancel()
        }
    }

    @Nested
    @DisplayName("RestorePurchases")
    inner class RestorePurchases {

        @Test
        fun `RestorePurchases with true result updates user tier to PRO and emits restore success top pill`() = runTest(
            testDispatcher
        ) {
            coEvery { billingService.restorePurchases() } returns Result.success(true)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
            advanceUntilIdle()

            coVerify(exactly = 1) { updateUserTierUseCase("user_123", SubscriptionTier.PRO) }
            assertFalse(viewModel.uiState.value.isProcessingAction)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_restore_success, message.resId)

            job.cancel()
        }

        @Test
        fun `RestorePurchases with false result emits no purchases found top pill`() = runTest(testDispatcher) {
            coEvery { billingService.restorePurchases() } returns Result.success(false)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
            advanceUntilIdle()

            coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
            assertFalse(viewModel.uiState.value.isProcessingAction)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_restore_none_found, message.resId)

            job.cancel()
        }

        @Test
        fun `RestorePurchases with failure emits error top pill`() = runTest(testDispatcher) {
            coEvery {
                billingService.restorePurchases()
            } returns Result.failure(IllegalStateException("Billing unavailable"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isProcessingAction)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_billing_error, message.resId)

            job.cancel()
        }
    }
}
