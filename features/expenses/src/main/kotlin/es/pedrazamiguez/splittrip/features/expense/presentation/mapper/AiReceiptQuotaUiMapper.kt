package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.service.featuregate.AiReceiptQuota
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AiReceiptQuotaUiModel

class AiReceiptQuotaUiMapper(
    private val resourceProvider: ResourceProvider
) {
    fun mapQuotaToUiModel(quota: AiReceiptQuota): AiReceiptQuotaUiModel {
        val remaining = quota.remainingScans
        val remainingScansLabel = when {
            quota.isUnlimited -> null
            quota.isFreeTrialEligible && remaining != null && remaining > 0 ->
                resourceProvider.getQuantityString(
                    R.plurals.expense_autofill_free_scans_remaining,
                    remaining,
                    remaining
                )
            quota.isFreeTrialEligible && remaining == 0 ->
                resourceProvider.getString(R.string.expense_autofill_free_scans_exhausted)
            quota.isProTier && !quota.canScan ->
                resourceProvider.getString(R.string.expense_autofill_monthly_scans_exhausted)
            else -> null
        }

        return AiReceiptQuotaUiModel(
            isAiGated = !quota.canScan,
            isAiProFeature = quota.requiresUpgrade,
            remainingScansLabel = remainingScansLabel
        )
    }

    fun mapBlockedMessage(quota: AiReceiptQuota): UiText {
        val limit = quota.monthlyLimit
        return when {
            quota.isFreeTrialEligible && !quota.canScan && limit != null ->
                UiText.StringResource(R.string.expense_autofill_monthly_limit_reached, limit)
            quota.isProTier && !quota.canScan && limit != null ->
                UiText.StringResource(R.string.expense_autofill_pro_monthly_limit_reached, limit)
            else ->
                UiText.StringResource(R.string.expense_autofill_pro_required)
        }
    }
}
