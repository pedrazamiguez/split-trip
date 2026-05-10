package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmountWithCurrency
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import java.util.Locale

/**
 * Step 11 (final): Read-only summary of all entered data.
 * Shows amounts, exchange rate (if foreign), detail fields, split, and add-ons.
 */
@Composable
fun ReviewStep(
    uiState: AddExpenseUiState,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        SectionCard(
            title = stringResource(R.string.expense_review_title)
        ) {
            ReviewAmountSection(uiState)
            ReviewDetailsSection(uiState)
            ReviewSplitSection(uiState)
            ReviewAddOnsSection(uiState)
        }
    }
}

// ── Amount ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewAmountSection(uiState: AddExpenseUiState) {
    val none = stringResource(R.string.expense_review_none)
    val locale = rememberLocale()

    ReviewRow(
        label = stringResource(R.string.expense_review_title_label),
        value = uiState.expenseTitle.ifBlank { none }
    )
    ReviewRow(
        label = stringResource(R.string.expense_review_amount),
        value = uiState.selectedCurrency?.code?.let { code ->
            formatAmountWithCurrency(uiState.sourceAmount, code, locale)
        }?.ifBlank { none } ?: uiState.sourceAmount.ifBlank { none }
    )
    ReviewRow(
        label = stringResource(R.string.expense_review_currency),
        value = uiState.selectedCurrency?.displayText ?: none
    )

    if (uiState.showExchangeRateSection) {
        ReviewRow(
            label = stringResource(R.string.expense_review_exchange_rate),
            value = uiState.displayExchangeRate
        )
        ReviewRow(
            label = stringResource(R.string.expense_review_group_amount),
            value = uiState.groupCurrency?.code?.let { code ->
                formatAmountWithCurrency(uiState.calculatedGroupAmount, code, locale)
            }?.ifBlank { none } ?: uiState.calculatedGroupAmount.ifBlank { none }
        )
    }
}

// ── Details ──────────────────────────────────────────────────────────────

@Composable
private fun ReviewDetailsSection(uiState: AddExpenseUiState) {
    val hasAnyDetail = uiState.selectedPaymentMethod != null ||
        uiState.selectedFundingSource != null ||
        uiState.selectedCategory != null ||
        uiState.vendor.isNotBlank() ||
        uiState.notes.isNotBlank() ||
        uiState.selectedPaymentStatus != null ||
        uiState.formattedDueDate.isNotBlank() ||
        uiState.receiptUri != null

    if (!hasAnyDetail) return

    ReviewCategoryAndVendor(uiState)
    ReviewStatusAndSchedule(uiState)
    ReviewReceipt(uiState)
}

@Composable
private fun ReviewCategoryAndVendor(uiState: AddExpenseUiState) {
    uiState.selectedPaymentMethod?.let {
        ReviewRow(
            label = stringResource(R.string.expense_review_payment_method),
            value = it.displayText
        )
    }
    uiState.selectedFundingSource?.let {
        ReviewRow(
            label = stringResource(R.string.expense_review_funding_source),
            value = it.displayText
        )
    }
    if (uiState.showContributionScopeStep) {
        ReviewRow(
            label = stringResource(R.string.expense_review_contribution_scope),
            value = when (uiState.contributionScope) {
                PayerType.GROUP -> stringResource(R.string.expense_review_scope_group)
                PayerType.SUBUNIT ->
                    uiState.contributionSubunitOptions
                        .find { it.id == uiState.selectedContributionSubunitId }
                        ?.name ?: stringResource(R.string.expense_review_none)
                else -> stringResource(R.string.expense_review_scope_personal)
            }
        )
    }
    uiState.selectedCategory?.let {
        ReviewRow(
            label = stringResource(R.string.expense_review_category),
            value = it.displayText
        )
    }
    if (uiState.vendor.isNotBlank()) {
        ReviewRow(
            label = stringResource(R.string.expense_review_vendor),
            value = uiState.vendor
        )
    }
    if (uiState.notes.isNotBlank()) {
        ReviewRow(
            label = stringResource(R.string.expense_review_notes),
            value = uiState.notes
        )
    }
}

@Composable
private fun ReviewStatusAndSchedule(uiState: AddExpenseUiState) {
    uiState.selectedPaymentStatus?.let {
        ReviewRow(
            label = stringResource(R.string.expense_review_payment_status),
            value = it.displayText
        )
    }
    if (uiState.formattedDueDate.isNotBlank()) {
        ReviewRow(
            label = stringResource(R.string.expense_review_due_date),
            value = uiState.formattedDueDate
        )
    }
}

@Composable
private fun ReviewReceipt(uiState: AddExpenseUiState) {
    if (uiState.receiptUri != null) {
        ReviewRow(
            label = stringResource(R.string.expense_review_receipt),
            value = stringResource(R.string.expense_review_receipt_attached)
        )
    }
}

// ── Split ────────────────────────────────────────────────────────────────

@Composable
private fun ReviewSplitSection(uiState: AddExpenseUiState) {
    if (uiState.memberIds.size <= 1) return

    uiState.selectedSplitType?.let {
        ReviewRow(
            label = stringResource(R.string.expense_review_split_type),
            value = it.displayText
        )
    }
}

// ── Add-Ons ──────────────────────────────────────────────────────────────

@Composable
private fun ReviewAddOnsSection(uiState: AddExpenseUiState) {
    if (uiState.addOns.isEmpty()) return

    ReviewRow(
        label = stringResource(R.string.expense_review_add_ons),
        value = uiState.addOns.size.toString()
    )
    if (uiState.effectiveTotal.isNotBlank()) {
        ReviewRow(
            label = stringResource(R.string.expense_review_effective_total),
            value = uiState.effectiveTotal
        )
    }
}

// ── Shared ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun rememberLocale(): Locale {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        configuration.locales[0] ?: Locale.getDefault()
    }
}
