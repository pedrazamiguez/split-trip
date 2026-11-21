package es.pedrazamiguez.splittrip.features.expense.presentation.model

data class ManualReceiptQuotaUiModel(
    val isManualReceiptGated: Boolean,
    val isManualReceiptProFeature: Boolean,
    val remainingAttachmentsLabel: String?
)
