package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class FeatureGateServiceImpl(
    private val authenticationService: AuthenticationService,
    private val appConfigRepository: AppConfigRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : FeatureGateService {

    override fun isFeatureEnabled(feature: GatedFeature, groupId: String?): Flow<Boolean> =
        combine(
            appConfigRepository.subscriptionGatingEnabled,
            observeActingUserTier()
        ) { gatingEnabled, actingTier ->
            if (!gatingEnabled) {
                true
            } else {
                val effectiveTier = when (feature) {
                    GatedFeature.AI_RECEIPT_SCANNING,
                    GatedFeature.AD_FREE -> actingTier
                    GatedFeature.GROUP_COVER_UPLOAD -> resolveGroupCreatorTier(groupId, actingTier)
                    GatedFeature.SUBUNIT_CREATION -> if (actingTier == SubscriptionTier.PRO) {
                        SubscriptionTier.PRO
                    } else {
                        resolveGroupCreatorTier(groupId, actingTier)
                    }
                }
                effectiveTier == SubscriptionTier.PRO
            }
        }.distinctUntilChanged()

    override fun isActingUserPro(): Flow<Boolean> =
        observeActingUserTier()
            .map { it == SubscriptionTier.PRO }
            .distinctUntilChanged()

    override fun checkLimit(limit: GatedLimit, currentCount: Int, groupId: String?): Flow<LimitResult> {
        val (freeLimitFlow, proLimitFlow) = when (limit) {
            GatedLimit.MAX_OWNED_GROUPS_COUNT ->
                appConfigRepository.maxOwnedGroupsFree to appConfigRepository.maxOwnedGroupsPro
            GatedLimit.MAX_MEMBERS_PER_GROUP ->
                appConfigRepository.maxMembersPerGroupFree to appConfigRepository.maxMembersPerGroupPro
        }

        return combine(
            appConfigRepository.subscriptionGatingEnabled,
            observeActingUserTier(),
            freeLimitFlow,
            proLimitFlow
        ) { gatingEnabled, actingTier, freeLimit, proLimit ->
            evaluateLimit(
                gatingEnabled = gatingEnabled,
                limit = limit,
                currentCount = currentCount,
                groupId = groupId,
                actingTier = actingTier,
                freeLimit = freeLimit,
                proLimit = proLimit
            )
        }.distinctUntilChanged()
    }

    private suspend fun evaluateLimit(
        gatingEnabled: Boolean,
        limit: GatedLimit,
        currentCount: Int,
        groupId: String?,
        actingTier: SubscriptionTier,
        freeLimit: Int,
        proLimit: Int
    ): LimitResult {
        if (!gatingEnabled) return LimitResult.Allowed
        val effectiveTier = when (limit) {
            GatedLimit.MAX_OWNED_GROUPS_COUNT -> actingTier
            GatedLimit.MAX_MEMBERS_PER_GROUP -> resolveGroupCreatorTier(groupId, actingTier)
        }
        val maxAllowed = if (effectiveTier == SubscriptionTier.PRO) proLimit else freeLimit
        return if (currentCount >= maxAllowed) {
            LimitResult.Blocked(limit, upgradeRequired = effectiveTier != SubscriptionTier.PRO)
        } else {
            LimitResult.Allowed
        }
    }

    private fun observeActingUserTier(): Flow<SubscriptionTier> =
        userRepository.observeCurrentUserProfile()
            .map { profile ->
                if (authenticationService.isAnonymous()) {
                    SubscriptionTier.FREE
                } else {
                    profile?.tier ?: SubscriptionTier.FREE
                }
            }
            .distinctUntilChanged()

    private suspend fun resolveGroupCreatorTier(
        groupId: String?,
        fallbackTier: SubscriptionTier
    ): SubscriptionTier {
        if (groupId != null) {
            val group = runCatching { groupRepository.getGroupById(groupId) }.getOrNull()
            if (group != null && group.createdBy.isNotBlank()) {
                val creator = runCatching {
                    userRepository.getUsersByIds(listOf(group.createdBy))[group.createdBy]
                }.getOrNull()
                if (creator != null) {
                    return creator.tier
                }
            }
        }
        return fallbackTier
    }
}
