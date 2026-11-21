package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.domain.service.featuregate.ManualReceiptQuota
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ManualReceiptQuotaUiModel

class ManualReceiptQuotaUiMapper(
    private val resourceProvider: ResourceProvider
) {
    fun mapQuotaToUiModel(quota: ManualReceiptQuota): ManualReceiptQuotaUiModel {
        val remaining = quota.remainingAttachments
        val remainingLabel = when {
            !quota.isGatingEnabled || quota.isProTier -> null
            quota.isFreeQuotaTracked && remaining != null && remaining > 0 ->
                resourceProvider.getQuantityString(
                    R.plurals.add_expense_receipt_free_attachments_remaining,
                    remaining,
                    remaining
                )
            quota.isFreeQuotaTracked && remaining == 0 ->
                resourceProvider.getString(R.string.add_expense_receipt_free_attachments_exhausted)
            else -> null
        }

        return ManualReceiptQuotaUiModel(
            isManualReceiptGated = !quota.canAttach,
            isManualReceiptProFeature = quota.requiresUpgrade,
            remainingAttachmentsLabel = remainingLabel
        )
    }

    fun mapBlockedMessage(quota: ManualReceiptQuota): UiText =
        UiText.StringResource(R.string.add_expense_receipt_monthly_limit_reached, quota.monthlyFreeLimit)
}
