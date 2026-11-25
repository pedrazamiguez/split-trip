package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.SubscriptionsUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.SubscriptionsUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.SubscriptionsUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.SubscriptionsUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val GOOGLE_PLAY_SUBSCRIPTIONS_URL =
    "https://play.google.com/store/account/subscriptions?package=es.pedrazamiguez.splittrip"

@Suppress("TooManyFunctions")
class SubscriptionsViewModel(
    private val subscriptionsUseCases: SubscriptionsUseCases,
    private val subscriptionsUiMapper: SubscriptionsUiMapper,
    private val billingService: BillingService,
    private val telemetryTracker: TelemetryTracker,
    private val entrySource: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    private val _actions = Channel<SubscriptionsUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    private var hasTrackedPaywallView: Boolean = false

    init {
        observeSubscriptionProfile()
        observeBillingProducts()
        observePurchaseUpdates()
        loadSubscriptions()
    }

    private fun observeSubscriptionProfile() {
        viewModelScope.launch {
            var wasAllowlistedPro = false
            combine(
                subscriptionsUseCases.observeCurrentUserProfileUseCase(),
                subscriptionsUseCases.observeProAllowlistAccessUseCase()
            ) { user, isAllowlistedPro ->
                user to isAllowlistedPro
            }.collect { (user, isAllowlistedPro) ->
                if (user != null) {
                    val effectiveTier = if (isAllowlistedPro) SubscriptionTier.PRO else user.tier
                    val selectedInterval = _uiState.value.selectedInterval
                    val products = billingService.subscriptionProducts.value
                    val plans = mapPlansForDevice(
                        currentTier = effectiveTier,
                        selectedInterval = selectedInterval,
                        products = products
                    )
                    _uiState.update {
                        it.copy(
                            currentTier = effectiveTier,
                            isAllowlistedPro = isAllowlistedPro,
                            plans = plans
                        )
                    }
                }
                val wasRevoked = wasAllowlistedPro && !isAllowlistedPro
                wasAllowlistedPro = isAllowlistedPro
                if (wasRevoked) {
                    subscriptionsUseCases.reconcileSubscriptionEntitlementsUseCase()
                        .onFailure { error ->
                            Timber.w(error, "Best-effort entitlement reconciliation failed on allowlist revocation")
                        }
                }
            }
        }
    }

    private fun observeBillingProducts() {
        viewModelScope.launch {
            billingService.subscriptionProducts.collect { products ->
                val currentTier = _uiState.value.currentTier
                val selectedInterval = _uiState.value.selectedInterval
                val plans = mapPlansForDevice(
                    currentTier = currentTier,
                    selectedInterval = selectedInterval,
                    products = products
                )
                _uiState.update {
                    it.copy(plans = plans)
                }
            }
        }
    }

    private fun observePurchaseUpdates() {
        viewModelScope.launch {
            billingService.purchaseUpdates.collect { status ->
                when (status) {
                    is PurchaseStatus.Success -> {
                        val interval = resolveBillingInterval(status.productId)
                        handlePurchaseSuccess(isAlreadyOwned = false, billingInterval = interval)
                    }
                    is PurchaseStatus.Pending -> {
                        telemetryTracker.trackEvent(
                            MonetizationTelemetry.Event.PURCHASE_PENDING,
                            mapOf(
                                MonetizationTelemetry.Param.TARGET_TIER to SubscriptionTier.PRO.name.lowercase(),
                                MonetizationTelemetry.Param.BILLING_INTERVAL to
                                    _uiState.value.selectedInterval.name.lowercase()
                            )
                        )
                        val message = subscriptionsUiMapper.formatPurchasePendingMessage()
                        _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                    }
                    is PurchaseStatus.AlreadyOwned -> {
                        handlePurchaseSuccess(
                            isAlreadyOwned = true,
                            billingInterval = _uiState.value.selectedInterval
                        )
                    }
                    is PurchaseStatus.UserCanceled -> {
                        telemetryTracker.trackEvent(
                            MonetizationTelemetry.Event.PURCHASE_CANCELED,
                            mapOf(
                                MonetizationTelemetry.Param.TARGET_TIER to SubscriptionTier.PRO.name.lowercase(),
                                MonetizationTelemetry.Param.BILLING_INTERVAL to
                                    _uiState.value.selectedInterval.name.lowercase()
                            )
                        )
                    }
                    is PurchaseStatus.Error -> {
                        val errorException = IllegalStateException(
                            "Billing purchase update error: ${status.message ?: "Unknown billing error"}"
                        )
                        Timber.e(errorException, "Billing purchase update error: %s", status.message)
                        trackPurchaseFailed(
                            billingInterval = _uiState.value.selectedInterval,
                            failureStage = MonetizationTelemetry.FailureStage.PURCHASE_UPDATE
                        )
                        val message = subscriptionsUiMapper.formatBillingError()
                        _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                    }
                }
            }
        }
    }

    private suspend fun handlePurchaseSuccess(
        isAlreadyOwned: Boolean,
        billingInterval: BillingInterval
    ) {
        try {
            val user = subscriptionsUseCases.getCurrentUserProfileUseCase()
            if (user != null) {
                subscriptionsUseCases.updateUserTierUseCase(user.userId, SubscriptionTier.PRO)
            }
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.PURCHASE_COMPLETED,
                mapOf(
                    MonetizationTelemetry.Param.TARGET_TIER to SubscriptionTier.PRO.name.lowercase(),
                    MonetizationTelemetry.Param.BILLING_INTERVAL to billingInterval.name.lowercase(),
                    MonetizationTelemetry.Param.IS_ALREADY_OWNED to isAlreadyOwned,
                    MonetizationTelemetry.Param.ENTRY_SOURCE to resolvedEntrySource()
                )
            )
            val message = if (isAlreadyOwned) {
                subscriptionsUiMapper.formatAlreadyOwnedMessage()
            } else {
                subscriptionsUiMapper.formatUpgradeSuccessMessage(SubscriptionTier.PRO)
            }
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user tier after purchase")
            trackPurchaseFailed(
                billingInterval = billingInterval,
                failureStage = MonetizationTelemetry.FailureStage.TIER_PERSISTENCE
            )
            val message = subscriptionsUiMapper.formatBillingError()
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        }
    }

    fun onEvent(event: SubscriptionsUiEvent) {
        when (event) {
            SubscriptionsUiEvent.LoadSubscriptions -> loadSubscriptions()
            is SubscriptionsUiEvent.SelectBillingInterval -> handleSelectBillingInterval(event.interval)
            is SubscriptionsUiEvent.UpgradePlan -> handlePlanAction(event.tier)
            SubscriptionsUiEvent.ManageSubscription -> handleManageSubscription()
            SubscriptionsUiEvent.DismissManageSubscriptionDialog -> {
                _uiState.update { it.copy(showManageSubscriptionDialog = false) }
            }
            SubscriptionsUiEvent.ConfirmManageSubscription -> confirmManageSubscription()
            SubscriptionsUiEvent.RestorePurchases -> handleRestorePurchases()
        }
    }

    fun launchBillingFlow(activity: Any, productId: String) {
        val result = billingService.launchBillingFlow(activity, productId)
        if (result.isFailure) {
            val failureThrowable = result.exceptionOrNull()
                ?: IllegalStateException("Failed to launch billing flow for product: $productId")
            Timber.e(failureThrowable, "Failed to launch billing flow for product: %s", productId)
            trackPurchaseFailed(
                billingInterval = resolveBillingInterval(productId),
                failureStage = MonetizationTelemetry.FailureStage.LAUNCH_BILLING_FLOW
            )
            viewModelScope.launch {
                val message = subscriptionsUiMapper.formatBillingError()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
        }
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            val isAnon = subscriptionsUseCases.isUserAnonymousUseCase().firstOrNull() ?: false
            if (isAnon) {
                _uiState.update { it.copy(isLoading = false, isAnonymous = true) }
                _actions.send(SubscriptionsUiAction.NavigateToAccountStatus)
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            try {
                val queryResult = billingService.querySubscriptionProducts()
                if (queryResult.isFailure) {
                    Timber.w(
                        queryResult.exceptionOrNull(),
                        "Failed to query subscription products from billing service"
                    )
                    val message = subscriptionsUiMapper.formatSubscriptionsUnavailableMessage()
                    _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                }
                subscriptionsUseCases.reconcileSubscriptionEntitlementsUseCase()
                    .onFailure { error ->
                        Timber.w(error, "Best-effort entitlement reconciliation failed on subscriptions load")
                    }
                val currentUser = subscriptionsUseCases.getCurrentUserProfileUseCase()
                val isAllowlistedPro = subscriptionsUseCases.observeProAllowlistAccessUseCase().firstOrNull() ?: false
                val currentTier = if (isAllowlistedPro) {
                    SubscriptionTier.PRO
                } else {
                    currentUser?.tier ?: SubscriptionTier.FREE
                }
                val selectedInterval = _uiState.value.selectedInterval
                val products = billingService.subscriptionProducts.value
                val plans = mapPlansForDevice(
                    currentTier = currentTier,
                    selectedInterval = selectedInterval,
                    products = products
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAnonymous = isAnon,
                        currentTier = currentTier,
                        isAllowlistedPro = isAllowlistedPro,
                        plans = plans
                    )
                }
                trackPaywallViewedIfNeeded(currentTier = currentTier)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load subscriptions")
                _uiState.update { it.copy(isLoading = false) }
                trackPaywallViewedIfNeeded(currentTier = _uiState.value.currentTier)
                val message = subscriptionsUiMapper.formatSubscriptionsUnavailableMessage()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
        }
    }

    private fun handleSelectBillingInterval(interval: BillingInterval) {
        val previousInterval = _uiState.value.selectedInterval
        val currentTier = _uiState.value.currentTier
        val products = billingService.subscriptionProducts.value
        val updatedPlans = mapPlansForDevice(
            currentTier = currentTier,
            selectedInterval = interval,
            products = products
        )
        _uiState.update {
            it.copy(
                selectedInterval = interval,
                plans = updatedPlans
            )
        }
        if (interval != previousInterval) {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.BILLING_INTERVAL_SELECTED,
                mapOf(
                    MonetizationTelemetry.Param.BILLING_INTERVAL to interval.name.lowercase(),
                    MonetizationTelemetry.Param.CURRENT_TIER to currentTier.name.lowercase()
                )
            )
        }
    }

    private fun handlePlanAction(tier: SubscriptionTier) {
        if (tier == SubscriptionTier.PRO) {
            handleUpgradeToPro()
        } else {
            handleManageSubscription()
        }
    }

    private fun handleManageSubscription() {
        if (!_uiState.value.isAllowlistedPro) {
            _uiState.update { it.copy(showManageSubscriptionDialog = true) }
            return
        }
        viewModelScope.launch {
            val hasActivePlayPro = billingService.queryActivePurchases()
                .getOrDefault(emptyList())
                .any { it in BillingConstants.PRO_PRODUCT_IDS }
            if (hasActivePlayPro) {
                _uiState.update { it.copy(showManageSubscriptionDialog = true) }
            } else {
                val message = subscriptionsUiMapper.formatComplimentaryProMessage()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
        }
    }

    private fun handleUpgradeToPro() {
        if (_uiState.value.isAnonymous) {
            viewModelScope.launch {
                _actions.send(SubscriptionsUiAction.NavigateToAccountStatus)
            }
            return
        }
        if (_uiState.value.isAllowlistedPro) {
            viewModelScope.launch {
                val message = subscriptionsUiMapper.formatComplimentaryProMessage()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
            return
        }
        val selectedInterval = _uiState.value.selectedInterval
        val hasProduct = billingService.subscriptionProducts.value.any { it.billingInterval == selectedInterval }
        if (!hasProduct) {
            trackPurchaseFailed(
                billingInterval = selectedInterval,
                failureStage = MonetizationTelemetry.FailureStage.PRODUCT_UNAVAILABLE
            )
            viewModelScope.launch {
                val message = subscriptionsUiMapper.formatSubscriptionsUnavailableMessage()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
            return
        }
        telemetryTracker.trackEvent(
            MonetizationTelemetry.Event.PURCHASE_INITIATED,
            mapOf(
                MonetizationTelemetry.Param.TARGET_TIER to SubscriptionTier.PRO.name.lowercase(),
                MonetizationTelemetry.Param.BILLING_INTERVAL to selectedInterval.name.lowercase(),
                MonetizationTelemetry.Param.ENTRY_SOURCE to resolvedEntrySource()
            )
        )
        val productId = if (selectedInterval == BillingInterval.MONTHLY) {
            BillingConstants.PRODUCT_ID_PRO_MONTHLY
        } else {
            BillingConstants.PRODUCT_ID_PRO_ANNUAL
        }
        viewModelScope.launch {
            _actions.send(SubscriptionsUiAction.LaunchBillingFlow(productId))
        }
    }

    private fun confirmManageSubscription() {
        _uiState.update { it.copy(showManageSubscriptionDialog = false) }
        telemetryTracker.trackEvent(
            MonetizationTelemetry.Event.MANAGE_SUBSCRIPTION_OPENED,
            mapOf(
                MonetizationTelemetry.Param.CURRENT_TIER to _uiState.value.currentTier.name.lowercase()
            )
        )
        viewModelScope.launch {
            _actions.send(SubscriptionsUiAction.OpenUrl(GOOGLE_PLAY_SUBSCRIPTIONS_URL))
        }
    }

    private fun handleRestorePurchases() {
        if (_uiState.value.isAnonymous) {
            viewModelScope.launch {
                _actions.send(SubscriptionsUiAction.NavigateToAccountStatus)
            }
            return
        }
        telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_ATTEMPTED)
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true) }
            try {
                val restoreResult = billingService.restorePurchases()
                if (restoreResult.isSuccess) {
                    processRestoreSuccess(restoreResult.getOrDefault(false))
                } else {
                    Timber.e(restoreResult.exceptionOrNull(), "Failed to restore purchases")
                    telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_FAILED)
                    val message = subscriptionsUiMapper.formatBillingError()
                    _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle restore purchases")
                telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_FAILED)
                val message = subscriptionsUiMapper.formatBillingError()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    private suspend fun processRestoreSuccess(hasActivePro: Boolean) {
        if (hasActivePro) {
            val user = subscriptionsUseCases.getCurrentUserProfileUseCase()
            if (user != null) {
                subscriptionsUseCases.updateUserTierUseCase(user.userId, SubscriptionTier.PRO)
            }
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.RESTORE_PURCHASES_SUCCEEDED,
                mapOf(MonetizationTelemetry.Param.TARGET_TIER to SubscriptionTier.PRO.name.lowercase())
            )
            val message = subscriptionsUiMapper.formatRestorePurchasesSuccessMessage()
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        } else {
            subscriptionsUseCases.reconcileSubscriptionEntitlementsUseCase()
                .onFailure { error ->
                    Timber.w(error, "Best-effort entitlement reconciliation failed during restore")
                }
            telemetryTracker.trackEvent(MonetizationTelemetry.Event.RESTORE_PURCHASES_EMPTY)
            val message = subscriptionsUiMapper.formatNoPurchasesToRestoreMessage()
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        }
    }

    private fun mapPlansForDevice(
        currentTier: SubscriptionTier,
        selectedInterval: BillingInterval,
        products: List<SubscriptionProduct>
    ): ImmutableList<SubscriptionPlanUiModel> = subscriptionsUiMapper.mapPlans(
        currentTier = currentTier,
        selectedInterval = selectedInterval,
        products = products,
        extractionCapability = subscriptionsUseCases.getReceiptExtractionCapabilityUseCase()
    )

    private fun trackPaywallViewedIfNeeded(currentTier: SubscriptionTier) {
        if (hasTrackedPaywallView) return
        hasTrackedPaywallView = true
        telemetryTracker.trackEvent(
            MonetizationTelemetry.Event.PAYWALL_VIEWED,
            mapOf(
                MonetizationTelemetry.Param.ENTRY_SOURCE to resolvedEntrySource(),
                MonetizationTelemetry.Param.CURRENT_TIER to currentTier.name.lowercase(),
                MonetizationTelemetry.Param.BILLING_INTERVAL to _uiState.value.selectedInterval.name.lowercase()
            )
        )
    }

    private fun trackPurchaseFailed(
        billingInterval: BillingInterval,
        failureStage: String
    ) {
        telemetryTracker.trackEvent(
            MonetizationTelemetry.Event.PURCHASE_FAILED,
            mapOf(
                MonetizationTelemetry.Param.TARGET_TIER to SubscriptionTier.PRO.name.lowercase(),
                MonetizationTelemetry.Param.BILLING_INTERVAL to billingInterval.name.lowercase(),
                MonetizationTelemetry.Param.FAILURE_STAGE to failureStage
            )
        )
    }

    private fun resolveBillingInterval(productId: String): BillingInterval = when (productId) {
        BillingConstants.PRODUCT_ID_PRO_MONTHLY -> BillingInterval.MONTHLY
        BillingConstants.PRODUCT_ID_PRO_ANNUAL -> BillingInterval.ANNUAL
        else -> _uiState.value.selectedInterval
    }

    private fun resolvedEntrySource(): String =
        entrySource ?: MonetizationTelemetry.EntrySource.DIRECT
}
