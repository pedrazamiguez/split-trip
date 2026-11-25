package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.component.dialog.ResetFiltersConfirmationDialog
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.AmountFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.AttributesFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.CategoryFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.DateRangeFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.FilterActionFooter
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.MemberFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.PaymentMethodFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.PaymentStatusFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState

private val STICKY_ACTION_BAR_BOTTOM_SPACING = 132.dp

@Suppress("LongMethod")
@Composable
fun ExpensesFilterScreen(
    uiState: ExpensesFilterUiState = ExpensesFilterUiState(),
    onUpdateDraft: (ExpenseFilterCriteria) -> Unit = {},
    onPresetSelected: (DateRangePreset) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onApplyFilters: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current
    val isBottomBarHidden = bottomPadding == 0.dp
    val navBarInset = if (isBottomBarHidden) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }
    var showResetFiltersDialog by remember { mutableStateOf(false) }

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList(modifier = Modifier.padding(top = topPadding)) }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.Default,
                    top = MaterialTheme.spacing.Default + topPadding,
                    end = MaterialTheme.spacing.Default,
                    bottom = STICKY_ACTION_BAR_BOTTOM_SPACING + bottomPadding + navBarInset
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraLarge)
            ) {
                if (uiState.hasVisibleCategories) {
                    item(key = "category_section") {
                        CategoryFilterSection(
                            availableCategories = uiState.availableCategories,
                            availableSubcategories = uiState.availableSubcategories,
                            criteria = uiState.draftCriteria,
                            onCriteriaChange = onUpdateDraft,
                            carouselEdgePadding = MaterialTheme.spacing.Default,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item(key = "payment_method_section") {
                    PaymentMethodFilterSection(
                        criteria = uiState.draftCriteria,
                        onCriteriaChange = onUpdateDraft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "payment_status_section") {
                    PaymentStatusFilterSection(
                        criteria = uiState.draftCriteria,
                        onCriteriaChange = onUpdateDraft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "attributes_section") {
                    AttributesFilterSection(
                        criteria = uiState.draftCriteria,
                        onCriteriaChange = onUpdateDraft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "amount_section") {
                    AmountFilterSection(
                        criteria = uiState.draftCriteria,
                        groupCurrencies = uiState.groupCurrencies,
                        defaultCurrency = uiState.groupCurrency,
                        onCriteriaChange = onUpdateDraft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "member_section") {
                    MemberFilterSection(
                        availableMembers = uiState.availableMembers,
                        criteria = uiState.draftCriteria,
                        onCriteriaChange = onUpdateDraft,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item(key = "date_range_section") {
                    DateRangeFilterSection(
                        criteria = uiState.draftCriteria,
                        activePreset = uiState.activePreset,
                        onPresetSelected = onPresetSelected,
                        onCriteriaChange = onUpdateDraft,
                        oldestExpenseDate = uiState.oldestExpenseDate,
                        newestExpenseDate = uiState.newestExpenseDate,
                        formattedStartDate = uiState.formattedStartDate,
                        formattedEndDate = uiState.formattedEndDate,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            FilterActionFooter(
                matchingExpensesCount = uiState.matchingExpensesCount,
                canReset = uiState.canReset,
                bottomPadding = bottomPadding,
                isBottomBarHidden = isBottomBarHidden,
                onResetClick = { showResetFiltersDialog = true },
                onApplyClick = onApplyFilters,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showResetFiltersDialog) {
        ResetFiltersConfirmationDialog(
            onDismiss = { showResetFiltersDialog = false },
            onConfirm = {
                onResetFilters()
                showResetFiltersDialog = false
            }
        )
    }
}
