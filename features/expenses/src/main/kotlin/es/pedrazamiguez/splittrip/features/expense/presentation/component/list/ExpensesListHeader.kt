package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

private val HEADER_ELEMENT_GAP = 6.dp

@Suppress("LongParameterList")
@Composable
internal fun ExpensesListHeader(
    formattedTotalSpent: String,
    formattedTotalScheduled: String?,
    visibleExpensesCount: Int,
    isFiltered: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = MaterialTheme.spacing.Default,
                vertical = MaterialTheme.spacing.Small
            ),
        verticalArrangement = Arrangement.spacedBy(HEADER_ELEMENT_GAP)
    ) {
        ExpensesTotalSummaryRow(
            formattedTotalSpent = formattedTotalSpent,
            formattedTotalScheduled = formattedTotalScheduled,
            visibleExpensesCount = visibleExpensesCount,
            isFiltered = isFiltered,
            modifier = Modifier.fillMaxWidth()
        )
        ExpenseSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChanged,
            onFocusChanged = onSearchFocusChanged,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
