package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.User
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
            observeActingUserTier(),
            appConfigRepository.proAllowlistEmailHashes
        ) { gatingEnabled, actingTier, allowlistHashes ->
            if (!gatingEnabled) {
                true
            } else {
                val effectiveTier = when (feature) {
                    GatedFeature.AI_RECEIPT_SCANNING,
                    GatedFeature.AD_FREE -> actingTier
                    GatedFeature.GROUP_COVER_UPLOAD ->
                        resolveGroupCreatorTier(groupId, actingTier, allowlistHashes)
                    GatedFeature.SUBUNIT_CREATION -> if (actingTier == SubscriptionTier.PRO) {
                        SubscriptionTier.PRO
                    } else {
                        resolveGroupCreatorTier(groupId, actingTier, allowlistHashes)
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
            appConfigRepository.proAllowlistEmailHashes,
            freeLimitFlow,
            proLimitFlow
        ) { gatingEnabled, actingTier, allowlistHashes, freeLimit, proLimit ->
            if (!gatingEnabled) {
                LimitResult.Allowed
            } else {
                val effectiveTier = when (limit) {
                    GatedLimit.MAX_OWNED_GROUPS_COUNT -> actingTier
                    GatedLimit.MAX_MEMBERS_PER_GROUP ->
                        resolveGroupCreatorTier(groupId, actingTier, allowlistHashes)
                }
                evaluateLimit(
                    limit = limit,
                    currentCount = currentCount,
                    effectiveTier = effectiveTier,
                    freeLimit = freeLimit,
                    proLimit = proLimit
                )
            }
        }.distinctUntilChanged()
    }

    private fun evaluateLimit(
        limit: GatedLimit,
        currentCount: Int,
        effectiveTier: SubscriptionTier,
        freeLimit: Int,
        proLimit: Int
    ): LimitResult {
        val maxAllowed = if (effectiveTier == SubscriptionTier.PRO) proLimit else freeLimit
        return if (currentCount >= maxAllowed) {
            LimitResult.Blocked(limit, upgradeRequired = effectiveTier != SubscriptionTier.PRO)
        } else {
            LimitResult.Allowed
        }
    }

    private fun observeActingUserTier(): Flow<SubscriptionTier> =
        combine(
            userRepository.observeCurrentUserProfile(),
            appConfigRepository.proAllowlistEmailHashes
        ) { profile, allowlistHashes ->
            if (authenticationService.isAnonymous()) {
                SubscriptionTier.FREE
            } else {
                val actingEmail = profile?.email?.takeIf(String::isNotBlank)
                    ?: authenticationService.currentUserEmail()
                if (
                    profile?.tier == SubscriptionTier.PRO ||
                    User.isEmailInAllowlist(actingEmail, allowlistHashes)
                ) {
                    SubscriptionTier.PRO
                } else {
                    SubscriptionTier.FREE
                }
            }
        }.distinctUntilChanged()

    private suspend fun resolveGroupCreatorTier(
        groupId: String?,
        fallbackTier: SubscriptionTier,
        proAllowlistEmailHashes: Set<String>
    ): SubscriptionTier {
        if (groupId == null) return fallbackTier
        val group = runCatching { groupRepository.getGroupById(groupId) }.getOrNull() ?: return fallbackTier
        if (group.createdBy.isBlank()) return fallbackTier
        val creator = runCatching {
            userRepository.getUsersByIds(listOf(group.createdBy))[group.createdBy]
        }.getOrNull() ?: return fallbackTier
        val isAllowlisted = !creator.isPending && User.isEmailInAllowlist(creator.email, proAllowlistEmailHashes)
        return if (creator.tier == SubscriptionTier.PRO || isAllowlisted) {
            SubscriptionTier.PRO
        } else {
            creator.tier
        }
    }
}
