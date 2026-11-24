package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * **Passport Chip** — Horizon Narrative signature travel chip component.
 *
 * Designed to feel like a collectible travel stamp. Uses `secondaryFixedDim` as the selected
 * background and `onSecondaryFixed` for selected content (text/icons), keeping the tone stable
 * across light and dark themes. Unselected chips use `surfaceContainerHigh` for a subtle,
 * tonal-layered appearance.
 *
 * - **No border** in any state (Horizon Narrative No-Line rule — colour shift defines selection).
 * - **Shape:** `CircleShape` (full-pill — "collectible stamp" feel).
 *
 * ### Usage — selectable tag (SubcategoryStampTray, PaymentMethodChips, etc.)
 * ```kotlin
 * PassportChip(
 *     label = category.displayText,
 *     selected = isSelected,
 *     onClick = { onCategorySelected(category.id) },
 *     leadingIcon = if (isSelected) { { Icon(TablerIcons.Outline.Check, null) } } else null,
 * )
 * ```
 *
 * ### Usage — removable item chip (SearchableChipSelector)
 * ```kotlin
 * PassportChip(
 *     label = itemDisplayText(item),
 *     selected = true,
 *     onClick = { onItemRemoved(item) },
 *     trailingIcon = {
 *         Icon(TablerIcons.Outline.X, contentDescription = chipRemoveContentDescription,
 *             modifier = Modifier.size(18.dp))
 *     },
 * )
 * ```
 *
 * ### Usage — overflow chip (CondensedChips)
 * ```kotlin
 * PassportChip(
 *     label = stringResource(R.string.add_expense_chips_more),
 *     selected = anyOverflowSelected,
 *     onClick = { expanded = true },
 *     trailingIcon = { Icon(TablerIcons.Outline.Dots, contentDescription = ...) },
 * )
 * ```
 *
 * @param label Text displayed inside the chip.
 * @param selected Whether the chip is in the selected (active) state.
 * @param onClick Callback invoked when the chip is clicked.
 * @param modifier Modifier applied to the chip.
 * @param leadingIcon Optional composable shown before the label (e.g. a checkmark icon).
 * @param trailingIcon Optional composable shown after the label (e.g. a Close or MoreHoriz icon).
 */
@Composable
fun PassportChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            // Unselected state — subtle tonal background, muted text
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            // Selected state — secondaryFixedDim background with onSecondaryFixed content
            // Both roles are tone-stable (same in light and dark — M3 Fixed role spec)
            selectedContainerColor = MaterialTheme.colorScheme.secondaryFixedDim,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryFixed,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryFixed,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryFixed
        ),
        elevation = null,
        border = null // No border — Horizon Narrative No-Line rule; colour shift defines state
    )
}
