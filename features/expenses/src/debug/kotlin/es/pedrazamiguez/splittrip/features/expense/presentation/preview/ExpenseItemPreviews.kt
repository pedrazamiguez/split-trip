package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocales
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpenseItem

@PreviewLocales
@Composable
private fun ExpenseItemBasicPreview() {
    ExpenseItemPreviewHelper(domainExpense = PREVIEW_EXPENSE_BASIC) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewLocales
@Composable
private fun ExpenseItemForeignCurrencyPreview() {
    ExpenseItemPreviewHelper(domainExpense = PREVIEW_EXPENSE_FOREIGN_CURRENCY) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewLocales
@Composable
private fun ExpenseItemScheduledPreview() {
    ExpenseItemPreviewHelper(domainExpense = PREVIEW_EXPENSE_SCHEDULED) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewLocales
@Composable
private fun ExpenseItemWithVendorPreview() {
    ExpenseItemPreviewHelper(domainExpense = PREVIEW_EXPENSE_WITH_VENDOR) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewLocales
@Composable
private fun ExpenseItemOutOfPocketPreview() {
    ExpenseItemPreviewHelper(
        domainExpense = PREVIEW_EXPENSE_OUT_OF_POCKET,
        memberProfiles = PREVIEW_MEMBER_PROFILES,
        pairedContributions = PREVIEW_PAIRED_CONTRIBUTIONS
    ) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewLocales
@Composable
private fun ExpenseItemOutOfPocketCurrentUserPreview() {
    ExpenseItemPreviewHelper(
        domainExpense = PREVIEW_EXPENSE_OUT_OF_POCKET_SUBUNIT,
        memberProfiles = PREVIEW_MEMBER_PROFILES,
        currentUserId = PREVIEW_CURRENT_USER_ID,
        pairedContributions = PREVIEW_PAIRED_CONTRIBUTIONS,
        subunits = PREVIEW_SUBUNITS
    ) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewLocales
@Composable
private fun ExpenseItemOutOfPocketGroupScopePreview() {
    ExpenseItemPreviewHelper(
        domainExpense = PREVIEW_EXPENSE_OUT_OF_POCKET_GROUP,
        memberProfiles = PREVIEW_MEMBER_PROFILES,
        pairedContributions = PREVIEW_PAIRED_CONTRIBUTIONS
    ) {
        ExpenseItem(
            expenseUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun ExpenseItemAllCategoriesPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PREVIEW_ALL_CATEGORY_EXPENSES.forEach { expense ->
            ExpenseItemPreviewHelper(domainExpense = expense) {
                ExpenseItem(expenseUiModel = it)
            }
        }
    }
}
