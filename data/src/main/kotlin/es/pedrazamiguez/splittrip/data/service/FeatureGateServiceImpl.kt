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
import kotlinx.coroutines.flow.flow

internal class FeatureGateServiceImpl(
    private val authenticationService: AuthenticationService,
    private val appConfigRepository: AppConfigRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) : FeatureGateService {

    override fun isFeatureEnabled(feature: GatedFeature, groupId: String?): Flow<Boolean> = flow {
        if (!appConfigRepository.subscriptionGatingEnabled.value) {
            emit(true)
            return@flow
        }

        val effectiveTier = when (feature) {
            GatedFeature.AI_RECEIPT_SCANNING,
            GatedFeature.AD_FREE -> resolveActingUserTier()
            GatedFeature.GROUP_COVER_UPLOAD -> resolveEffectiveTier(groupId)
            GatedFeature.SUBUNIT_CREATION -> resolveSubunitCreationTier(groupId)
        }

        emit(effectiveTier == SubscriptionTier.PRO)
    }

    override fun isActingUserPro(): Flow<Boolean> = flow {
        emit(resolveActingUserTier() == SubscriptionTier.PRO)
    }

    override fun checkLimit(limit: GatedLimit, currentCount: Int, groupId: String?): Flow<LimitResult> = flow {
        if (!appConfigRepository.subscriptionGatingEnabled.value) {
            emit(LimitResult.Allowed)
            return@flow
        }

        val (maxAllowed, tier) = resolveLimitThreshold(limit, groupId)

        if (currentCount >= maxAllowed) {
            emit(LimitResult.Blocked(limit, upgradeRequired = tier != SubscriptionTier.PRO))
        } else {
            emit(LimitResult.Allowed)
        }
    }

    private suspend fun resolveLimitThreshold(
        limit: GatedLimit,
        groupId: String?
    ): Pair<Int, SubscriptionTier> = when (limit) {
        GatedLimit.MAX_OWNED_GROUPS_COUNT -> resolveOwnedGroupsLimit()
        GatedLimit.MAX_MEMBERS_PER_GROUP -> resolveMembersLimit(groupId)
    }

    private suspend fun resolveOwnedGroupsLimit(): Pair<Int, SubscriptionTier> {
        val actingTier = resolveActingUserTier()
        val max = if (actingTier == SubscriptionTier.PRO) {
            appConfigRepository.maxOwnedGroupsPro.value
        } else {
            appConfigRepository.maxOwnedGroupsFree.value
        }
        return max to actingTier
    }

    private suspend fun resolveMembersLimit(groupId: String?): Pair<Int, SubscriptionTier> {
        val groupTier = resolveEffectiveTier(groupId)
        val max = if (groupTier == SubscriptionTier.PRO) {
            appConfigRepository.maxMembersPerGroupPro.value
        } else {
            appConfigRepository.maxMembersPerGroupFree.value
        }
        return max to groupTier
    }

    private suspend fun resolveActingUserTier(): SubscriptionTier {
        if (authenticationService.isAnonymous()) {
            return SubscriptionTier.FREE
        }
        return runCatching {
            userRepository.getCurrentUserProfile()?.tier
        }.getOrNull() ?: SubscriptionTier.FREE
    }

    private suspend fun resolveSubunitCreationTier(groupId: String?): SubscriptionTier {
        if (resolveActingUserTier() == SubscriptionTier.PRO) {
            return SubscriptionTier.PRO
        }
        return resolveEffectiveTier(groupId)
    }

    private suspend fun resolveEffectiveTier(groupId: String?): SubscriptionTier {
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
        return resolveActingUserTier()
    }
}
