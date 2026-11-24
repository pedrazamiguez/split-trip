package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardReviewRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatAmountWithCurrency
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 11 (final): Read-only summary of all entered data.
 * Shows amounts, exchange rate (if foreign), detail fields, split, and add-ons.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun ReviewStep(
    uiState: AddExpenseUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.expense_review_none)
    val locale = rememberLocale()

    WizardStepLayout(modifier = modifier) {
        SectionCard(
            title = stringResource(R.string.expense_review_title)
        ) {
            WizardReviewRow(
                label = stringResource(R.string.expense_review_title_label),
                value = uiState.expenseTitle.ifBlank { none }
            )

            val amountVal = uiState.selectedCurrency?.code?.let { code ->
                formatAmountWithCurrency(uiState.sourceAmount, code, locale)
            }?.ifBlank { none } ?: uiState.sourceAmount.ifBlank { none }
            WizardReviewRow(
                label = stringResource(R.string.expense_review_amount),
                value = amountVal
            )

            WizardReviewRow(
                label = stringResource(R.string.expense_review_currency),
                value = uiState.selectedCurrency?.displayText ?: none
            )

            if (uiState.showExchangeRateSection) {
                WizardReviewRow(
                    label = stringResource(R.string.expense_review_exchange_rate),
                    value = uiState.displayExchangeRate
                )

                val groupAmountVal = uiState.groupCurrency?.code?.let { code ->
                    formatAmountWithCurrency(uiState.calculatedGroupAmount, code, locale)
                }?.ifBlank { none } ?: uiState.calculatedGroupAmount.ifBlank { none }
                WizardReviewRow(
                    label = stringResource(R.string.expense_review_group_amount),
                    value = groupAmountVal
                )
            }

            val hasAnyDetail = uiState.selectedPaymentMethod != null ||
                uiState.selectedFundingSource != null ||
                uiState.selectedCategory != null ||
                uiState.vendor.isNotBlank() ||
                uiState.notes.isNotBlank() ||
                uiState.selectedPaymentStatus != null ||
                uiState.formattedDueDate.isNotBlank() ||
                uiState.receiptUri != null

            if (hasAnyDetail) {
                uiState.selectedPaymentMethod?.let {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_payment_method),
                        value = it.displayText
                    )
                }
                uiState.selectedFundingSource?.let {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_funding_source),
                        value = it.displayText
                    )
                }
                if (uiState.showContributionScopeStep) {
                    val scopeVal = when (uiState.contributionScope) {
                        PayerType.GROUP -> stringResource(R.string.expense_review_scope_group)
                        PayerType.SUBUNIT ->
                            uiState.contributionSubunitOptions
                                .find { it.id == uiState.selectedContributionSubunitId }
                                ?.name ?: none
                        else -> stringResource(R.string.expense_review_scope_personal)
                    }
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_contribution_scope),
                        value = scopeVal
                    )
                }
                uiState.selectedCategory?.let { category ->
                    val subcategoryLabel = uiState.selectedSubcategory?.name?.asString()
                    val categoryDisplay = if (!subcategoryLabel.isNullOrBlank()) {
                        "${category.displayText} · $subcategoryLabel"
                    } else {
                        category.displayText
                    }
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_category),
                        value = categoryDisplay
                    )
                }
                if (uiState.vendor.isNotBlank()) {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_vendor),
                        value = uiState.vendor
                    )
                }
                if (uiState.notes.isNotBlank()) {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_notes),
                        value = uiState.notes
                    )
                }
                uiState.selectedPaymentStatus?.let {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_payment_status),
                        value = it.displayText
                    )
                }
                if (uiState.formattedDueDate.isNotBlank()) {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_due_date),
                        value = uiState.formattedDueDate
                    )
                }
                if (uiState.receiptUri != null) {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_receipt),
                        value = stringResource(R.string.expense_review_receipt_attached)
                    )
                }
            }

            if (uiState.memberIds.size > 1) {
                uiState.selectedSplitType?.let {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_split_type),
                        value = it.displayText
                    )
                }
            }

            if (uiState.addOns.isNotEmpty()) {
                WizardReviewRow(
                    label = stringResource(R.string.expense_review_add_ons),
                    value = uiState.addOns.size.toString()
                )
                if (uiState.effectiveTotal.isNotBlank()) {
                    WizardReviewRow(
                        label = stringResource(R.string.expense_review_effective_total),
                        value = uiState.effectiveTotal
                    )
                }
            }
        }
    }
}
