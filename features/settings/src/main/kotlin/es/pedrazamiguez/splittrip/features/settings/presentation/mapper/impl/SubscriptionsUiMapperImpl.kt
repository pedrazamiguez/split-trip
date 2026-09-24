package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.SubscriptionsUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.BillingInterval
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionFeatureUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Suppress("UnusedPrivateProperty", "unused")
class SubscriptionsUiMapperImpl(
    private val localeProvider: LocaleProvider
) : SubscriptionsUiMapper {

    override fun mapPlans(
        currentTier: SubscriptionTier,
        selectedInterval: BillingInterval
    ): ImmutableList<SubscriptionPlanUiModel> {
        val freePlan = createFreePlan(isCurrentPlan = currentTier == SubscriptionTier.FREE)
        val proPlan = createProPlan(
            isCurrentPlan = currentTier == SubscriptionTier.PRO,
            selectedInterval = selectedInterval
        )
        return persistentListOf(freePlan, proPlan)
    }

    override fun formatSavingsBadge(): UiText {
        return UiText.StringResource(R.string.subscriptions_annual_save_badge)
    }

    override fun formatUpgradeSuccessMessage(tier: SubscriptionTier): UiText {
        val tierTitleRes = when (tier) {
            SubscriptionTier.FREE -> R.string.subscriptions_tier_free_title
            SubscriptionTier.PRO -> R.string.subscriptions_tier_pro_title
        }
        return UiText.StringResource(
            R.string.subscriptions_upgrade_success,
            UiText.StringResource(tierTitleRes)
        )
    }

    override fun formatRestorePurchasesSuccessMessage(): UiText {
        return UiText.StringResource(R.string.subscriptions_restore_success)
    }

    private fun createFreePlan(isCurrentPlan: Boolean): SubscriptionPlanUiModel {
        val features = listOf(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_free_groups),
                isIncluded = true
            ),
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_free_members),
                isIncluded = true
            ),
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_free_standard_calc),
                isIncluded = true
            ),
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_free_multi_currency),
                isIncluded = true
            ),
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_subunits),
                isIncluded = false
            ),
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_ai_ocr),
                isIncluded = false
            ),
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_ad_free),
                isIncluded = false
            )
        ).toImmutableList()

        return SubscriptionPlanUiModel(
            tier = SubscriptionTier.FREE,
            title = UiText.StringResource(R.string.subscriptions_tier_free_title),
            description = UiText.StringResource(R.string.subscriptions_tier_free_description),
            price = UiText.StringResource(R.string.subscriptions_tier_free_price),
            period = UiText.StringResource(R.string.subscriptions_tier_free_period),
            badge = null,
            features = features,
            isCurrentPlan = isCurrentPlan,
            ctaButtonText = if (isCurrentPlan) {
                UiText.StringResource(R.string.subscriptions_cta_current_plan)
            } else {
                UiText.StringResource(R.string.subscriptions_cta_downgrade_free)
            },
            isCtaButtonEnabled = !isCurrentPlan,
            isHighlightedCard = false
        )
    }

    private fun createProPlan(
        isCurrentPlan: Boolean,
        selectedInterval: BillingInterval
    ): SubscriptionPlanUiModel {
        val priceRes = when (selectedInterval) {
            BillingInterval.MONTHLY -> R.string.subscriptions_tier_pro_price_monthly
            BillingInterval.ANNUAL -> R.string.subscriptions_tier_pro_price_annual
        }
        val periodRes = when (selectedInterval) {
            BillingInterval.MONTHLY -> R.string.subscriptions_period_month
            BillingInterval.ANNUAL -> R.string.subscriptions_period_annual_billed
        }

        return SubscriptionPlanUiModel(
            tier = SubscriptionTier.PRO,
            title = UiText.StringResource(R.string.subscriptions_tier_pro_title),
            description = UiText.StringResource(R.string.subscriptions_tier_pro_description),
            price = UiText.StringResource(priceRes),
            period = UiText.StringResource(periodRes),
            badge = UiText.StringResource(R.string.subscriptions_badge_popular),
            features = createProPlanFeatures(),
            isCurrentPlan = isCurrentPlan,
            ctaButtonText = if (isCurrentPlan) {
                UiText.StringResource(R.string.subscriptions_cta_current_plan)
            } else {
                UiText.StringResource(R.string.subscriptions_cta_upgrade_pro)
            },
            isCtaButtonEnabled = !isCurrentPlan,
            isHighlightedCard = true
        )
    }

    private fun createProPlanFeatures(): ImmutableList<SubscriptionFeatureUiModel> = listOf(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_unlimited_groups),
            isIncluded = true,
            isHighlighted = true
        ),
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_members),
            isIncluded = true,
            isHighlighted = true
        ),
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_subunits),
            isIncluded = true,
            isHighlighted = true
        ),
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_ai_ocr),
            isIncluded = true,
            isHighlighted = true
        ),
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_ad_free),
            isIncluded = true,
            isHighlighted = true
        ),
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_blended_fx),
            isIncluded = true
        ),
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_priority_support),
            isIncluded = true
        )
    ).toImmutableList()
}
