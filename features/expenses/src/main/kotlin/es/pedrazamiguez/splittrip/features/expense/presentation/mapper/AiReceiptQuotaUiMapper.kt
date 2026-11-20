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
        val remainingScansLabel = when {
            !quota.isGatingEnabled -> null
            quota.isFreeTrialEligible && quota.remainingScans > 0 ->
                resourceProvider.getQuantityString(
                    R.plurals.expense_autofill_free_scans_remaining,
                    quota.remainingScans,
                    quota.remainingScans
                )
            quota.isFreeTrialEligible && quota.remainingScans == 0 ->
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

    fun mapBlockedMessage(quota: AiReceiptQuota): UiText = when {
        quota.isFreeTrialEligible && !quota.canScan ->
            UiText.StringResource(R.string.expense_autofill_monthly_limit_reached, quota.monthlyLimit)
        quota.isProTier && !quota.canScan ->
            UiText.StringResource(R.string.expense_autofill_pro_monthly_limit_reached, quota.monthlyLimit)
        else ->
            UiText.StringResource(R.string.expense_autofill_pro_required)
    }
}
