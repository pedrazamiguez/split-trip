package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.BillingInterval
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionFeatureUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SubscriptionsUiMapperImpl")
class SubscriptionsUiMapperImplTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var mapper: SubscriptionsUiMapperImpl

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.ENGLISH
        mapper = SubscriptionsUiMapperImpl(localeProvider = localeProvider)
    }

    private fun assertFeature(
        feature: SubscriptionFeatureUiModel,
        expectedResId: Int,
        expectedIncluded: Boolean,
        expectedHighlighted: Boolean = false
    ) {
        val labelRes = assertInstanceOf(UiText.StringResource::class.java, feature.label)
        assertEquals(expectedResId, labelRes.resId)
        assertEquals(expectedIncluded, feature.isIncluded)
        assertEquals(expectedHighlighted, feature.isHighlighted)
    }

    private fun assertFreePlan(freePlan: SubscriptionPlanUiModel) {
        assertEquals(SubscriptionTier.FREE, freePlan.tier)
        val freeTitle = assertInstanceOf(UiText.StringResource::class.java, freePlan.title)
        assertEquals(R.string.subscriptions_tier_free_title, freeTitle.resId)
        val freePrice = assertInstanceOf(UiText.StringResource::class.java, freePlan.price)
        assertEquals(R.string.subscriptions_tier_free_price, freePrice.resId)
        val freePeriod = assertInstanceOf(UiText.StringResource::class.java, freePlan.period)
        assertEquals(R.string.subscriptions_tier_free_period, freePeriod.resId)
        assertNull(freePlan.badge)
        assertTrue(freePlan.isCurrentPlan)
        assertFalse(freePlan.isCtaButtonEnabled)
        assertFalse(freePlan.isHighlightedCard)
        assertEquals(7, freePlan.features.size)
        assertFeature(freePlan.features[0], R.string.subscriptions_feature_free_groups, expectedIncluded = true)
        assertFeature(freePlan.features[1], R.string.subscriptions_feature_free_members, expectedIncluded = true)
        assertFeature(
            freePlan.features[2],
            R.string.subscriptions_feature_free_standard_calc,
            expectedIncluded = true
        )
        assertFeature(
            freePlan.features[3],
            R.string.subscriptions_feature_free_multi_currency,
            expectedIncluded = true
        )
        assertFeature(freePlan.features[4], R.string.subscriptions_feature_subunits, expectedIncluded = false)
        assertFeature(freePlan.features[5], R.string.subscriptions_feature_pro_ai_ocr, expectedIncluded = false)
        assertFeature(freePlan.features[6], R.string.subscriptions_feature_pro_ad_free, expectedIncluded = false)
    }

    private fun assertProPlan(proPlan: SubscriptionPlanUiModel) {
        assertEquals(SubscriptionTier.PRO, proPlan.tier)
        val proTitle = assertInstanceOf(UiText.StringResource::class.java, proPlan.title)
        assertEquals(R.string.subscriptions_tier_pro_title, proTitle.resId)
        val proPrice = assertInstanceOf(UiText.StringResource::class.java, proPlan.price)
        assertEquals(R.string.subscriptions_tier_pro_price_annual, proPrice.resId)
        val proPeriod = assertInstanceOf(UiText.StringResource::class.java, proPlan.period)
        assertEquals(R.string.subscriptions_period_annual_billed, proPeriod.resId)
        val proBadge = assertInstanceOf(UiText.StringResource::class.java, proPlan.badge)
        assertEquals(R.string.subscriptions_badge_popular, proBadge.resId)
        assertFalse(proPlan.isCurrentPlan)
        assertTrue(proPlan.isCtaButtonEnabled)
        assertTrue(proPlan.isHighlightedCard)
        assertEquals(7, proPlan.features.size)
        assertFeature(
            proPlan.features[0],
            R.string.subscriptions_feature_pro_unlimited_groups,
            expectedIncluded = true,
            expectedHighlighted = true
        )
        assertFeature(
            proPlan.features[1],
            R.string.subscriptions_feature_pro_members,
            expectedIncluded = true,
            expectedHighlighted = true
        )
        assertFeature(
            proPlan.features[2],
            R.string.subscriptions_feature_subunits,
            expectedIncluded = true,
            expectedHighlighted = true
        )
        assertFeature(
            proPlan.features[3],
            R.string.subscriptions_feature_pro_ai_ocr,
            expectedIncluded = true,
            expectedHighlighted = true
        )
        assertFeature(
            proPlan.features[4],
            R.string.subscriptions_feature_pro_ad_free,
            expectedIncluded = true,
            expectedHighlighted = true
        )
        assertFeature(
            proPlan.features[5],
            R.string.subscriptions_feature_pro_blended_fx,
            expectedIncluded = true,
            expectedHighlighted = false
        )
        assertFeature(
            proPlan.features[6],
            R.string.subscriptions_feature_pro_priority_support,
            expectedIncluded = true,
            expectedHighlighted = false
        )
    }

    @Nested
    @DisplayName("mapPlans")
    inner class MapPlans {

        @Test
        fun `mapPlans generates Free and Pro plans with Annual pricing when Annual selected`() {
            val plans = mapper.mapPlans(
                currentTier = SubscriptionTier.FREE,
                selectedInterval = BillingInterval.ANNUAL
            )

            assertEquals(2, plans.size)
            assertFreePlan(plans[0])
            assertProPlan(plans[1])
        }

        @Test
        fun `mapPlans generates Pro plan with Monthly pricing when Monthly selected`() {
            val plans = mapper.mapPlans(
                currentTier = SubscriptionTier.FREE,
                selectedInterval = BillingInterval.MONTHLY
            )

            val proPlan = plans.first { it.tier == SubscriptionTier.PRO }
            val proPrice = assertInstanceOf(UiText.StringResource::class.java, proPlan.price)
            assertEquals(R.string.subscriptions_tier_pro_price_monthly, proPrice.resId)
            val proPeriod = assertInstanceOf(UiText.StringResource::class.java, proPlan.period)
            assertEquals(R.string.subscriptions_period_month, proPeriod.resId)
        }

        @Test
        fun `mapPlans sets Pro as current plan when user tier is Pro`() {
            val plans = mapper.mapPlans(
                currentTier = SubscriptionTier.PRO,
                selectedInterval = BillingInterval.ANNUAL
            )

            val freePlan = plans.first { it.tier == SubscriptionTier.FREE }
            val proPlan = plans.first { it.tier == SubscriptionTier.PRO }

            assertFalse(freePlan.isCurrentPlan)
            assertTrue(freePlan.isCtaButtonEnabled)
            val freeCtaText = assertInstanceOf(UiText.StringResource::class.java, freePlan.ctaButtonText)
            assertEquals(R.string.subscriptions_cta_downgrade_free, freeCtaText.resId)

            assertTrue(proPlan.isCurrentPlan)
            assertFalse(proPlan.isCtaButtonEnabled)
            val ctaText = assertInstanceOf(UiText.StringResource::class.java, proPlan.ctaButtonText)
            assertEquals(R.string.subscriptions_cta_current_plan, ctaText.resId)
        }
    }

    @Nested
    @DisplayName("formatSavingsBadge")
    inner class FormatSavingsBadge {

        @Test
        fun `formatSavingsBadge returns annual save badge resource`() {
            val result = mapper.formatSavingsBadge()
            val res = assertInstanceOf(UiText.StringResource::class.java, result)
            assertEquals(R.string.subscriptions_annual_save_badge, res.resId)
        }
    }

    @Nested
    @DisplayName("formatUpgradeSuccessMessage")
    inner class FormatUpgradeSuccessMessage {

        @Test
        fun `formatUpgradeSuccessMessage for PRO returns upgrade success message with PRO title`() {
            val result = mapper.formatUpgradeSuccessMessage(SubscriptionTier.PRO)
            val res = assertInstanceOf(UiText.StringResource::class.java, result)
            assertEquals(R.string.subscriptions_upgrade_success, res.resId)
            assertEquals(1, res.args.size)
            val innerRes = assertInstanceOf(UiText.StringResource::class.java, res.args[0])
            assertEquals(R.string.subscriptions_tier_pro_title, innerRes.resId)
        }

        @Test
        fun `formatUpgradeSuccessMessage for FREE returns upgrade success message with FREE title`() {
            val result = mapper.formatUpgradeSuccessMessage(SubscriptionTier.FREE)
            val res = assertInstanceOf(UiText.StringResource::class.java, result)
            assertEquals(R.string.subscriptions_upgrade_success, res.resId)
            assertEquals(1, res.args.size)
            val innerRes = assertInstanceOf(UiText.StringResource::class.java, res.args[0])
            assertEquals(R.string.subscriptions_tier_free_title, innerRes.resId)
        }
    }

    @Nested
    @DisplayName("formatRestorePurchasesSuccessMessage")
    inner class FormatRestorePurchasesSuccessMessage {

        @Test
        fun `formatRestorePurchasesSuccessMessage returns restore success message`() {
            val result = mapper.formatRestorePurchasesSuccessMessage()
            val res = assertInstanceOf(UiText.StringResource::class.java, result)
            assertEquals(R.string.subscriptions_restore_success, res.resId)
        }
    }
}
