package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.constant.BillingConstants
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileSubscriptionEntitlementsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import kotlin.coroutines.cancellation.CancellationException

class ReconcileSubscriptionEntitlementsUseCaseImpl(
    private val userRepository: UserRepository,
    private val billingService: BillingService,
    private val updateUserTierUseCase: UpdateUserTierUseCase,
    private val authenticationService: AuthenticationService,
    private val appConfigService: AppConfigService
) : ReconcileSubscriptionEntitlementsUseCase {

    override suspend operator fun invoke(): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUserProfile() ?: return Result.success(Unit)
            if (currentUser.tier != SubscriptionTier.PRO) return Result.success(Unit)
            if (isComplimentaryAllowlistedUser(currentUser)) return Result.success(Unit)
            reconcileProUser(currentUser.userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isComplimentaryAllowlistedUser(user: User): Boolean {
        if (authenticationService.isAnonymous()) return false
        val email = user.email.takeIf(String::isNotBlank) ?: authenticationService.currentUserEmail()
        return User.isEmailInAllowlist(
            email = email,
            allowlistHashes = appConfigService.proAllowlistEmailHashes.value
        )
    }

    private suspend fun reconcileProUser(userId: String): Result<Unit> {
        return billingService.queryActivePurchases().fold(
            onSuccess = { activeProductIds ->
                val hasActivePro = activeProductIds.any { it in BillingConstants.PRO_PRODUCT_IDS }
                if (hasActivePro) {
                    Result.success(Unit)
                } else {
                    updateUserTierUseCase(userId, SubscriptionTier.FREE)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
