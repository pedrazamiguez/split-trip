package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

/**
 * A reusable search-based multi-select component with autocomplete dropdown and removable chips.
 *
 * This component provides a clean UX for selecting multiple items from a large list,
 * with search/filter functionality and visual chips for selected items.
 *
 * @param T The type of items being selected
 * @param availableItems All items that can be searched and selected
 * @param selectedItems Currently selected items (displayed as chips)
 * @param onItemAdded Called when an item is selected from the dropdown
 * @param onItemRemoved Called when a chip is removed (item deselected)
 * @param itemKey Extracts a unique key from an item (used for filtering duplicates)
 * @param itemDisplayText Converts an item to its display text for chips and dropdown
 * @param itemSecondaryText Optional secondary text shown below the main text in dropdown
 * @param itemMatchesQuery Determines if an item matches the search query
 * @param excludedItems Items to exclude from search results (e.g., a "main" selection)
 * @param modifier Modifier to be applied to the component
 * @param title Optional title text displayed above the component
 * @param searchLabel Label for the search text field
 * @param searchPlaceholder Placeholder text for the search field
 * @param helperText Helper text shown when no items are selected and search is empty
 * @param chipRemoveContentDescription Content description for chip remove icon
 * @param clearSearchContentDescription Content description for clear search icon
 * @param searchIcon Icon for the search field leading icon
 * @param minQueryLength Minimum characters required before showing search results
 * @param maxSuggestions Maximum number of suggestions to show in dropdown
 * @param keyboardCapitalization Keyboard capitalization for the search field
 */
@Suppress("LongMethod", "LongParameterList") // Compose UI — many optional params with defaults
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T> SearchableChipSelector(
    availableItems: List<T>,
    selectedItems: List<T>,
    onItemAdded: (T) -> Unit,
    onItemRemoved: (T) -> Unit,
    itemKey: (T) -> Any,
    itemDisplayText: (T) -> String,
    itemMatchesQuery: (T, String) -> Boolean,
    modifier: Modifier = Modifier,
    excludedItems: List<T> = emptyList(),
    itemSecondaryText: ((T) -> String)? = null,
    title: String? = null,
    searchLabel: String = "",
    searchPlaceholder: String = "",
    helperText: String? = null,
    chipRemoveContentDescription: String? = null,
    clearSearchContentDescription: String? = null,
    searchIcon: ImageVector = TablerIcons.Outline.Search,
    minQueryLength: Int = 2,
    maxSuggestions: Int = 5,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val excludedKeys = remember(excludedItems) { excludedItems.map { itemKey(it) }.toSet() }
    val selectedKeys = remember(selectedItems) { selectedItems.map { itemKey(it) }.toSet() }

    val filteredItems by remember(searchQuery, excludedKeys, selectedKeys, availableItems) {
        derivedStateOf {
            if (searchQuery.length < minQueryLength) {
                emptyList()
            } else {
                availableItems
                    .filter { item ->
                        val key = itemKey(item)
                        key !in excludedKeys && key !in selectedKeys && itemMatchesQuery(item, searchQuery)
                    }
                    .take(maxSuggestions)
            }
        }
    }

    LaunchedEffect(filteredItems) { expanded = filteredItems.isNotEmpty() }

    val handleItemAdded: (T) -> Unit = { item ->
        onItemAdded(item)
        searchQuery = ""
        expanded = false
    }
    val handleClearSearch = {
        searchQuery = ""
        expanded = false
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }
        if (selectedItems.isNotEmpty()) {
            SelectedChipsRow(
                selectedItems = selectedItems,
                itemDisplayText = itemDisplayText,
                chipRemoveContentDescription = chipRemoveContentDescription,
                onItemRemoved = onItemRemoved
            )
        }
        SearchableTextField(
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            searchLabel = searchLabel,
            searchPlaceholder = searchPlaceholder,
            searchIcon = searchIcon,
            clearSearchContentDescription = clearSearchContentDescription,
            keyboardCapitalization = keyboardCapitalization,
            onClearSearch = handleClearSearch
        ) {
            DropdownMenuItems(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                filteredItems = filteredItems,
                itemDisplayText = itemDisplayText,
                itemSecondaryText = itemSecondaryText,
                onItemAdded = handleItemAdded
            )
        }
        SearchHelperText(
            helperText = helperText,
            showHelper = selectedItems.isEmpty() && searchQuery.isEmpty()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SelectedChipsRow(
    selectedItems: List<T>,
    itemDisplayText: (T) -> String,
    chipRemoveContentDescription: String?,
    onItemRemoved: (T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        selectedItems.forEach { item ->
            PassportChip(
                label = itemDisplayText(item),
                selected = true,
                onClick = { onItemRemoved(item) },
                trailingIcon = {
                    Icon(
                        imageVector = TablerIcons.Outline.X,
                        contentDescription = chipRemoveContentDescription,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Suppress("LongParameterList") // Compose UI — many optional params with defaults
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchableTextField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    searchLabel: String = "",
    searchPlaceholder: String = "",
    searchIcon: ImageVector = TablerIcons.Outline.Search,
    clearSearchContentDescription: String? = null,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    dropdownContent: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            label = null,
            placeholder = {
                Text(searchPlaceholder.ifEmpty { searchLabel })
            },
            leadingIcon = { Icon(searchIcon, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = TablerIcons.Outline.X,
                        contentDescription = clearSearchContentDescription,
                        modifier = Modifier.clickable { onClearSearch() }
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = keyboardCapitalization
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onExpandedChange(false)
                }
            ),
            colors = softFieldColors(),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .semantics {
                    contentDescription = searchLabel.ifEmpty { searchPlaceholder }
                }
        )
        dropdownContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ExposedDropdownMenuBoxScope.DropdownMenuItems(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    filteredItems: List<T>,
    itemDisplayText: (T) -> String,
    onItemAdded: (T) -> Unit,
    itemSecondaryText: ((T) -> String)? = null
) {
    if (filteredItems.isEmpty()) return
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        filteredItems.forEach { item ->
            DropdownMenuItem(
                text = {
                    DropdownItemText(
                        item = item,
                        itemDisplayText = itemDisplayText,
                        itemSecondaryText = itemSecondaryText
                    )
                },
                onClick = { onItemAdded(item) },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
        }
    }
}

@Composable
private fun <T> DropdownItemText(
    item: T,
    itemDisplayText: (T) -> String,
    itemSecondaryText: ((T) -> String)?
) {
    if (itemSecondaryText != null) {
        Column {
            BodyText(text = itemDisplayText(item))
            SecondaryBodyText(text = itemSecondaryText(item))
        }
    } else {
        BodyText(text = itemDisplayText(item))
    }
}

@Composable
private fun SearchHelperText(helperText: String?, showHelper: Boolean) {
    if (helperText != null) {
        AnimatedVisibility(visible = showHelper) {
            SecondaryBodyText(text = helperText, maxLines = Int.MAX_VALUE)
        }
    }
}
