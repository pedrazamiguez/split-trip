package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetReceiptExtractionCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveProAllowlistAccessUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileSubscriptionEntitlementsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase

data class SubscriptionsUseCases(
    val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    val observeProAllowlistAccessUseCase: ObserveProAllowlistAccessUseCase,
    val updateUserTierUseCase: UpdateUserTierUseCase,
    val isUserAnonymousUseCase: IsUserAnonymousUseCase,
    val reconcileSubscriptionEntitlementsUseCase: ReconcileSubscriptionEntitlementsUseCase,
    val getReceiptExtractionCapabilityUseCase: GetReceiptExtractionCapabilityUseCase
)
