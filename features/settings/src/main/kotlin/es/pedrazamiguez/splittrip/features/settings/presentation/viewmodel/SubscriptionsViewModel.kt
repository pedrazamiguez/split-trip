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
                        val message = subscriptionsUiMapper.formatBillingError(status.message)
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
            val message = subscriptionsUiMapper.formatBillingError(e.message)
            _actions.send(SubscriptionsUiAction.ShowTopPill(message))
        }
    }

    fun onEvent(event: SubscriptionsUiEvent) {
        when (event) {
            SubscriptionsUiEvent.LoadSubscriptions -> loadSubscriptions()
            is SubscriptionsUiEvent.SelectBillingInterval -> handleSelectBillingInterval(event.interval)
            is SubscriptionsUiEvent.UpgradePlan -> handleUpgradePlan(event.tier)
            SubscriptionsUiEvent.RestorePurchases -> handleRestorePurchases()
        }
    }

    fun launchBillingFlow(activity: Any, productId: String) {
        val result = billingService.launchBillingFlow(activity, productId)
        if (result.isFailure) {
            viewModelScope.launch {
                val error = result.exceptionOrNull()?.message
                val message = subscriptionsUiMapper.formatBillingError(error)
                _actions.send(SubscriptionsUiAction.ShowTopPill(message))
            }
        }
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                billingService.querySubscriptionProducts()
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

    private fun handleUpgradePlan(tier: SubscriptionTier) {
        if (tier == SubscriptionTier.PRO) {
            val productId = if (_uiState.value.selectedInterval == BillingInterval.MONTHLY) {
                BillingConstants.PRODUCT_ID_PRO_MONTHLY
            } else {
                BillingConstants.PRODUCT_ID_PRO_ANNUAL
            }
            viewModelScope.launch {
                _actions.send(SubscriptionsUiAction.LaunchBillingFlow(productId))
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isProcessingAction = true) }
                try {
                    val user = getCurrentUserProfileUseCase()
                    if (user != null) {
                        updateUserTierUseCase(user.userId, tier)
                    }
                    val message = subscriptionsUiMapper.formatUpgradeSuccessMessage(tier)
                    _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to handle upgrade plan")
                } finally {
                    _uiState.update { it.copy(isProcessingAction = false) }
                }
            }
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
                    val error = restoreResult.exceptionOrNull()?.message
                    val message = subscriptionsUiMapper.formatBillingError(error)
                    _actions.send(SubscriptionsUiAction.ShowTopPill(message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle restore purchases")
                val message = subscriptionsUiMapper.formatBillingError(e.message)
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
