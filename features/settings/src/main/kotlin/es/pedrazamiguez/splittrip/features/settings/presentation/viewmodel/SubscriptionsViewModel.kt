package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.PurchaseStatus
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.SubscriptionsUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.SubscriptionsUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.SubscriptionsUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.SubscriptionsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val GOOGLE_PLAY_SUBSCRIPTIONS_URL =
    "https://play.google.com/store/account/subscriptions?package=es.pedrazamiguez.splittrip"

class SubscriptionsViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    private val updateUserTierUseCase: UpdateUserTierUseCase,
    private val isUserAnonymousUseCase: IsUserAnonymousUseCase,
    private val subscriptionsUiMapper: SubscriptionsUiMapper,
    private val billingService: BillingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    private val _actions = Channel<SubscriptionsUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        observeUserProfile()
        observeBillingProducts()
        observePurchaseUpdates()
        loadSubscriptions()
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            observeCurrentUserProfileUseCase().collect { user ->
                if (user != null) {
                    val selectedInterval = _uiState.value.selectedInterval
                    val products = billingService.subscriptionProducts.value
                    val plans = subscriptionsUiMapper.mapPlans(
                        currentTier = user.tier,
                        selectedInterval = selectedInterval,
                        products = products
                    )
                    _uiState.update {
                        it.copy(
                            currentTier = user.tier,
                            plans = plans
                        )
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
                val plans = subscriptionsUiMapper.mapPlans(
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
                        handlePurchaseSuccess(isAlreadyOwned = false)
                    }
                    is PurchaseStatus.Pending -> {
                        val message = subscriptionsUiMapper.formatPurchasePendingMessage()
                        _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                    }
                    is PurchaseStatus.AlreadyOwned -> {
                        handlePurchaseSuccess(isAlreadyOwned = true)
                    }
                    is PurchaseStatus.UserCanceled -> {
                        // User canceled purchase dialog; no action needed
                    }
                    is PurchaseStatus.Error -> {
                        val errorException = IllegalStateException(
                            "Billing purchase update error: ${status.message ?: "Unknown billing error"}"
                        )
                        Timber.e(errorException, "Billing purchase update error: %s", status.message)
                        val message = subscriptionsUiMapper.formatBillingError()
                        _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                    }
                }
            }
        }
    }

    private suspend fun handlePurchaseSuccess(isAlreadyOwned: Boolean) {
        try {
            val user = getCurrentUserProfileUseCase()
            if (user != null) {
                updateUserTierUseCase(user.userId, SubscriptionTier.PRO)
            }
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
            val message = subscriptionsUiMapper.formatBillingError()
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        }
    }

    fun onEvent(event: SubscriptionsUiEvent) {
        when (event) {
            SubscriptionsUiEvent.LoadSubscriptions -> loadSubscriptions()
            is SubscriptionsUiEvent.SelectBillingInterval -> handleSelectBillingInterval(event.interval)
            is SubscriptionsUiEvent.UpgradePlan -> handlePlanAction(event.tier)
            SubscriptionsUiEvent.ManageSubscription -> {
                _uiState.update { it.copy(showManageSubscriptionDialog = true) }
            }
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
            viewModelScope.launch {
                val message = subscriptionsUiMapper.formatBillingError()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
        }
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val queryResult = billingService.querySubscriptionProducts()
                if (queryResult.isFailure) {
                    Timber.w(
                        queryResult.exceptionOrNull(),
                        "Failed to query subscription products from billing service"
                    )
                }
                val isAnon = isUserAnonymousUseCase().firstOrNull() ?: false
                val currentUser = getCurrentUserProfileUseCase()
                val currentTier = currentUser?.tier ?: SubscriptionTier.FREE
                val selectedInterval = _uiState.value.selectedInterval
                val products = billingService.subscriptionProducts.value
                val plans = subscriptionsUiMapper.mapPlans(
                    currentTier = currentTier,
                    selectedInterval = selectedInterval,
                    products = products
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAnonymous = isAnon,
                        currentTier = currentTier,
                        plans = plans
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load subscriptions")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleSelectBillingInterval(interval: BillingInterval) {
        val currentTier = _uiState.value.currentTier
        val products = billingService.subscriptionProducts.value
        val updatedPlans = subscriptionsUiMapper.mapPlans(
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
    }

    private fun handlePlanAction(tier: SubscriptionTier) {
        if (tier == SubscriptionTier.PRO) {
            handleUpgradeToPro()
        } else {
            _uiState.update { it.copy(showManageSubscriptionDialog = true) }
        }
    }

    private fun handleUpgradeToPro() {
        val selectedInterval = _uiState.value.selectedInterval
        val hasProduct = billingService.subscriptionProducts.value.any { it.billingInterval == selectedInterval }
        if (!hasProduct) {
            viewModelScope.launch {
                val message = subscriptionsUiMapper.formatSubscriptionsUnavailableMessage()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
            return
        }
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
        viewModelScope.launch {
            _actions.send(SubscriptionsUiAction.OpenUrl(GOOGLE_PLAY_SUBSCRIPTIONS_URL))
        }
    }

    private fun handleRestorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAction = true) }
            try {
                val restoreResult = billingService.restorePurchases()
                if (restoreResult.isSuccess) {
                    processRestoreSuccess(restoreResult.getOrDefault(false))
                } else {
                    Timber.e(restoreResult.exceptionOrNull(), "Failed to restore purchases")
                    val message = subscriptionsUiMapper.formatBillingError()
                    _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle restore purchases")
                val message = subscriptionsUiMapper.formatBillingError()
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            } finally {
                _uiState.update { it.copy(isProcessingAction = false) }
            }
        }
    }

    private suspend fun processRestoreSuccess(hasActivePro: Boolean) {
        if (hasActivePro) {
            val user = getCurrentUserProfileUseCase()
            if (user != null) {
                updateUserTierUseCase(user.userId, SubscriptionTier.PRO)
            }
            val message = subscriptionsUiMapper.formatRestorePurchasesSuccessMessage()
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        } else {
            val message = subscriptionsUiMapper.formatNoPurchasesToRestoreMessage()
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        }
    }
}
