package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.SubcategoryTrayHeaderRow

@Composable
internal fun FilterSectionHeader(
    title: String,
    selectedCount: Int,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    var retainedCount by remember { mutableIntStateOf(selectedCount) }
    if (selectedCount > 0) {
        retainedCount = selectedCount
    }

    val badgeDescription = if (retainedCount > 0) {
        pluralStringResource(
            R.plurals.category_filter_selected_count,
            retainedCount,
            retainedCount
        )
    } else {
        null
    }
    val badgeModifier = badgeDescription?.let { desc ->
        Modifier.semantics { contentDescription = desc }
    } ?: Modifier

    SubcategoryTrayHeaderRow(
        title = title,
        showBadge = selectedCount > 0,
        retainedBadgeText = retainedCount.toString(),
        badgeStyle = PassportTagStyle.INFO,
        badgeModifier = badgeModifier,
        modifier = modifier,
        trailingContent = trailingContent
    )
}
