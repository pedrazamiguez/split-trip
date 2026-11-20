package es.pedrazamiguez.splittrip.features.expense.presentation.model

/**
 * UI projection of the acting user's monthly AI receipt scanning quota.
 *
 * @property isAiGated True when the user cannot currently run AI receipt scans (quota exhausted).
 * @property isAiProFeature True when unlocking AI scanning requires upgrading to Pro.
 * @property remainingScansLabel Localized indicator label for Free trial users (or exhausted Pro cap);
 * null for active Pro users or disabled gating.
 */
data class AiReceiptQuotaUiModel(
    val isAiGated: Boolean,
    val isAiProFeature: Boolean,
    val remainingScansLabel: String?
)
