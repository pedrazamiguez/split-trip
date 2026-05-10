package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Funding source selection using condensed chips.
 * Allows the user to choose between "Group Pocket" and "My Money".
 */
@Composable
internal fun FundingSourceSection(
    fundingSources: ImmutableList<FundingSourceUiModel>,
    selectedFundingSource: FundingSourceUiModel?,
    onFundingSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.funding_source_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        CondensedChips(
            items = fundingSources,
            selectedId = selectedFundingSource?.id,
            onItemSelected = { sourceId ->
                onFundingSourceSelected(sourceId)
                focusManager.clearFocus()
            },
            itemId = { it.id },
            itemLabel = { it.displayText },
            visibleCount = 2
        )
    }
}
