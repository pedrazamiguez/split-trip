package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.MediaUploadQuota
import es.pedrazamiguez.splittrip.domain.service.featuregate.PlanAllowances
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceUsageStatus
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

        val result = mapper.map(allowances)

        assertFalse(result.isLoading)
        assertFalse(result.isProTier)
        assertTrue(result.isGatingEnabled)
        assertTrue(result.showUpgradeCta)
        assertEquals(UiText.StringResource(R.string.subscriptions_tier_free_title), result.tierTitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_free_subtitle), result.tierSubtitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_free), result.tierBadge)

        assertEquals(3, result.quotas.size)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceQuotaType.AI_RECEIPT_SCANS, aiQuota.type)
        assertEquals(AllowanceUsageStatus.HEALTHY, aiQuota.status)
        assertEquals(0.3333f, aiQuota.progressFraction, 0.0001f)
        assertTrue(aiQuota.showProgressBar)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_fraction, "1", "3"), aiQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_remaining, "2"), aiQuota.statusBadge)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceQuotaType.MEDIA_UPLOADS, mediaQuota.type)
        assertEquals(AllowanceUsageStatus.HEALTHY, mediaQuota.status)
        assertEquals(0.4000f, mediaQuota.progressFraction, 0.0001f)
        assertTrue(mediaQuota.showProgressBar)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_fraction, "2", "5"), mediaQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_remaining, "3"), mediaQuota.statusBadge)

        val groupsQuota = result.quotas[2]
        assertEquals(AllowanceQuotaType.OWNED_GROUPS, groupsQuota.type)
        assertEquals(AllowanceUsageStatus.HEALTHY, groupsQuota.status)
        assertEquals(0f, groupsQuota.progressFraction, 0.0001f)
        assertTrue(groupsQuota.showProgressBar)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_fraction, "0", "1"), groupsQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_remaining, "1"), groupsQuota.statusBadge)

        assertEquals(3, result.perks.size)

        val membersPerk = result.perks[0]
        assertEquals(AllowancePerkType.MEMBERS_PER_GROUP, membersPerk.type)
        assertTrue(membersPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_members_detail, "4"), membersPerk.detail)

        val subunitsPerk = result.perks[1]
        assertEquals(AllowancePerkType.SUBUNITS_PER_GROUP, subunitsPerk.type)
        assertTrue(subunitsPerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_subunits_detail, "1"), subunitsPerk.detail)

        val adFreePerk = result.perks[2]
        assertEquals(AllowancePerkType.AD_FREE, adFreePerk.type)
        assertFalse(adFreePerk.isIncluded)
        assertEquals(UiText.StringResource(R.string.plan_allowances_perk_status_pro_only), adFreePerk.detail)
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

        val result = mapper.map(allowances)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.APPROACHING_LIMIT, aiQuota.status)
        assertEquals(0.6667f, aiQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_remaining, "1"), aiQuota.statusBadge)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.APPROACHING_LIMIT, mediaQuota.status)
        assertEquals(0.8000f, mediaQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_status_remaining, "1"), mediaQuota.statusBadge)
    }

    @Test
    fun `map Free tier exhausted state sets EXHAUSTED status and Limit reached badges across all 3 quotas`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.FREE,
            usedScans = 3,
            aiMonthlyLimit = 3,
            usedUploads = 5,
            mediaMonthlyFreeLimit = 5,
            ownedGroupsUsed = 1,
            maxOwnedGroups = 1
        )

        val result = mapper.map(allowances)

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

        val result = mapper.map(allowances)

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

        val result = mapper.map(allowances)

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

        val result = mapper.map(allowances)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.EXHAUSTED, mediaQuota.status)
        assertEquals(1f, mediaQuota.progressFraction, 0.0001f)

        val groupsQuota = result.quotas[2]
        assertEquals(AllowanceUsageStatus.EXHAUSTED, groupsQuota.status)
        assertEquals(1f, groupsQuota.progressFraction, 0.0001f)
    }

    @Test
    fun `map Pro tier sets Pro header, quota bars, UNLIMITED media, Unlimited subunits, ad-free, and no CTA`() {
        val allowances = createAllowances(
            tier = SubscriptionTier.PRO,
            isGatingEnabled = true,
            usedScans = 12,
            aiMonthlyLimit = 100,
            usedUploads = 18,
            mediaMonthlyFreeLimit = 5,
            ownedGroupsUsed = 4,
            maxOwnedGroups = 100,
            maxMembersPerGroup = 20,
            maxSubunitsPerGroup = null,
            isAdFreeEnabled = true
        )

        val result = mapper.map(allowances)

        assertTrue(result.isProTier)
        assertFalse(result.showUpgradeCta)
        assertEquals(UiText.StringResource(R.string.subscriptions_tier_pro_title), result.tierTitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_pro_subtitle), result.tierSubtitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_pro), result.tierBadge)

        val aiQuota = result.quotas[0]
        assertEquals(AllowanceUsageStatus.HEALTHY, aiQuota.status)
        assertTrue(aiQuota.showProgressBar)
        assertEquals(0.1200f, aiQuota.progressFraction, 0.0001f)

        val mediaQuota = result.quotas[1]
        assertEquals(AllowanceUsageStatus.UNLIMITED, mediaQuota.status)
        assertFalse(mediaQuota.showProgressBar)
        assertEquals(0f, mediaQuota.progressFraction, 0.0001f)
        assertEquals(UiText.StringResource(R.string.plan_allowances_usage_unlimited, "18"), mediaQuota.usageText)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), mediaQuota.statusBadge)

        val groupsQuota = result.quotas[2]
        assertEquals(AllowanceUsageStatus.HEALTHY, groupsQuota.status)
        assertTrue(groupsQuota.showProgressBar)
        assertEquals(0.0400f, groupsQuota.progressFraction, 0.0001f)

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
    fun `map ungated state hides progress bars, uses UNLIMITED status, unlocks ad-free, and hides CTA`() {
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

        val result = mapper.map(allowances)

        assertFalse(result.isGatingEnabled)
        assertFalse(result.showUpgradeCta)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_ungated_title), result.tierTitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_tier_ungated_subtitle), result.tierSubtitle)
        assertEquals(UiText.StringResource(R.string.plan_allowances_badge_unlimited), result.tierBadge)

        result.quotas.forEach { quota ->
            assertEquals(AllowanceUsageStatus.UNLIMITED, quota.status)
            assertFalse(quota.showProgressBar)
            assertEquals(0f, quota.progressFraction, 0.0001f)
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
        val enResult = mapper.map(allowances)
        assertEquals(
            UiText.StringResource(R.string.plan_allowances_usage_fraction, "1,000", "2,000"),
            enResult.quotas[0].usageText
        )

        every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
        val esResult = mapper.map(allowances)
        assertEquals(
            UiText.StringResource(R.string.plan_allowances_usage_fraction, "1.000", "2.000"),
            esResult.quotas[0].usageText
        )
    }

    @Suppress("LongParameterList")
    private fun createAllowances(
        tier: SubscriptionTier = SubscriptionTier.FREE,
        isGatingEnabled: Boolean = true,
        usedScans: Int = 1,
        aiMonthlyLimit: Int = 3,
        usedUploads: Int = 2,
        mediaMonthlyFreeLimit: Int = 5,
        ownedGroupsUsed: Int = 0,
        maxOwnedGroups: Int = 1,
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
            ownedGroupsUsed = ownedGroupsUsed,
            maxOwnedGroups = maxOwnedGroups,
            maxMembersPerGroup = maxMembersPerGroup,
            maxSubunitsPerGroup = maxSubunitsPerGroup,
            isAdFreeEnabled = isAdFreeEnabled
        )
    }
}
