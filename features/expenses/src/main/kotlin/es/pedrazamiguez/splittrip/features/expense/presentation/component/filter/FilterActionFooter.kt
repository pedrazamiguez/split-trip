package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.expense.R

@Suppress("LongParameterList")
@Composable
internal fun FilterActionFooter(
    matchingExpensesCount: Int,
    canReset: Boolean,
    bottomPadding: Dp,
    isBottomBarHidden: Boolean,
    onResetClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = MaterialTheme.spacing.Large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isBottomBarHidden) Modifier.navigationBarsPadding() else Modifier)
                .padding(
                    start = MaterialTheme.spacing.Default,
                    end = MaterialTheme.spacing.Default,
                    top = MaterialTheme.spacing.Small,
                    bottom = if (isBottomBarHidden) MaterialTheme.spacing.Medium else bottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilterMatchingCountRow(
                matchingExpensesCount = matchingExpensesCount,
                hasActiveFilters = canReset
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = stringResource(R.string.expenses_filter_reset),
                    onClick = onResetClick,
                    enabled = canReset,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = stringResource(R.string.expenses_filter_apply),
                    onClick = onApplyClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
