package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.service.featuregate.ManualReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private data class TierGroupLimits(
    val maxOwnedGroupsFree: Int,
    val maxOwnedGroupsPro: Int,
    val maxMembersPerGroupFree: Int,
    val maxMembersPerGroupPro: Int,
    val maxSubunitsPerGroupFree: Int,
    val maxSubunitsPerGroupPro: Int
)

private data class SubunitTierLimits(
    val freeLimit: Int,
    val proLimit: Int
)

internal class FeatureGateServiceImpl(
    private val authenticationService: AuthenticationService,
    private val appConfigRepository: AppConfigRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val yearMonthProvider: () -> YearMonth = { YearMonth.now() }
) : FeatureGateService {

    private data class TierLimitFlows(
        val freeLimitFlow: Flow<Int>,
        val proLimitFlow: Flow<Int>
    )

    override fun isFeatureEnabled(feature: GatedFeature, groupId: String?): Flow<Boolean> {
        if (feature == GatedFeature.AI_RECEIPT_SCANNING) {
            return observeAiReceiptQuota()
                .map { it.canScan }
                .distinctUntilChanged()
        }
        if (feature == GatedFeature.GROUP_COVER_UPLOAD) {
            return observeMediaUploadQuota()
                .map { it.canUpload }
                .distinctUntilChanged()
        }

        return combine(
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
                    GatedFeature.SUBUNIT_CREATION,
                    GatedFeature.TRIP_REPORT_EXPORT -> if (actingTier == SubscriptionTier.PRO) {
                        SubscriptionTier.PRO
                    } else {
                        resolveGroupCreatorTier(groupId, actingTier, allowlistHashes)
                    }
                }
                effectiveTier == SubscriptionTier.PRO
            }
        }.distinctUntilChanged()
    }

    override fun observeAiReceiptQuota(): Flow<AiReceiptQuota> =
        combine(
            appConfigRepository.subscriptionGatingEnabled,
            observeActingUserTier(),
            appConfigRepository.aiReceiptMonthlyLimitFree,
            appConfigRepository.aiReceiptMonthlyLimitPro,
            userPreferenceRepository.getMonthlyAiReceiptScanCount(yearMonthProvider())
        ) { gatingEnabled, actingTier, freeLimit, proLimit, usedScans ->
            val isPro = actingTier == SubscriptionTier.PRO
            val monthlyLimit = if (isPro) proLimit else freeLimit
            AiReceiptQuota(
                usedScans = usedScans.coerceAtLeast(0),
                monthlyLimit = monthlyLimit.coerceAtLeast(0),
                isProTier = isPro,
                isGatingEnabled = gatingEnabled
            )
        }.distinctUntilChanged()

    override fun observeManualReceiptQuota(): Flow<ManualReceiptQuota> =
        observeMediaUploadQuota()
            .map { quota ->
                ManualReceiptQuota(
                    usedAttachments = quota.usedUploads,
                    monthlyFreeLimit = quota.monthlyFreeLimit,
                    isProTier = quota.isProTier,
                    isGatingEnabled = quota.isGatingEnabled
                )
            }
            .distinctUntilChanged()

    override fun observeMediaUploadQuota(): Flow<MediaUploadQuota> =
        combine(
            appConfigRepository.subscriptionGatingEnabled,
            observeActingUserTier(),
            appConfigRepository.manualReceiptMonthlyLimitFree,
            userPreferenceRepository.getMonthlyManualReceiptAttachmentCount(yearMonthProvider())
        ) { gatingEnabled, actingTier, freeLimit, usedUploads ->
            MediaUploadQuota(
                usedUploads = usedUploads.coerceAtLeast(0),
                monthlyFreeLimit = freeLimit.coerceAtLeast(0),
                isProTier = actingTier == SubscriptionTier.PRO,
                isGatingEnabled = gatingEnabled
            )
        }.distinctUntilChanged()

    override fun observePlanAllowances(): Flow<PlanAllowances> =
        combine(
            observeAiReceiptQuota(),
            observeMediaUploadQuota(),
            groupRepository.getAllGroupsFlow(),
            observeTierGroupLimits()
        ) { aiQuota, mediaQuota, groups, limits ->
            buildPlanAllowances(
                aiQuota = aiQuota,
                mediaQuota = mediaQuota,
                groups = groups,
                currentUserId = authenticationService.currentUserId(),
                limits = limits
            )
        }.distinctUntilChanged()

    private fun observeTierGroupLimits(): Flow<TierGroupLimits> =
        combine(
            appConfigRepository.maxOwnedGroupsFree,
            appConfigRepository.maxOwnedGroupsPro,
            appConfigRepository.maxMembersPerGroupFree,
            appConfigRepository.maxMembersPerGroupPro,
            combine(
                appConfigRepository.maxSubunitsPerGroupFree,
                appConfigRepository.maxSubunitsPerGroupPro,
                ::SubunitTierLimits
            )
        ) { ownedFree, ownedPro, membersFree, membersPro, subunitLimits ->
            TierGroupLimits(
                maxOwnedGroupsFree = ownedFree,
                maxOwnedGroupsPro = ownedPro,
                maxMembersPerGroupFree = membersFree,
                maxMembersPerGroupPro = membersPro,
                maxSubunitsPerGroupFree = subunitLimits.freeLimit,
                maxSubunitsPerGroupPro = subunitLimits.proLimit
            )
        }

    override fun isActingUserPro(): Flow<Boolean> =
        observeActingUserTier()
            .map { it == SubscriptionTier.PRO }
            .distinctUntilChanged()

    override fun checkLimit(limit: GatedLimit, currentCount: Int, groupId: String?): Flow<LimitResult> {
        val (freeLimitFlow, proLimitFlow) = when (limit) {
            GatedLimit.MAX_OWNED_GROUPS_COUNT -> TierLimitFlows(
                freeLimitFlow = appConfigRepository.maxOwnedGroupsFree,
                proLimitFlow = appConfigRepository.maxOwnedGroupsPro
            )
            GatedLimit.MAX_MEMBERS_PER_GROUP -> TierLimitFlows(
                freeLimitFlow = appConfigRepository.maxMembersPerGroupFree,
                proLimitFlow = appConfigRepository.maxMembersPerGroupPro
            )
            GatedLimit.AI_RECEIPT_MONTHLY_SCANS -> TierLimitFlows(
                freeLimitFlow = appConfigRepository.aiReceiptMonthlyLimitFree,
                proLimitFlow = appConfigRepository.aiReceiptMonthlyLimitPro
            )
            GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS -> TierLimitFlows(
                freeLimitFlow = appConfigRepository.manualReceiptMonthlyLimitFree,
                proLimitFlow = flowOf(Int.MAX_VALUE)
            )
            GatedLimit.MAX_SUBUNITS_PER_GROUP -> TierLimitFlows(
                freeLimitFlow = appConfigRepository.maxSubunitsPerGroupFree,
                proLimitFlow = appConfigRepository.maxSubunitsPerGroupPro
            )
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
                    GatedLimit.MAX_OWNED_GROUPS_COUNT,
                    GatedLimit.AI_RECEIPT_MONTHLY_SCANS,
                    GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS -> actingTier
                    GatedLimit.MAX_MEMBERS_PER_GROUP ->
                        resolveGroupCreatorTier(groupId, actingTier, allowlistHashes)
                    GatedLimit.MAX_SUBUNITS_PER_GROUP -> if (actingTier == SubscriptionTier.PRO) {
                        SubscriptionTier.PRO
                    } else {
                        resolveGroupCreatorTier(groupId, actingTier, allowlistHashes)
                    }
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
        if (isUnlimitedProLimit(limit, effectiveTier, maxAllowed)) {
            return LimitResult.Allowed
        }
        return if (currentCount >= maxAllowed) {
            LimitResult.Blocked(limit, upgradeRequired = effectiveTier != SubscriptionTier.PRO)
        } else {
            LimitResult.Allowed
        }
    }

    private fun isUnlimitedProLimit(
        limit: GatedLimit,
        effectiveTier: SubscriptionTier,
        maxAllowed: Int
    ): Boolean {
        if (effectiveTier != SubscriptionTier.PRO) return false
        return limit == GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS ||
            (limit == GatedLimit.MAX_SUBUNITS_PER_GROUP && maxAllowed < 0)
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

private fun buildPlanAllowances(
    aiQuota: AiReceiptQuota,
    mediaQuota: MediaUploadQuota,
    groups: List<Group>,
    currentUserId: String?,
    limits: TierGroupLimits
): PlanAllowances {
    val isPro = aiQuota.isProTier
    val isGatingEnabled = aiQuota.isGatingEnabled
    val tier = if (isPro) SubscriptionTier.PRO else SubscriptionTier.FREE
    val ownedGroupsUsed = if (currentUserId.isNullOrBlank()) {
        0
    } else {
        groups.count { it.createdBy == currentUserId }
    }
    val maxOwnedGroups = (if (isPro) limits.maxOwnedGroupsPro else limits.maxOwnedGroupsFree)
        .coerceAtLeast(0)
    val maxMembersPerGroup = (if (isPro) limits.maxMembersPerGroupPro else limits.maxMembersPerGroupFree)
        .coerceAtLeast(0)

    return PlanAllowances(
        tier = tier,
        isGatingEnabled = isGatingEnabled,
        aiReceiptQuota = aiQuota,
        mediaUploadQuota = mediaQuota,
        ownedGroupsUsed = ownedGroupsUsed,
        maxOwnedGroups = maxOwnedGroups,
        maxMembersPerGroup = maxMembersPerGroup,
        maxSubunitsPerGroup = resolveMaxSubunitsPerGroup(isGatingEnabled, isPro, limits),
        isAdFreeEnabled = !isGatingEnabled || isPro
    )
}

private fun resolveMaxSubunitsPerGroup(
    isGatingEnabled: Boolean,
    isPro: Boolean,
    limits: TierGroupLimits
): Int? = when {
    !isGatingEnabled -> null
    isPro -> limits.maxSubunitsPerGroupPro.takeIf { it > 0 }
    else -> limits.maxSubunitsPerGroupFree.coerceAtLeast(0)
}
