package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.BreakdownCardSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.CashTranchesDetailSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.HeroSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.NotesSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.ProvenanceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.SplitBreakdownSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.SubExpensesDetailSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

@Composable
internal fun ExpenseDetailContent(
    expense: ExpenseDetailUiModel,
    modifier: Modifier,
    onReceiptTap: (() -> Unit)?,
    onConfirmPaymentTap: (() -> Unit)?
) {
    val bottomPadding = LocalBottomPadding.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.Default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        HeroSection(expense = expense, onReceiptTap = onReceiptTap)

        if (expense.notesText != null) {
            NotesSection(notesText = expense.notesText)
        }

        if (expense.isComposite && expense.subExpenses.isNotEmpty()) {
            SubExpensesDetailSection(subExpenses = expense.subExpenses)
        }

        if (expense.hasAddOns || expense.formattedIncludedBaseCost != null) {
            BreakdownCardSection(
                addOns = expense.addOns,
                formattedEffectiveTotal = expense.formattedEffectiveTotal,
                formattedIncludedBaseCost = expense.formattedIncludedBaseCost,
                formattedOriginalEnteredTotal = expense.formattedOriginalEnteredTotal
            )
        }

        if (expense.cashTranches.isNotEmpty()) {
            CashTranchesDetailSection(tranches = expense.cashTranches)
        }

        SplitBreakdownSection(
            splitTypeText = expense.splitTypeText,
            splits = expense.splits,
            splitGroups = expense.splitGroups
        )

        ProvenanceSection(expense = expense)

        if (expense.isScheduled && expense.isForeignCurrency && onConfirmPaymentTap != null) {
            GradientButton(
                text = stringResource(R.string.confirm_payment),
                onClick = onConfirmPaymentTap,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
