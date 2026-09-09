package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.dialog.ResetFiltersConfirmationDialog
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.CategoryFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.DateRangeFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.MemberFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState

private val STICKY_ACTION_BAR_BOTTOM_SPACING = 132.dp
private val ICON_SIZE = 18.dp

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
    val bottomPadding = LocalBottomPadding.current
    var showResetFiltersDialog by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.Default,
                    top = MaterialTheme.spacing.Default,
                    end = MaterialTheme.spacing.Default,
                    bottom = STICKY_ACTION_BAR_BOTTOM_SPACING + bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraLarge)
            ) {
                item(key = "category_section") {
                    CategoryFilterSection(
                        criteria = uiState.draftCriteria,
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .horizonGlassEffect(hazeState = hazeState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = MaterialTheme.spacing.Default,
                            end = MaterialTheme.spacing.Default,
                            top = MaterialTheme.spacing.Small,
                            bottom = bottomPadding
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.padding(
                            top = MaterialTheme.spacing.Small,
                            bottom = MaterialTheme.spacing.Large
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                    ) {
                        Icon(
                            imageVector = if (uiState.canReset) {
                                TablerIcons.Outline.AlignJustified
                            } else {
                                TablerIcons.Outline.Receipt
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ICON_SIZE)
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.expenses_filter_matching_count,
                                uiState.matchingExpensesCount,
                                uiState.matchingExpensesCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SecondaryButton(
                            text = stringResource(R.string.expenses_filter_reset),
                            onClick = { showResetFiltersDialog = true },
                            enabled = uiState.canReset,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = stringResource(R.string.expenses_filter_apply),
                            onClick = onApplyFilters,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
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
