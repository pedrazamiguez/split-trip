package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.features.expense.R

@Composable
internal fun ShowAllCategoriesChip(
    showAllCategories: Boolean,
    onToggleShowAllCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    PassportChip(
        label = stringResource(R.string.category_show_all_toggle),
        selected = showAllCategories,
        onClick = onToggleShowAllCategories,
        modifier = modifier,
        leadingIcon = if (showAllCategories) {
            {
                Icon(
                    imageVector = TablerIcons.Outline.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        }
    )
}
