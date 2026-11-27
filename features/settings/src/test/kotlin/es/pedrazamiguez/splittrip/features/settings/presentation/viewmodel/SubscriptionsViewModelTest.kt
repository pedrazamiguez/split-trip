package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetReceiptExtractionCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveProAllowlistAccessUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileSubscriptionEntitlementsUseCase
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
import io.mockk.verify
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
    private lateinit var observeProAllowlistAccessUseCase: ObserveProAllowlistAccessUseCase
    private lateinit var updateUserTierUseCase: UpdateUserTierUseCase
    private lateinit var isUserAnonymousUseCase: IsUserAnonymousUseCase
    private lateinit var reconcileSubscriptionEntitlementsUseCase: ReconcileSubscriptionEntitlementsUseCase
    private lateinit var getReceiptExtractionCapabilityUseCase: GetReceiptExtractionCapabilityUseCase
    private lateinit var subscriptionsUiMapper: SubscriptionsUiMapper
    private lateinit var billingService: BillingService
    private lateinit var telemetryTracker: TelemetryTracker

    private val userProfileFlow = MutableSharedFlow<User?>(replay = 1)
    private val proAllowlistAccessFlow = MutableStateFlow(false)
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
        isCtaButtonEnabled = false, isHighlightedCard = false
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
        isCtaButtonEnabled = true, isHighlightedCard = true
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
        observeProAllowlistAccessUseCase = mockk()
        updateUserTierUseCase = mockk(relaxed = true)
        isUserAnonymousUseCase = mockk()
        reconcileSubscriptionEntitlementsUseCase = mockk()
        getReceiptExtractionCapabilityUseCase = mockk()
        subscriptionsUiMapper = mockk()
        billingService = mockk(relaxed = true)
        telemetryTracker = mockk(relaxed = true)

        userProfileFlow.tryEmit(testUser)
        proAllowlistAccessFlow.value = false
        subscriptionProductsFlow.value = defaultTestProducts

        coEvery { getCurrentUserProfileUseCase() } returns testUser
        every { observeCurrentUserProfileUseCase() } returns userProfileFlow
        every { observeProAllowlistAccessUseCase() } returns proAllowlistAccessFlow
        every { isUserAnonymousUseCase() } returns flowOf(false)
        coEvery { reconcileSubscriptionEntitlementsUseCase() } returns Result.success(Unit)
        coEvery { updateUserTierUseCase(any(), any()) } returns Result.success(Unit)
        every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.ON_DEVICE_AI

        every { billingService.subscriptionProducts } returns subscriptionProductsFlow
        every { billingService.purchaseUpdates } returns purchaseUpdatesFlow
        coEvery { billingService.querySubscriptionProducts() } returns Result.success(emptyList())

        setupUiMapperMocks()
    }

    private fun setupUiMapperMocks() {
        every {
            subscriptionsUiMapper.mapPlans(any(), any(), any(), any())
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
        every { subscriptionsUiMapper.formatAlreadyOwnedMessage() } returns
            UiText.StringResource(R.string.subscriptions_purchase_already_owned)
        every { subscriptionsUiMapper.formatNoPurchasesToRestoreMessage() } returns
            UiText.StringResource(R.string.subscriptions_restore_none_found)
        every { subscriptionsUiMapper.formatBillingError() } returns
            UiText.StringResource(R.string.subscriptions_billing_error)
        every { subscriptionsUiMapper.formatSubscriptionsUnavailableMessage() } returns
            UiText.StringResource(R.string.subscriptions_service_unavailable)
        every { subscriptionsUiMapper.formatComplimentaryProMessage() } returns
            UiText.StringResource(R.string.subscriptions_complimentary_pro_manage_info)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(entrySource: String? = null): SubscriptionsViewModel = SubscriptionsViewModel(
        subscriptionsUseCases = SubscriptionsUseCases(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
            observeProAllowlistAccessUseCase = observeProAllowlistAccessUseCase,
            updateUserTierUseCase = updateUserTierUseCase,
            isUserAnonymousUseCase = isUserAnonymousUseCase,
            reconcileSubscriptionEntitlementsUseCase = reconcileSubscriptionEntitlementsUseCase,
            getReceiptExtractionCapabilityUseCase = getReceiptExtractionCapabilityUseCase
        ),
        subscriptionsUiMapper = subscriptionsUiMapper,
        billingService = billingService,
        telemetryTracker = telemetryTracker,
        entrySource = entrySource
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
        fun `loadSubscriptions emits NavigateToAccountStatus when user is anonymous`() = runTest(testDispatcher) {
            every { isUserAnonymousUseCase() } returns flowOf(true)
            coEvery { getCurrentUserProfileUseCase() } returns null
            userProfileFlow.tryEmit(null)

            val actions = mutableListOf<SubscriptionsUiAction>()
            val viewModel = createViewModel()
            val job = launch { viewModel.actions.collect { actions.add(it) } }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isAnonymous)
            assertTrue(actions.contains(SubscriptionsUiAction.NavigateToAccountStatus))
            coVerify(exactly = 0) { billingService.querySubscriptionProducts() }
            job.cancel()
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
        fun `loadSubscriptions emits ShowTopPill and stops loading when query returns failure`() =
            runTest(testDispatcher) {
                coEvery {
                    billingService.querySubscriptionProducts()
                } returns Result.failure(IllegalStateException("Billing connection timed out"))

                val actions = mutableListOf<SubscriptionsUiAction>()
                val viewModel = createViewModel()
                val job = launch { viewModel.actions.collect { actions.add(it) } }
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.isLoading)
                val topPillAction = actions.filterIsInstance<SubscriptionsUiAction.ShowTopPill>().firstOrNull()
                assertTrue(topPillAction != null)
                val stringRes = assertInstanceOf(UiText.StringResource::class.java, topPillAction!!.message)
                assertEquals(R.string.subscriptions_service_unavailable, stringRes.resId)
                assertEquals(TopPillStyle.ERROR, topPillAction.style)

                job.cancel()
            }

        @Test
        fun `loadSubscriptions sets isLoading false and emits ShowTopPill when query throws`() =
            runTest(testDispatcher) {
                coEvery {
                    billingService.querySubscriptionProducts()
                } throws RuntimeException("Billing IPC crash")

                val actions = mutableListOf<SubscriptionsUiAction>()
                val viewModel = createViewModel()
                val job = launch { viewModel.actions.collect { actions.add(it) } }
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.isLoading)
                val topPillAction = actions.filterIsInstance<SubscriptionsUiAction.ShowTopPill>().firstOrNull()
                assertTrue(topPillAction != null)
                val stringRes = assertInstanceOf(UiText.StringResource::class.java, topPillAction!!.message)
                assertEquals(R.string.subscriptions_service_unavailable, stringRes.resId)
                assertEquals(TopPillStyle.ERROR, topPillAction.style)

                job.cancel()
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
                    products = any(),
                    extractionCapability = ExtractionCapability.ON_DEVICE_AI
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
                    products = listOf(product),
                    extractionCapability = ExtractionCapability.ON_DEVICE_AI
                )
            }
        }

        @Test
        fun `loadSubscriptions invokes reconcileSubscriptionEntitlementsUseCase for non-anonymous users`() = runTest(
            testDispatcher
        ) {
            createViewModel()
            advanceUntilIdle()

            coVerify(exactly = 1) { reconcileSubscriptionEntitlementsUseCase() }
        }

        @Test
        fun `loadSubscriptions still maps cached PRO plans when reconciliation fails`() = runTest(
            testDispatcher
        ) {
            val proUser = testUser.copy(tier = SubscriptionTier.PRO)
            coEvery { getCurrentUserProfileUseCase() } returns proUser
            userProfileFlow.tryEmit(proUser)
            coEvery {
                reconcileSubscriptionEntitlementsUseCase()
            } returns Result.failure(IllegalStateException("Offline"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
            coVerify(atLeast = 1) {
                subscriptionsUiMapper.mapPlans(
                    currentTier = SubscriptionTier.PRO,
                    selectedInterval = BillingInterval.ANNUAL,
                    products = any(),
                    extractionCapability = ExtractionCapability.ON_DEVICE_AI
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
                    products = any(),
                    extractionCapability = ExtractionCapability.ON_DEVICE_AI
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
                assertEquals(TopPillStyle.ERROR, action.style)

                job.cancel()
            }

        @Test
        fun `UpgradePlan for Free shows manage dialog and confirm emits OpenUrl`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.FREE))
            assertTrue(viewModel.uiState.value.showManageSubscriptionDialog)
            viewModel.onEvent(SubscriptionsUiEvent.DismissManageSubscriptionDialog)
            assertFalse(viewModel.uiState.value.showManageSubscriptionDialog)
            viewModel.onEvent(SubscriptionsUiEvent.ManageSubscription)
            assertTrue(viewModel.uiState.value.showManageSubscriptionDialog)
            viewModel.onEvent(SubscriptionsUiEvent.ConfirmManageSubscription)
            assertFalse(viewModel.uiState.value.showManageSubscriptionDialog)
            advanceUntilIdle()

            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.OpenUrl::class.java, actions.first())
            assertTrue(action.url.contains("play.google.com"))
            job.cancel()
        }

        @Test
        fun `onEvent UpgradePlan for PRO emits NavigateToAccountStatus when user is anonymous`() = runTest(
            testDispatcher
        ) {
            every { isUserAnonymousUseCase() } returns flowOf(true)
            coEvery { getCurrentUserProfileUseCase() } returns null
            userProfileFlow.tryEmit(null)

            val viewModel = createViewModel()
            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }
            advanceUntilIdle()
            actions.clear()

            viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.PRO))
            advanceUntilIdle()

            assertEquals(1, actions.size)
            assertEquals(SubscriptionsUiAction.NavigateToAccountStatus, actions.first())

            job.cancel()
        }

        @Test
        fun `launchBillingFlow delegates to BillingService and emits ShowTopPill on failure`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                every { billingService.launchBillingFlow(any(), any()) } returns
                    Result.failure(IllegalStateException("Flow error"))

                val actions = mutableListOf<SubscriptionsUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.launchBillingFlow("activity", BillingConstants.PRODUCT_ID_PRO_MONTHLY)
                advanceUntilIdle()

                assertEquals(1, actions.size)
                val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
                assertEquals(TopPillStyle.ERROR, action.style)
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
            assertEquals(TopPillStyle.SUCCESS, action.style)

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
            assertEquals(TopPillStyle.INFO, action.style)

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
            assertEquals(TopPillStyle.SUCCESS, action.style)

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
            assertEquals(TopPillStyle.ERROR, action.style)

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
            assertEquals(TopPillStyle.SUCCESS, action.style)

            job.cancel()
        }

        @Test
        fun `RestorePurchases with false result reconciles and emits no purchases found top pill`() = runTest(
            testDispatcher
        ) {
            coEvery { billingService.restorePurchases() } returns Result.success(false)

            val viewModel = createViewModel()
            advanceUntilIdle()
            coVerify(exactly = 1) { reconcileSubscriptionEntitlementsUseCase() }

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
            advanceUntilIdle()

            coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
            coVerify(exactly = 2) { reconcileSubscriptionEntitlementsUseCase() }
            assertFalse(viewModel.uiState.value.isProcessingAction)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_restore_none_found, message.resId)
            assertEquals(TopPillStyle.INFO, action.style)

            job.cancel()
        }

        @Test
        fun `RestorePurchases with failure does not reconcile again and emits error top pill`() = runTest(
            testDispatcher
        ) {
            coEvery {
                billingService.restorePurchases()
            } returns Result.failure(IllegalStateException("Billing unavailable"))

            val viewModel = createViewModel()
            advanceUntilIdle()
            coVerify(exactly = 1) { reconcileSubscriptionEntitlementsUseCase() }

            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
            advanceUntilIdle()

            coVerify(exactly = 1) { reconcileSubscriptionEntitlementsUseCase() }
            assertFalse(viewModel.uiState.value.isProcessingAction)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
            val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
            assertEquals(R.string.subscriptions_billing_error, message.resId)
            assertEquals(TopPillStyle.ERROR, action.style)

            job.cancel()
        }

        @Test
        fun `onEvent RestorePurchases emits NavigateToAccountStatus when user is anonymous`() = runTest(
            testDispatcher
        ) {
            every { isUserAnonymousUseCase() } returns flowOf(true)
            coEvery { getCurrentUserProfileUseCase() } returns null
            userProfileFlow.tryEmit(null)

            val viewModel = createViewModel()
            val actions = mutableListOf<SubscriptionsUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }
            advanceUntilIdle()
            actions.clear()

            viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
            advanceUntilIdle()

            coVerify(exactly = 0) { billingService.restorePurchases() }
            assertEquals(1, actions.size)
            assertEquals(SubscriptionsUiAction.NavigateToAccountStatus, actions.first())

            job.cancel()
        }
    }

    @Nested
    @DisplayName("Complimentary Pro allowlist")
    inner class ComplimentaryProAllowlist {

        @Test
        fun `allowlisted FREE user displays PRO as active tier and maps plans with PRO as current plan`() =
            runTest(testDispatcher) {
                proAllowlistAccessFlow.value = true

                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(SubscriptionTier.PRO, state.currentTier)
                assertTrue(state.isAllowlistedPro)
                coVerify(atLeast = 1) {
                    subscriptionsUiMapper.mapPlans(
                        currentTier = SubscriptionTier.PRO,
                        selectedInterval = BillingInterval.ANNUAL,
                        products = any(),
                        extractionCapability = ExtractionCapability.ON_DEVICE_AI
                    )
                }
            }

        @Test
        fun `allowlist status updates reactively when access flips false to true and back`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.currentTier)
                assertFalse(viewModel.uiState.value.isAllowlistedPro)
                coVerify(exactly = 1) { reconcileSubscriptionEntitlementsUseCase() }

                proAllowlistAccessFlow.value = true
                advanceUntilIdle()

                assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
                assertTrue(viewModel.uiState.value.isAllowlistedPro)

                proAllowlistAccessFlow.value = false
                advanceUntilIdle()

                assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.currentTier)
                assertFalse(viewModel.uiState.value.isAllowlistedPro)
                coVerify(exactly = 2) { reconcileSubscriptionEntitlementsUseCase() }
            }

        @Test
        fun `ManageSubscription for allowlisted user without Play purchase emits ShowTopPill`() =
            runTest(testDispatcher) {
                proAllowlistAccessFlow.value = true
                coEvery { billingService.queryActivePurchases() } returns Result.success(emptyList())

                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<SubscriptionsUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(SubscriptionsUiEvent.ManageSubscription)
                advanceUntilIdle()

                assertFalse(viewModel.uiState.value.showManageSubscriptionDialog)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(SubscriptionsUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.subscriptions_complimentary_pro_manage_info, message.resId)
                assertEquals(TopPillStyle.INFO, action.style)

                job.cancel()
            }

        @Test
        fun `ManageSubscription for allowlisted user with active Play purchase opens ManageSubscriptionDialog`() =
            runTest(testDispatcher) {
                proAllowlistAccessFlow.value = true
                coEvery {
                    billingService.queryActivePurchases()
                } returns Result.success(listOf(BillingConstants.PRODUCT_ID_PRO_ANNUAL))

                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<SubscriptionsUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(SubscriptionsUiEvent.ManageSubscription)
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.showManageSubscriptionDialog)
                assertTrue(actions.isEmpty())

                job.cancel()
            }
    }

    @Nested
    @DisplayName("Telemetry")
    inner class Telemetry {

        @Test
        fun `loadSubscriptions tracks paywall_viewed once for non-anonymous user with entry_source and tier`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel(entrySource = MonetizationTelemetry.EntrySource.GROUPS_FAB)
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.LoadSubscriptions)
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PAYWALL_VIEWED,
                        mapOf(
                            MonetizationTelemetry.Param.ENTRY_SOURCE to MonetizationTelemetry.EntrySource.GROUPS_FAB,
                            MonetizationTelemetry.Param.CURRENT_TIER to "free",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "annual"
                        )
                    )
                }
            }

        @Test
        fun `loadSubscriptions does not track paywall_viewed when user is anonymous`() =
            runTest(testDispatcher) {
                every { isUserAnonymousUseCase() } returns flowOf(true)

                createViewModel(entrySource = MonetizationTelemetry.EntrySource.SETTINGS)
                advanceUntilIdle()

                verify(exactly = 0) {
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.PAYWALL_VIEWED, any())
                }
            }

        @Test
        fun `SelectBillingInterval tracks billing_interval_selected only when interval changes`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.SelectBillingInterval(BillingInterval.ANNUAL))
                viewModel.onEvent(SubscriptionsUiEvent.SelectBillingInterval(BillingInterval.MONTHLY))
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.BILLING_INTERVAL_SELECTED,
                        mapOf(
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "monthly",
                            MonetizationTelemetry.Param.CURRENT_TIER to "free"
                        )
                    )
                }
            }

        @Test
        fun `UpgradePlan for PRO tracks purchase_initiated when product is available`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel(entrySource = MonetizationTelemetry.EntrySource.SETTINGS)
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.PRO))
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PURCHASE_INITIATED,
                        mapOf(
                            MonetizationTelemetry.Param.TARGET_TIER to "pro",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "annual",
                            MonetizationTelemetry.Param.ENTRY_SOURCE to MonetizationTelemetry.EntrySource.SETTINGS
                        )
                    )
                }
            }

        @Test
        fun `UpgradePlan for PRO tracks purchase_failed with product_unavailable when product is missing`() =
            runTest(testDispatcher) {
                subscriptionProductsFlow.value = emptyList()
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.UpgradePlan(SubscriptionTier.PRO))
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PURCHASE_FAILED,
                        mapOf(
                            MonetizationTelemetry.Param.TARGET_TIER to "pro",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "annual",
                            MonetizationTelemetry.Param.FAILURE_STAGE to
                                MonetizationTelemetry.FailureStage.PRODUCT_UNAVAILABLE
                        )
                    )
                }
            }

        @Test
        fun `launchBillingFlow failure tracks purchase_failed with launch_billing_flow stage`() =
            runTest(testDispatcher) {
                every {
                    billingService.launchBillingFlow(any(), BillingConstants.PRODUCT_ID_PRO_MONTHLY)
                } returns Result.failure(IllegalStateException("Billing unavailable"))
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.launchBillingFlow(mockk(), BillingConstants.PRODUCT_ID_PRO_MONTHLY)
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PURCHASE_FAILED,
                        mapOf(
                            MonetizationTelemetry.Param.TARGET_TIER to "pro",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "monthly",
                            MonetizationTelemetry.Param.FAILURE_STAGE to
                                MonetizationTelemetry.FailureStage.LAUNCH_BILLING_FLOW
                        )
                    )
                }
            }

        @Test
        fun `purchaseUpdates Success tracks purchase_completed without purchase token or price`() =
            runTest(testDispatcher) {
                createViewModel(entrySource = MonetizationTelemetry.EntrySource.AI_RECEIPT_SCANNING)
                advanceUntilIdle()

                purchaseUpdatesFlow.emit(
                    PurchaseStatus.Success(BillingConstants.PRODUCT_ID_PRO_MONTHLY, "secret_token")
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PURCHASE_COMPLETED,
                        mapOf(
                            MonetizationTelemetry.Param.TARGET_TIER to "pro",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "monthly",
                            MonetizationTelemetry.Param.IS_ALREADY_OWNED to false,
                            MonetizationTelemetry.Param.ENTRY_SOURCE to
                                MonetizationTelemetry.EntrySource.AI_RECEIPT_SCANNING
                        )
                    )
                }
            }

        @Test
        fun `purchaseUpdates AlreadyOwned tracks purchase_completed with is_already_owned true`() =
            runTest(testDispatcher) {
                createViewModel()
                advanceUntilIdle()

                purchaseUpdatesFlow.emit(PurchaseStatus.AlreadyOwned)
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PURCHASE_COMPLETED,
                        mapOf(
                            MonetizationTelemetry.Param.TARGET_TIER to "pro",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "annual",
                            MonetizationTelemetry.Param.IS_ALREADY_OWNED to true,
                            MonetizationTelemetry.Param.ENTRY_SOURCE to MonetizationTelemetry.EntrySource.DIRECT
                        )
                    )
                }
            }

        @Test
        fun `purchaseUpdates Success when tier update fails tracks purchase_failed with tier_persistence stage`() =
            runTest(testDispatcher) {
                coEvery { updateUserTierUseCase(any(), any()) } throws RuntimeException("DB write failed")
                createViewModel()
                advanceUntilIdle()

                purchaseUpdatesFlow.emit(
                    PurchaseStatus.Success(BillingConstants.PRODUCT_ID_PRO_ANNUAL, "token")
                )
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.PURCHASE_FAILED,
                        mapOf(
                            MonetizationTelemetry.Param.TARGET_TIER to "pro",
                            MonetizationTelemetry.Param.BILLING_INTERVAL to "annual",
                            MonetizationTelemetry.Param.FAILURE_STAGE to
                                MonetizationTelemetry.FailureStage.TIER_PERSISTENCE
                        )
                    )
                }
            }

        @Test
        fun `purchaseUpdates Pending tracks purchase_pending`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            purchaseUpdatesFlow.emit(PurchaseStatus.Pending)
            advanceUntilIdle()

            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.PURCHASE_PENDING,
                    mapOf(
                        MonetizationTelemetry.Param.TARGET_TIER to "pro",
                        MonetizationTelemetry.Param.BILLING_INTERVAL to "annual"
                    )
                )
            }
        }

        @Test
        fun `purchaseUpdates UserCanceled tracks purchase_canceled`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            purchaseUpdatesFlow.emit(PurchaseStatus.UserCanceled)
            advanceUntilIdle()

            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.PURCHASE_CANCELED,
                    mapOf(
                        MonetizationTelemetry.Param.TARGET_TIER to "pro",
                        MonetizationTelemetry.Param.BILLING_INTERVAL to "annual"
                    )
                )
            }
        }

        @Test
        fun `purchaseUpdates Error tracks purchase_failed with purchase_update stage`() = runTest(testDispatcher) {
            createViewModel()
            advanceUntilIdle()

            purchaseUpdatesFlow.emit(PurchaseStatus.Error("Billing error"))
            advanceUntilIdle()

            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.PURCHASE_FAILED,
                    mapOf(
                        MonetizationTelemetry.Param.TARGET_TIER to "pro",
                        MonetizationTelemetry.Param.BILLING_INTERVAL to "annual",
                        MonetizationTelemetry.Param.FAILURE_STAGE to MonetizationTelemetry.FailureStage.PURCHASE_UPDATE
                    )
                )
            }
        }

        @Test
        fun `RestorePurchases true tracks restore_purchases_attempted and restore_purchases_succeeded`() =
            runTest(testDispatcher) {
                coEvery { billingService.restorePurchases() } returns Result.success(true)
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_ATTEMPTED)
                    telemetryTracker.trackEvent(
                        MonetizationTelemetry.Event.RESTORE_PURCHASES_SUCCEEDED,
                        mapOf(MonetizationTelemetry.Param.TARGET_TIER to "pro")
                    )
                }
            }

        @Test
        fun `RestorePurchases false tracks restore_purchases_attempted and restore_purchases_empty`() =
            runTest(testDispatcher) {
                coEvery { billingService.restorePurchases() } returns Result.success(false)
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_ATTEMPTED)
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_EMPTY)
                }
            }

        @Test
        fun `RestorePurchases failure tracks restore_purchases_attempted and restore_purchases_failed`() =
            runTest(testDispatcher) {
                coEvery { billingService.restorePurchases() } returns Result.failure(RuntimeException("Network error"))
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(SubscriptionsUiEvent.RestorePurchases)
                advanceUntilIdle()

                verify(exactly = 1) {
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_ATTEMPTED)
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_FAILED)
                }
            }

        @Test
        fun `ConfirmManageSubscription tracks manage_subscription_opened`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(SubscriptionsUiEvent.ConfirmManageSubscription)
            advanceUntilIdle()

            verify(exactly = 1) {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.MANAGE_SUBSCRIPTION_OPENED,
                    mapOf(MonetizationTelemetry.Param.CURRENT_TIER to "free")
                )
            }
        }
    }

    @Nested
    @DisplayName("Device AI capability forwarding")
    inner class DeviceAiCapability {

        @Test
        fun `loadSubscriptions forwards UNSUPPORTED extractionCapability to mapper when device lacks on-device AI`() =
            runTest(testDispatcher) {
                every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.UNSUPPORTED

                createViewModel()
                advanceUntilIdle()

                coVerify(atLeast = 1) {
                    subscriptionsUiMapper.mapPlans(
                        currentTier = SubscriptionTier.FREE,
                        selectedInterval = BillingInterval.ANNUAL,
                        products = any(),
                        extractionCapability = ExtractionCapability.UNSUPPORTED
                    )
                }
            }

        @Test
        fun `SelectBillingInterval forwards current device extractionCapability to mapper`() = runTest(
            testDispatcher
        ) {
            every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.UNSUPPORTED
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(SubscriptionsUiEvent.SelectBillingInterval(BillingInterval.MONTHLY))
            advanceUntilIdle()

            coVerify {
                subscriptionsUiMapper.mapPlans(
                    currentTier = SubscriptionTier.FREE,
                    selectedInterval = BillingInterval.MONTHLY,
                    products = any(),
                    extractionCapability = ExtractionCapability.UNSUPPORTED
                )
            }
        }

        @Test
        fun `user profile and product updates forward current device extractionCapability to mapper`() = runTest(
            testDispatcher
        ) {
            every { getReceiptExtractionCapabilityUseCase() } returns ExtractionCapability.UNSUPPORTED
            createViewModel()
            advanceUntilIdle()

            val proUser = testUser.copy(tier = SubscriptionTier.PRO)
            userProfileFlow.emit(proUser)
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
                    currentTier = SubscriptionTier.PRO,
                    selectedInterval = BillingInterval.ANNUAL,
                    products = listOf(product),
                    extractionCapability = ExtractionCapability.UNSUPPORTED
                )
            }
        }
    }
}
