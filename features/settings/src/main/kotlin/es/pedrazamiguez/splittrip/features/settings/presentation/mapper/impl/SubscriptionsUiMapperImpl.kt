package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.SubscriptionProduct
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.SubscriptionsUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionFeatureStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionFeatureUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SubscriptionPlanUiModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val MONTHS_IN_YEAR = 12
private const val MICROS_PER_UNIT = 1_000_000L
private const val DEFAULT_FRACTION_DIGITS = 2
private const val EXTRA_CALCULATION_SCALE = 2

@Suppress("unused")
class SubscriptionsUiMapperImpl(
    private val localeProvider: LocaleProvider
) : SubscriptionsUiMapper {

    private data class ProPricingDisplay(
        val price: UiText,
        val billingDetail: UiText?
    )

    override fun mapPlans(
        currentTier: SubscriptionTier,
        selectedInterval: BillingInterval,
        products: List<SubscriptionProduct>,
        extractionCapability: ExtractionCapability
    ): ImmutableList<SubscriptionPlanUiModel> {
        val isAiSupported = extractionCapability == ExtractionCapability.ON_DEVICE_AI
        val freePlan = createFreePlan(
            isCurrentPlan = currentTier == SubscriptionTier.FREE,
            isAiSupported = isAiSupported
        )
        val proPlan = createProPlan(
            isCurrentPlan = currentTier == SubscriptionTier.PRO,
            selectedInterval = selectedInterval,
            products = products,
            isAiSupported = isAiSupported
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

    override fun formatPurchasePendingMessage(): UiText {
        return UiText.StringResource(R.string.subscriptions_purchase_pending)
    }

    override fun formatAlreadyOwnedMessage(): UiText {
        return UiText.StringResource(R.string.subscriptions_purchase_already_owned)
    }

    override fun formatNoPurchasesToRestoreMessage(): UiText {
        return UiText.StringResource(R.string.subscriptions_restore_none_found)
    }

    override fun formatBillingError(): UiText {
        return UiText.StringResource(R.string.subscriptions_billing_error)
    }

    override fun formatSubscriptionsUnavailableMessage(): UiText {
        return UiText.StringResource(R.string.subscriptions_service_unavailable)
    }

    override fun formatComplimentaryProMessage(): UiText {
        return UiText.StringResource(R.string.subscriptions_complimentary_pro_manage_info)
    }

    private fun createProPlan(
        isCurrentPlan: Boolean,
        selectedInterval: BillingInterval,
        products: List<SubscriptionProduct>,
        isAiSupported: Boolean
    ): SubscriptionPlanUiModel {
        val matchingProduct = products.firstOrNull { it.billingInterval == selectedInterval }
        val (price, billingDetail) = resolveProPricing(selectedInterval, matchingProduct)
        val description = if (isAiSupported) {
            UiText.StringResource(R.string.subscriptions_tier_pro_description)
        } else {
            UiText.StringResource(R.string.subscriptions_tier_pro_description_no_ai)
        }
        val compatibilityNotice = if (isAiSupported) {
            null
        } else {
            UiText.StringResource(R.string.subscriptions_ai_unsupported_device_notice)
        }

        return SubscriptionPlanUiModel(
            tier = SubscriptionTier.PRO,
            title = UiText.StringResource(R.string.subscriptions_tier_pro_title),
            description = description,
            price = price,
            period = UiText.StringResource(R.string.subscriptions_period_month),
            billingDetail = billingDetail,
            badge = UiText.StringResource(R.string.subscriptions_badge_popular),
            features = createProPlanFeatures(isAiSupported = isAiSupported),
            compatibilityNotice = compatibilityNotice,
            isCurrentPlan = isCurrentPlan,
            ctaButtonText = if (isCurrentPlan) {
                UiText.StringResource(R.string.subscriptions_cta_current_plan)
            } else {
                UiText.StringResource(R.string.subscriptions_cta_upgrade_pro)
            },
            isCtaButtonEnabled = !isCurrentPlan && matchingProduct != null,
            isHighlightedCard = true
        )
    }

    private fun resolveProPricing(
        selectedInterval: BillingInterval,
        matchingProduct: SubscriptionProduct?
    ): ProPricingDisplay = when (selectedInterval) {
        BillingInterval.MONTHLY -> {
            val price = if (matchingProduct != null && matchingProduct.formattedPrice.isNotBlank()) {
                UiText.DynamicString(matchingProduct.formattedPrice)
            } else {
                UiText.StringResource(R.string.subscriptions_tier_pro_price_monthly)
            }
            ProPricingDisplay(price = price, billingDetail = null)
        }
        BillingInterval.ANNUAL -> resolveAnnualProPricing(matchingProduct)
    }

    private fun resolveAnnualProPricing(
        matchingProduct: SubscriptionProduct?
    ): ProPricingDisplay = if (matchingProduct != null && matchingProduct.priceAmountMicros > 0) {
        val fractionDigits = runCatching {
            Currency.getInstance(matchingProduct.priceCurrencyCode).defaultFractionDigits
        }.getOrDefault(DEFAULT_FRACTION_DIGITS)
        val multiplier = BigDecimal.TEN.pow(fractionDigits)
        val divisor = BigDecimal(MONTHS_IN_YEAR * MICROS_PER_UNIT)
        val monthlySmallestUnit = BigDecimal(matchingProduct.priceAmountMicros)
            .divide(divisor, fractionDigits + EXTRA_CALCULATION_SCALE, RoundingMode.HALF_UP)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
        val formattedMonthlyEquivalent = formatCurrencyAmount(
            amount = monthlySmallestUnit,
            currencyCode = matchingProduct.priceCurrencyCode,
            locale = localeProvider.getCurrentLocale()
        )
        ProPricingDisplay(
            price = UiText.DynamicString(formattedMonthlyEquivalent),
            billingDetail = UiText.StringResource(
                R.string.subscriptions_tier_pro_billing_annual_detail,
                matchingProduct.formattedPrice
            )
        )
    } else {
        ProPricingDisplay(
            price = UiText.StringResource(R.string.subscriptions_tier_pro_price_annual),
            billingDetail = UiText.StringResource(
                R.string.subscriptions_tier_pro_billing_annual_detail,
                UiText.StringResource(R.string.subscriptions_tier_pro_price_annual_total)
            )
        )
    }
}

private fun createFreePlan(
    isCurrentPlan: Boolean,
    isAiSupported: Boolean
): SubscriptionPlanUiModel = SubscriptionPlanUiModel(
    tier = SubscriptionTier.FREE,
    title = UiText.StringResource(R.string.subscriptions_tier_free_title),
    description = UiText.StringResource(R.string.subscriptions_tier_free_description),
    price = UiText.StringResource(R.string.subscriptions_tier_free_price),
    period = UiText.StringResource(R.string.subscriptions_tier_free_period),
    badge = null,
    features = createFreePlanFeatures(isAiSupported = isAiSupported),
    compatibilityNotice = null,
    isCurrentPlan = isCurrentPlan,
    ctaButtonText = if (isCurrentPlan) {
        UiText.StringResource(R.string.subscriptions_cta_current_plan)
    } else {
        UiText.StringResource(R.string.subscriptions_cta_manage_subscription)
    },
    isCtaButtonEnabled = !isCurrentPlan,
    isHighlightedCard = false
)

private fun createFreePlanFeatures(isAiSupported: Boolean): ImmutableList<SubscriptionFeatureUiModel> = buildList {
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_free_groups),
            status = SubscriptionFeatureStatus.INCLUDED
        )
    )
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_free_members),
            status = SubscriptionFeatureStatus.INCLUDED
        )
    )
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_free_standard_calc),
            status = SubscriptionFeatureStatus.INCLUDED
        )
    )
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_free_multi_currency),
            status = SubscriptionFeatureStatus.INCLUDED
        )
    )
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_subunits),
            status = SubscriptionFeatureStatus.EXCLUDED
        )
    )
    if (isAiSupported) {
        add(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_ai_ocr),
                status = SubscriptionFeatureStatus.EXCLUDED
            )
        )
    }
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_report_exports),
            status = SubscriptionFeatureStatus.EXCLUDED
        )
    )
    add(
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_ad_free),
            status = SubscriptionFeatureStatus.EXCLUDED
        )
    )
}.toImmutableList()

private fun createProPlanFeatures(isAiSupported: Boolean): ImmutableList<SubscriptionFeatureUiModel> {
    val aiFeature = if (isAiSupported) {
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_ai_ocr),
            status = SubscriptionFeatureStatus.INCLUDED,
            isHighlighted = true
        )
    } else {
        SubscriptionFeatureUiModel(
            label = UiText.StringResource(R.string.subscriptions_feature_pro_ai_ocr_unsupported),
            status = SubscriptionFeatureStatus.UNAVAILABLE_ON_DEVICE,
            isHighlighted = false
        )
    }

    return buildList {
        add(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_unlimited_groups),
                status = SubscriptionFeatureStatus.INCLUDED,
                isHighlighted = true
            )
        )
        add(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_members),
                status = SubscriptionFeatureStatus.INCLUDED,
                isHighlighted = true
            )
        )
        add(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_subunits),
                status = SubscriptionFeatureStatus.INCLUDED,
                isHighlighted = true
            )
        )
        if (isAiSupported) {
            add(aiFeature)
        }
        add(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_report_exports),
                status = SubscriptionFeatureStatus.INCLUDED,
                isHighlighted = true
            )
        )
        add(
            SubscriptionFeatureUiModel(
                label = UiText.StringResource(R.string.subscriptions_feature_pro_ad_free),
                status = SubscriptionFeatureStatus.INCLUDED,
                isHighlighted = true
            )
        )
        if (!isAiSupported) {
            add(aiFeature)
        }
    }.toImmutableList()
}
