package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.rememberConnectedScrollBehavior
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.BreakdownCardSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.CashTranchesDetailSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.HeroSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.NotesSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.ProvenanceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.SplitBreakdownSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    uiState: ExpenseDetailUiState = ExpenseDetailUiState(),
    modifier: Modifier = Modifier,
    onReceiptTap: (() -> Unit)? = null
) {
    val scrollBehavior = rememberConnectedScrollBehavior()

    when {
        uiState.isLoading -> ShimmerLoadingList()
        uiState.hasError || uiState.expense == null -> {
            EmptyStateView(
                title = stringResource(R.string.expense_detail_error_loading),
                icon = TablerIcons.Outline.Receipt
            )
        }
        else -> {
            val expense = uiState.expense
            val bottomPadding = LocalBottomPadding.current

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

                HeroSection(expense = expense, onReceiptTap = onReceiptTap)

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
    }
}
