package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpenseSearchBar

@PreviewComplete
@Composable
private fun ExpenseSearchBarEmptyPreview() {
    PreviewThemeWrapper {
        ExpenseSearchBar(
            query = "",
            onQueryChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseSearchBarWithQueryPreview() {
    PreviewThemeWrapper {
        ExpenseSearchBar(
            query = "Dinner",
            onQueryChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default)
        )
    }
}
