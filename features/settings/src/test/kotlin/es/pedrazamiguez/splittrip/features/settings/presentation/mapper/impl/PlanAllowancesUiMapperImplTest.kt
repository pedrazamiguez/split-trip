package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.domain.service.featuregate.ReportExportQuota
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceUsageStatus
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlanAllowancesUiMapperImplTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var mapper: PlanAllowancesUiMapperImpl

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.ENGLISH
        mapper = PlanAllowancesUiMapperImpl(localeProvider = localeProvider)
    }

    @Test
    fun `map Free healthy state sets Free header, 3 healthy quotas, 2 allowances, locked ad-free, and upgrade CTA`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = true,
            usedScans = 1,
            aiMonthlyLimit = 3,
            usedUploads = 2,
            mediaMonthlyFreeLimit = 5,
            ownedGroupsUsed = 0,
            maxOwnedGroups = 1,
            maxMembersPerGroup = 4,
            maxSubunitsPerGroup = 1,
            isAdFreeEnabled = false
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        assertFalse(result.isLoading)
        assertFalse(result.isProTier)
        assertTrue(result.isGatingEnabled)
        assertTrue(result.showUpgradeCta)
        assertEquals(UiText.StringResource(R.string.plan_allowances_upgrade_description), result.upgradeDescription)
        assertEquals(UiText.StringResource(R.string.subscriptions_tier_free_title), result.tierTitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_free_subtitle), result.tierSubtitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_free), result.tierBadge)

        assertEquals(4, result.quotas.size)
        assertHealthyQuota(
            quota = result.quotas[0],
            expectedType = AllowanceQuotaType.AI_RECEIPT_SCANS,
            expectedProgress = 0.3333f,
            expectedUsage = UiText.StringResource(R.string.plan_allowances_usage_fraction, "1", "3"),
            expectedBadge = UiText.PluralResource(R.plurals.plan_allowances_status_remaining, 2, "2")
        )
        assertHealthyQuota(
            quota = result.quotas[1],
            expectedType = AllowanceQuotaType.MEDIA_UPLOADS,
            expectedProgress = 0.4000f,
            expectedUsage = UiText.StringResource(R.string.plan_allowances_usage_fraction, "2", "5"),
            expectedBadge = UiText.PluralResource(R.plurals.plan_allowances_status_remaining, 3, "3")
        )
        assertHealthyQuota(
            quota = result.quotas[2],
            expectedType = AllowanceQuotaType.REPORT_EXPORTS,
            expectedProgress = 0f,
            expectedUsage = UiText.StringResource(R.string.plan_allowances_usage_fraction, "0", "1"),
            expectedBadge = UiText.PluralResource(R.plurals.plan_allowances_status_remaining, 1, "1")
        )
        assertHealthyQuota(
            quota = result.quotas[3],
            expectedType = AllowanceQuotaType.OWNED_GROUPS,
            expectedProgress = 0f,
            expectedUsage = UiText.StringResource(R.string.plan_allowances_usage_fraction, "0", "1"),
            expectedBadge = UiText.PluralResource(R.plurals.plan_allowances_status_remaining, 1, "1")
        )

        assertEquals(3, result.perks.size)
        assertFreePerks(result.perks)
    }

    private fun assertFreePerks(perks: List<AllowancePerkUiModel>) {
        assertEquals(AllowancePerkType.MEMBERS_PER_GROUP, perks[0].type)
        assertTrue(perks[0].isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_members_detail, "4"), perks[0].detail)

        assertEquals(AllowancePerkType.SUBUNITS_PER_GROUP, perks[1].type)
        assertTrue(perks[1].isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_subunits_detail, "1"), perks[1].detail)

        assertEquals(AllowancePerkType.AD_FREE, perks[2].type)
        assertFalse(perks[2].isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_status_pro_only), perks[2].detail)
    }

    @Test
    fun `map Free tier approaching limit sets APPROACHING_LIMIT for 2 of 3 AI scans and 4 of 5 media uploads`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            usedScans = 2,
            aiMonthlyLimit = 3,
            usedUploads = 4,
            mediaMonthlyFreeLimit = 5
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.APPROACHING_LIMIT, aiQuota.status)
        assertEquals(0.6667f, aiQuota.progressFraction, 0.0001f)
        assertEquals(
            UiText.PluralResource(R.plurals.plan_allowances_status_remaining, 1, "1"),
            aiQuota.statusBadge
        )

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.APPROACHING_LIMIT, mediaQuota.status)
        assertEquals(0.8000f, mediaQuota.progressFraction, 0.0001f)
        assertEquals(
            UiText.PluralResource(R.plurals.plan_allowances_status_remaining, 1, "1"),
            mediaQuota.statusBadge
        )
    }

    @Test
    fun `map Free tier exhausted state sets EXHAUSTED status and Limit reached badges across all 3 quotas`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            usedScans = 3,
            aiMonthlyLimit = 3,
            usedUploads = 5,
            mediaMonthlyFreeLimit = 5,
            usedExports = 1,
            reportExportMonthlyFreeLimit = 1,
            ownedGroupsUsed = 1,
            maxOwnedGroups = 1
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        assertTrue(result.showUpgradeCta)
        result.quotas.forEach { quota ->
            assertEquals(AllowanceUsageStatus.EXHAUSTED, quota.status)
            assertEquals(1f, quota.progressFraction, 0.0001f)
            assertTrue(quota.showProgressBar)
            assertEquals(UiText.StringResource(R.string.plan_allowances_status_exhausted), quota.statusBadge)
        }
    }

    @Test
    fun `map zero limit when gated sets EXHAUSTED with zero progress fraction safely without division by zero`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = true,
            usedScans = 0,
            aiMonthlyLimit = 0
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.EXHAUSTED, aiQuota.status)
        assertEquals(0f, aiQuota.progressFraction, 0.0001f)
        assertTrue(aiQuota.showProgressBar)
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_exhausted), aiQuota.statusBadge)
    }

    @Test
    fun `map zero subunit allowance on Free falls back to Pro-only locked perk`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = true,
            maxSubunitsPerGroup = 0
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        val subunitsPerk = result.perks.first { it.type == AllowancePerkType.SUBUNITS_PER_GROUP }
        assertFalse(subunitsPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_status_pro_only), subunitsPerk.detail)
    }

    @Test
    fun `map over-limit usage clamps progress fraction to 1f`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            usedUploads = 7,
            mediaMonthlyFreeLimit = 5,
            ownedGroupsUsed = 2,
            maxOwnedGroups = 1
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.EXHAUSTED, mediaQuota.status)
        assertEquals(1f, mediaQuota.progressFraction, 0.0001f)

        val groupsQuota = result.quotas[3]
        assertEquals(AllowanceUsageStatus.EXHAUSTED, groupsQuota.status)
        assertEquals(1f, groupsQuota.progressFraction, 0.0001f)
    }

    @Test
    fun `map Pro tier sets Pro header, quota bars, UNLIMITED media, Unlimited subunits, ad-free, and no CTA`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.PRO,
            isGatingEnabled = true,
            usedScans = 12,
            aiMonthlyLimit = null,
            usedUploads = 18,
            mediaMonthlyFreeLimit = 5,
            usedExports = 0,
            ownedGroupsUsed = 4,
            maxOwnedGroups = null,
            maxMembersPerGroup = 20,
            maxSubunitsPerGroup = null,
            isAdFreeEnabled = true
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        assertTrue(result.isProTier)
        assertFalse(result.showUpgradeCta)
        assertEquals(UiText.StringResource(R.string.subscriptions_tier_pro_title), result.tierTitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_pro_subtitle), result.tierSubtitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_pro), result.tierBadge)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.UNLIMITED, aiQuota.status)
        assertTrue(aiQuota.showProgressBar)
        assertEquals(0.4235f, aiQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_unlimited, "12"), aiQuota.usageText)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.UNLIMITED, mediaQuota.status)
        assertTrue(mediaQuota.showProgressBar)
        assertEquals(0.4696f, mediaQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_unlimited, "18"), mediaQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), mediaQuota.statusBadge)

        val reportExportQuota = result.quotas[2]
        assertEquals(AllowanceQuotaType.REPORT_EXPORTS, reportExportQuota.type)
        assertEquals(AllowanceUsageStatus.UNLIMITED, reportExportQuota.status)
        assertTrue(reportExportQuota.showProgressBar)
        assertEquals(0f, reportExportQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_unlimited, "0"), reportExportQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), reportExportQuota.statusBadge)

        val groupsQuota = result.quotas[3]
        assertEquals(AllowanceUsageStatus.UNLIMITED, groupsQuota.status)
        assertTrue(groupsQuota.showProgressBar)
        assertEquals(0.2667f, groupsQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_unlimited, "4"), groupsQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), groupsQuota.statusBadge)

        val membersPerk = result.perks[0]
        assertTrue(membersPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_members_detail, "20"), membersPerk.detail)

        val subunitsPerk = result.perks[1]
        assertTrue(subunitsPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), subunitsPerk.detail)

        val adFreePerk = result.perks[2]
        assertTrue(adFreePerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_status_included), adFreePerk.detail)
    }

    @Test
    fun `map unlimited quota with zero usage shows progress bar at zero fraction`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.PRO,
            isGatingEnabled = true,
            usedUploads = 0
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.UNLIMITED, mediaQuota.status)
        assertTrue(mediaQuota.showProgressBar)
        assertEquals(0f, mediaQuota.progressFraction, 0.0001f)
    }

    @Test
    fun `map unlimited quota with positive usage bounds progress fraction strictly below 1f`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.PRO,
            isGatingEnabled = true,
            usedScans = 1000,
            aiMonthlyLimit = null
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.UNLIMITED, aiQuota.status)
        assertTrue(aiQuota.showProgressBar)
        assertTrue(aiQuota.progressFraction > 0f && aiQuota.progressFraction < 1f)
        assertTrue(aiQuota.progressFraction in 0.59f..0.60f)
    }

    @Test
    fun `map ungated state shows progress bars, uses UNLIMITED status, unlocks ad-free, and hides CTA`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = false,
            usedScans = 2,
            aiMonthlyLimit = 3,
            usedUploads = 3,
            mediaMonthlyFreeLimit = 5,
            ownedGroupsUsed = 2,
            maxOwnedGroups = 1,
            maxMembersPerGroup = 4,
            maxSubunitsPerGroup = null,
            isAdFreeEnabled = true
        )

        val result = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)

        assertFalse(result.isGatingEnabled)
        assertFalse(result.showUpgradeCta)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_ungated_title), result.tierTitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_ungated_subtitle), result.tierSubtitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), result.tierBadge)

        result.quotas.forEach { quota ->
            assertEquals(AllowanceUsageStatus.UNLIMITED, quota.status)
            assertTrue(quota.showProgressBar)
            assertTrue(quota.progressFraction >= 0f && quota.progressFraction < 1f)
            assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), quota.statusBadge)
        }

        val membersPerk = result.perks[0]
        assertTrue(membersPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), membersPerk.detail)

        val subunitsPerk = result.perks[1]
        assertTrue(subunitsPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), subunitsPerk.detail)

        val adFreePerk = result.perks[2]
        assertTrue(adFreePerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_status_included), adFreePerk.detail)
    }

    @Test
    fun `map formats large numbers using LocaleProvider locale`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.PRO,
            usedScans = 1000,
            aiMonthlyLimit = 2000
        )

        every { localeProvider.getCurrentLocale() } returns Locale.ENGLISH
        val enResult = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)
        assertEquals(
            UiText.StringResource(R.string.plan_allowances_usage_fraction, "1,000", "2,000"),
            enResult.quotas[0].usageText
        )

        every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
        val esResult = mapper.map(allowances, ExtractionCapability.ON_DEVICE_AI)
        assertEquals(
            UiText.StringResource(R.string.plan_allowances_usage_fraction, "1.000", "2.000"),
            esResult.quotas[0].usageText
        )
    }

    @Test
    fun `map Free tier on UNSUPPORTED device sets AI quota to UNSUPPORTED_ON_DEVICE and adapts CTA`() {
        val allowances = createAllowances(tier = SubscriptionTier.FREE)

        val result = mapper.map(allowances, ExtractionCapability.UNSUPPORTED)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceQuotaType.AI_RECEIPT_SCANS, aiQuota.type)
        assertEquals(AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE, aiQuota.status)
        assertEquals(
            UiText.StringResource(R.string.plan_allowances_quota_ai_scans_subtitle_unsupported),
            aiQuota.subtitle
        )
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_not_supported), aiQuota.statusBadge)
        assertNull(aiQuota.usageText)
        assertEquals(0f, aiQuota.progressFraction, 0.0001f)
        assertFalse(aiQuota.showProgressBar)

        assertEquals(AllowanceUsageStatus.HEALTHY, result.quotas[1].status)
        assertEquals(AllowanceUsageStatus.HEALTHY, result.quotas[2].status)
        assertEquals(AllowanceUsageStatus.HEALTHY, result.quotas[3].status)
        assertEquals(
            UiText.StringResource(R.string.plan_allowances_upgrade_description_no_ai),
            result.upgradeDescription
        )
    }

    @Test
    fun `map Pro tier on UNSUPPORTED device shows AI quota as UNSUPPORTED_ON_DEVICE instead of UNLIMITED`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.PRO,
            usedScans = 0,
            aiMonthlyLimit = null,
            maxOwnedGroups = null,
            maxSubunitsPerGroup = null
        )

        val result = mapper.map(allowances, ExtractionCapability.UNSUPPORTED)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE, aiQuota.status)
        assertNull(aiQuota.usageText)
        assertFalse(aiQuota.showProgressBar)
        assertEquals(AllowanceUsageStatus.UNLIMITED, result.quotas[1].status)
        assertEquals(AllowanceUsageStatus.UNLIMITED, result.quotas[2].status)
        assertEquals(AllowanceUsageStatus.UNLIMITED, result.quotas[3].status)
    }

    @Test
    fun `map ungated state on UNSUPPORTED device shows AI quota as UNSUPPORTED_ON_DEVICE instead of UNLIMITED`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            isGatingEnabled = false,
            maxSubunitsPerGroup = null
        )

        val result = mapper.map(allowances, ExtractionCapability.UNSUPPORTED)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.UNSUPPORTED_ON_DEVICE, aiQuota.status)
        assertFalse(aiQuota.showProgressBar)
        assertEquals(AllowanceUsageStatus.UNLIMITED, result.quotas[1].status)
        assertEquals(AllowanceUsageStatus.UNLIMITED, result.quotas[2].status)
        assertEquals(AllowanceUsageStatus.UNLIMITED, result.quotas[3].status)
    }

    private fun assertHealthyQuota(
        quota: AllowanceQuotaUiModel,
        expectedType: AllowanceQuotaType,
        expectedProgress: Float,
        expectedUsage: UiText,
        expectedBadge: UiText
    ) {
        assertEquals(expectedType, quota.type)
        assertEquals(AllowanceUsageStatus.HEALTHY, quota.status)
        assertEquals(expectedProgress, quota.progressFraction, 0.0001f)
        assertTrue(quota.showProgressBar)
        assertEquals(expectedUsage, quota.usageText)
        assertEquals(expectedBadge, quota.statusBadge)
    }

    @Suppress("LongParameterList")
    private fun createAllowances(
        tier: SubscriptionTier = SubscriptionTier.FREE,
        isGatingEnabled: Boolean = true,
        usedScans: Int = 1,
        aiMonthlyLimit: Int? = 3,
        usedUploads: Int = 2,
        mediaMonthlyFreeLimit: Int = 5,
        usedExports: Int = 0,
        reportExportMonthlyFreeLimit: Int = 1,
        ownedGroupsUsed: Int = 0,
        maxOwnedGroups: Int? = 1,
        maxMembersPerGroup: Int = 4,
        maxSubunitsPerGroup: Int? = 1,
        isAdFreeEnabled: Boolean = tier == SubscriptionTier.PRO || !isGatingEnabled
    ): PlanAllowances {
        val isPro = tier == SubscriptionTier.PRO
        return PlanAllowances(
            tier = tier,
            isGatingEnabled = isGatingEnabled,
            aiReceiptQuota = AiReceiptQuota(
                usedScans = usedScans,
                monthlyLimit = aiMonthlyLimit,
                isProTier = isPro,
                isGatingEnabled = isGatingEnabled
            ),
            mediaUploadQuota = MediaUploadQuota(
                usedUploads = usedUploads,
                monthlyFreeLimit = mediaMonthlyFreeLimit,
                isProTier = isPro,
                isGatingEnabled = isGatingEnabled
            ),
            reportExportQuota = ReportExportQuota(
                usedExports = usedExports,
                monthlyFreeLimit = reportExportMonthlyFreeLimit,
                isProTier = isPro,
                isGatingEnabled = isGatingEnabled
            ),
            ownedGroupsUsed = ownedGroupsUsed,
            maxOwnedGroups = maxOwnedGroups,
            maxMembersPerGroup = maxMembersPerGroup,
            maxSubunitsPerGroup = maxSubunitsPerGroup,
            isAdFreeEnabled = isAdFreeEnabled
        )
    }
}
