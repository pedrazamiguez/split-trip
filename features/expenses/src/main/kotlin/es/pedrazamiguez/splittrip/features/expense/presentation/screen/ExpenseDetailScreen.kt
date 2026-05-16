package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.BreakdownCardSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.CashTranchesDetailSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.HeroSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.NotesSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.ProvenanceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.SplitBreakdownSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState

@Composable
fun ExpenseDetailScreen(
    uiState: ExpenseDetailUiState = ExpenseDetailUiState(),
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> ShimmerLoadingList()
        uiState.hasError || uiState.expense == null -> {
            EmptyStateView(
                title = stringResource(R.string.expense_detail_error_loading),
                icon = TablerIcons.Outline.Receipt
            )
        }
        else -> ExpenseDetailContent(expense = uiState.expense, modifier = modifier)
    }
}

@Composable
private fun ExpenseDetailContent(
    expense: ExpenseDetailUiModel,
    modifier: Modifier = Modifier
) {
    // Tab-screens MUST consume LocalBottomPadding so the floating nav bar does not
    // cover the last card / sticky actions (Manifesto §7 — Bottom Padding rule).
    val bottomPadding = LocalBottomPadding.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.Default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        HeroSection(expense = expense)

        if (expense.notesText != null) {
            NotesSection(notesText = expense.notesText)
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

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
