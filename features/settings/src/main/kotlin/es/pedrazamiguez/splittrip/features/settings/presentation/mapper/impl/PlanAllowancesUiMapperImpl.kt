package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.PlanAllowancesUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceUsageStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.PlanAllowancesUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val FRACTION_SCALE = 4
private val APPROACHING_LIMIT_THRESHOLD = BigDecimal("0.65")
private val UNLIMITED_MAX_PROGRESS = BigDecimal("0.60")
private val UNLIMITED_PROGRESS_PIVOT = BigDecimal("5")

class PlanAllowancesUiMapperImpl(
    private val localeProvider: LocaleProvider
) : PlanAllowancesUiMapper {

    override fun map(
        allowances: PlanAllowances,
        extractionCapability: ExtractionCapability
    ): PlanAllowancesUiState {
        val isAiSupported = extractionCapability == ExtractionCapability.ON_DEVICE_AI
        val (tierTitle, tierSubtitle, tierBadge) = resolveTierHeader(allowances)

        return PlanAllowancesUiState(
            isLoading = false,
            isProTier = allowances.isProTier,
            isGatingEnabled = allowances.isGatingEnabled,
            tierTitle = tierTitle,
            tierSubtitle = tierSubtitle,
            tierBadge = tierBadge,
            quotas = mapQuotas(allowances = allowances, isAiSupported = isAiSupported),
            perks = mapPerks(allowances),
            showUpgradeCta = allowances.showUpgradeCta,
            upgradeDescription = resolveUpgradeDescription(isAiSupported)
        )
    }

    private fun resolveUpgradeDescription(isAiSupported: Boolean): UiText = UiText.StringResource(
        if (isAiSupported) {
            R.string.plan_allowances_upgrade_description
        } else {
            R.string.plan_allowances_upgrade_description_no_ai
        }
    )

    private data class TierHeaderDisplay(
        val title: UiText,
        val subtitle: UiText,
        val badge: UiText
    )

    private fun resolveTierHeader(allowances: PlanAllowances): TierHeaderDisplay = when {
        !allowances.isGatingEnabled -> TierHeaderDisplay(
            title = UiText.StringResource(R.string.plan_allowances_tier_ungated_title),
            subtitle = UiText.StringResource(R.string.plan_allowances_tier_ungated_subtitle),
            badge = UiText.StringResource(R.string.plan_allowances_badge_unlimited)
        )
        allowances.isProTier -> TierHeaderDisplay(
            title = UiText.StringResource(R.string.subscriptions_tier_pro_title),
            subtitle = UiText.StringResource(R.string.plan_allowances_tier_pro_subtitle),
            badge = UiText.StringResource(R.string.plan_allowances_badge_pro)
        )
        else -> TierHeaderDisplay(
            title = UiText.StringResource(R.string.subscriptions_tier_free_title),
            subtitle = UiText.StringResource(R.string.plan_allowances_tier_free_subtitle),
            badge = UiText.StringResource(R.string.plan_allowances_badge_free)
        )
    }

    private fun mapQuotas(
        allowances: PlanAllowances,
        isAiSupported: Boolean
    ): ImmutableList<AllowanceQuotaUiModel> {
        val aiQuota = if (isAiSupported) {
            mapQuota(
                type = AllowanceQuotaType.AI_RECEIPT_SCANS,
                titleRes = R.string.plan_allowances_quota_ai_scans_title,
                subtitleRes = R.string.plan_allowances_quota_ai_scans_subtitle,
                used = allowances.aiReceiptQuota.usedScans,
                limit = allowances.aiReceiptQuota.monthlyLimit,
                isGatingEnabled = allowances.isGatingEnabled
            )
        } else {
            createUnsupportedAiQuota()
        }

        return persistentListOf(
            aiQuota,
            mapQuota(
                type = AllowanceQuotaType.MEDIA_UPLOADS,
                titleRes = R.string.plan_allowances_quota_media_uploads_title,
                subtitleRes = R.string.plan_allowances_quota_media_uploads_subtitle,
                used = allowances.mediaUploadQuota.usedUploads,
                limit = allowances.mediaUploadQuota.monthlyLimit,
                isGatingEnabled = allowances.isGatingEnabled
            ),
            mapQuota(
                type = AllowanceQuotaType.REPORT_EXPORTS,
                titleRes = R.string.plan_allowances_quota_report_exports_title,
                subtitleRes = if (allowances.reportExportQuota.isUnlimited) {
                    R.string.plan_allowances_quota_report_exports_subtitle_pro
                } else {
                    R.string.plan_allowances_quota_report_exports_subtitle_free
                },
                used = allowances.reportExportQuota.usedExports,
                limit = allowances.reportExportQuota.monthlyLimit,
                isGatingEnabled = allowances.isGatingEnabled
            ),
            mapQuota(
                type = AllowanceQuotaType.OWNED_GROUPS,
                titleRes = R.string.plan_allowances_quota_owned_groups_title,
                subtitleRes = R.string.plan_allowances_quota_owned_groups_subtitle,
                used = allowances.ownedGroupsUsed,
                limit = allowances.maxOwnedGroups,
                isGatingEnabled = allowances.isGatingEnabled
            )
        )
    }

    private fun createUnsupportedAiQuota(): AllowanceQuotaUiModel = AllowanceQuotaUiModel(
        type = AllowanceQuotaType.AI_RECEIPT_SCANS,
        title = UiText.StringResource(R.string.plan_allowances_quota_ai_scans_title),
        subtitle = UiText.StringResource(R.string.plan_allowances_quota_ai_scans_subtitle_unsupported),
        usageText = null,
        statusBadge = UiText.StringResource(R.string.plan_allowances_status_not_supported),
        status = AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE,
        progressFraction = 0f,
        showProgressBar = false
    )

    private fun mapQuota(
        type: AllowanceQuotaType,
        titleRes: Int,
        subtitleRes: Int,
        used: Int,
        limit: Int?,
        isGatingEnabled: Boolean
    ): AllowanceQuotaUiModel {
        val safeUsed = used.coerceAtLeast(0)
        if (!isGatingEnabled || limit == null || limit < 0) {
            return AllowanceQuotaUiModel(
                type = type,
                title = UiText.StringResource(titleRes),
                subtitle = UiText.StringResource(subtitleRes),
                usageText = UiText.StringResource(R.string.plan_allowances_usage_unlimited, formatInt(safeUsed)),
                statusBadge = UiText.StringResource(R.string.plan_allowances_badge_unlimited),
                status = AllowanceUsageStatus.UNLIMITED,
                progressFraction = computeUnlimitedProgressFraction(safeUsed),
                showProgressBar = true
            )
        }

        val ratio = if (limit <= 0) {
            if (safeUsed > 0) BigDecimal.ONE else BigDecimal.ZERO
        } else {
            BigDecimal(safeUsed)
                .divide(BigDecimal(limit), FRACTION_SCALE, RoundingMode.HALF_UP)
                .coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
        }

        val status = when {
            limit <= 0 || safeUsed >= limit -> AllowanceUsageStatus.EXHAUSTED
            ratio >= APPROACHING_LIMIT_THRESHOLD -> AllowanceUsageStatus.APPROACHING_LIMIT
            else -> AllowanceUsageStatus.HEALTHY
        }

        val remaining = (limit - safeUsed).coerceAtLeast(0)
        val statusBadge = when (status) {
            AllowanceUsageStatus.EXHAUSTED -> UiText.StringResource(R.string.plan_allowances_status_exhausted)
            AllowanceUsageStatus.HEALTHY,
            AllowanceUsageStatus.APPROACHING_LIMIT -> UiText.PluralResource(
                resId = R.plurals.plan_allowances_status_remaining,
                quantity = remaining,
                formatInt(remaining)
            )
            AllowanceUsageStatus.UNLIMITED -> UiText.StringResource(R.string.plan_allowances_badge_unlimited)
            AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE ->
                UiText.StringResource(R.string.plan_allowances_status_not_supported)
        }

        return AllowanceQuotaUiModel(
            type = type,
            title = UiText.StringResource(titleRes),
            subtitle = UiText.StringResource(subtitleRes),
            usageText = UiText.StringResource(
                R.string.plan_allowances_usage_fraction,
                formatInt(safeUsed),
                formatInt(limit)
            ),
            statusBadge = statusBadge,
            status = status,
            progressFraction = ratio.toFloat(),
            showProgressBar = true
        )
    }

    private fun mapPerks(allowances: PlanAllowances): ImmutableList<AllowancePerkUiModel> {
        val membersDetail = if (!allowances.isGatingEnabled) {
            UiText.StringResource(R.string.plan_allowances_badge_unlimited)
        } else {
            UiText.StringResource(
                R.string.plan_allowances_perk_members_detail,
                formatInt(allowances.maxMembersPerGroup)
            )
        }

        val maxSubunits = allowances.maxSubunitsPerGroup
        val subunitsIncluded: Boolean
        val subunitsDetail: UiText
        when {
            !allowances.isGatingEnabled || maxSubunits == null -> {
                subunitsIncluded = true
                subunitsDetail = UiText.StringResource(R.string.plan_allowances_badge_unlimited)
            }
            maxSubunits > 0 -> {
                subunitsIncluded = true
                subunitsDetail = UiText.StringResource(
                    R.string.plan_allowances_perk_subunits_detail,
                    formatInt(maxSubunits)
                )
            }
            else -> {
                subunitsIncluded = false
                subunitsDetail = UiText.StringResource(R.string.plan_allowances_perk_status_pro_only)
            }
        }

        return persistentListOf(
            AllowancePerkUiModel(
                type = AllowancePerkType.MEMBERS_PER_GROUP,
                title = UiText.StringResource(R.string.plan_allowances_perk_members_title),
                detail = membersDetail,
                isIncluded = true
            ),
            AllowancePerkUiModel(
                type = AllowancePerkType.SUBUNITS_PER_GROUP,
                title = UiText.StringResource(R.string.plan_allowances_perk_subunits_title),
                detail = subunitsDetail,
                isIncluded = subunitsIncluded
            ),
            AllowancePerkUiModel(
                type = AllowancePerkType.AD_FREE,
                title = UiText.StringResource(R.string.plan_allowances_perk_ad_free_title),
                detail = includedOrProBadge(allowances.isAdFreeEnabled),
                isIncluded = allowances.isAdFreeEnabled
            )
        )
    }

    private fun includedOrProBadge(isIncluded: Boolean): UiText = UiText.StringResource(
        if (isIncluded) R.string.plan_allowances_perk_status_included else R.string.plan_allowances_perk_status_pro_only
    )

    private fun computeUnlimitedProgressFraction(used: Int): Float {
        if (used <= 0) return 0f
        val usedDecimal = BigDecimal(used)
        val denominator = usedDecimal.add(UNLIMITED_PROGRESS_PIVOT)
        return UNLIMITED_MAX_PROGRESS
            .multiply(usedDecimal)
            .divide(denominator, FRACTION_SCALE, RoundingMode.HALF_UP)
            .coerceIn(BigDecimal.ZERO, UNLIMITED_MAX_PROGRESS)
            .toFloat()
    }

    private fun formatInt(value: Int): String =
        NumberFormat.getIntegerInstance(localeProvider.getCurrentLocale()).format(value)
}
