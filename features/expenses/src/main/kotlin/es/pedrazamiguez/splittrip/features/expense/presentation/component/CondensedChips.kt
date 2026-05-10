package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Dots
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.features.expense.R

/**
 * A reusable chip selector that shows the top [visibleCount] items as PassportChips
 * and collapses the rest behind a "+ More" chip with a dropdown menu.
 *
 * If the currently selected item is in the overflow, it replaces the last visible
 * chip to ensure the selection is always visible.
 *
 * @param items All available items.
 * @param selectedId The ID of the currently selected item, or null.
 * @param onItemSelected Callback when an item is selected.
 * @param itemId Extracts the unique ID from an item.
 * @param itemLabel Extracts the display label from an item.
 * @param visibleCount How many chips to show before collapsing into "More".
 * @param modifier Modifier for the FlowRow container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod") // Compose UI builder DSL
@Composable
fun <T> CondensedChips(
    items: List<T>,
    selectedId: String?,
    onItemSelected: (String) -> Unit,
    itemId: (T) -> String,
    itemLabel: (T) -> String,
    visibleCount: Int = 3,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val needsOverflow = items.size > visibleCount

    // Determine visible chips: if the selected item is in the overflow,
    // promote it to the visible set (replacing the last visible item).
    val visibleItems: List<T>
    val overflowItems: List<T>

    if (needsOverflow) {
        val baseVisible = items.take(visibleCount)
        val baseOverflow = items.drop(visibleCount)

        val selectedInOverflow = selectedId != null &&
            baseOverflow.any { itemId(it) == selectedId }

        if (selectedInOverflow) {
            val selectedItem = baseOverflow.first { itemId(it) == selectedId }
            // Replace the last visible chip with the selected overflow item
            visibleItems = baseVisible.dropLast(1) + selectedItem
            overflowItems = (
                baseVisible.takeLast(1) + baseOverflow.filter {
                    itemId(it) != selectedId
                }
                )
        } else {
            visibleItems = baseVisible
            overflowItems = baseOverflow
        }
    } else {
        visibleItems = items
        overflowItems = emptyList()
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        visibleItems.forEach { item ->
            val id = itemId(item)
            val isSelected = selectedId == id
            PassportChip(
                label = itemLabel(item),
                selected = isSelected,
                onClick = { onItemSelected(id) },
                leadingIcon = if (isSelected) {
                    { Icon(TablerIcons.Outline.Check, contentDescription = null) }
                } else {
                    null
                }
            )
        }

        if (overflowItems.isNotEmpty()) {
            OverflowPassportChip(
                overflowItems = overflowItems,
                selectedId = selectedId,
                onItemSelected = onItemSelected,
                itemId = itemId,
                itemLabel = itemLabel
            )
        }
    }
}

@Composable
private fun <T> OverflowPassportChip(
    overflowItems: List<T>,
    selectedId: String?,
    onItemSelected: (String) -> Unit,
    itemId: (T) -> String,
    itemLabel: (T) -> String
) {
    Box {
        var expanded by remember { mutableStateOf(false) }
        val anyOverflowSelected = selectedId != null &&
            overflowItems.any { itemId(it) == selectedId }

        PassportChip(
            label = stringResource(R.string.add_expense_chips_more),
            selected = anyOverflowSelected,
            onClick = { expanded = true },
            trailingIcon = {
                Icon(
                    imageVector = TablerIcons.Outline.Dots,
                    contentDescription = stringResource(R.string.add_expense_chips_more)
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            overflowItems.forEach { item ->
                val id = itemId(item)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel(item),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onItemSelected(id)
                        expanded = false
                    },
                    leadingIcon = if (selectedId == id) {
                        { Icon(TablerIcons.Outline.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
