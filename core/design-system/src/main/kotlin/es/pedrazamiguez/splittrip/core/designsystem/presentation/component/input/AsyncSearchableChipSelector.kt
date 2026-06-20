package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
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
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Qrcode
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

/**
 * An async search-based multi-select component with autocomplete dropdown and removable chips.
 *
 * Unlike [SearchableChipSelector], this component does NOT filter items locally.
 * Instead, it delegates the search query to the caller via [onSearchQueryChanged],
 * which is responsible for fetching results asynchronously (e.g., from a remote API).
 * The caller provides the [searchResults] list, which is displayed in the dropdown.
 *
 * @param T The type of items being selected
 * @param searchResults Results returned from the async search (provided by caller/ViewModel)
 * @param selectedItems Currently selected items (displayed as chips)
 * @param onSearchQueryChanged Called when the search query changes (caller performs the search)
 * @param onItemAdded Called when an item is selected from the dropdown
 * @param onItemRemoved Called when a chip is removed (item deselected)
 * @param itemKey Extracts a unique key from an item
 * @param itemDisplayText Converts an item to its display text for chips and dropdown
 * @param itemSecondaryText Optional secondary text shown below the main text in dropdown
 * @param isSearching Whether an async search is in progress (shows loading indicator)
 * @param modifier Modifier to be applied to the component
 * @param title Optional title text displayed above the component
 * @param searchLabel Label for the search text field
 * @param searchPlaceholder Placeholder text for the search field
 * @param helperText Helper text shown when no items are selected and search is empty
 * @param noResultsText Text shown when a search returns no results
 * @param chipRemoveContentDescription Content description for chip remove icon
 * @param clearSearchContentDescription Content description for clear search icon
 * @param searchIcon Icon for the search field leading icon
 * @param minQueryLength Minimum characters required before triggering search
 * @param keyboardType Keyboard type for the search field
 * @param keyboardCapitalization Keyboard capitalization for the search field
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("LongMethod", "LongParameterList") // Compose UI — many optional params with defaults
@Composable
fun <T> AsyncSearchableChipSelector(
    searchResults: List<T>,
    selectedItems: List<T>,
    onSearchQueryChanged: (String) -> Unit,
    onItemAdded: (T) -> Unit,
    onItemRemoved: (T) -> Unit,
    itemKey: (T) -> Any,
    itemDisplayText: (T) -> String,
    isSearching: Boolean = false,
    modifier: Modifier = Modifier,
    itemSecondaryText: ((T) -> String)? = null,
    dropdownItemDisplayText: ((T) -> String)? = null,
    title: String? = null,
    searchLabel: String = "",
    searchPlaceholder: String = "",
    helperText: String? = null,
    noResultsText: String? = null,
    chipRemoveContentDescription: String? = null,
    clearSearchContentDescription: String? = null,
    searchIcon: ImageVector = TablerIcons.Outline.Search,
    minQueryLength: Int = 3,
    keyboardType: KeyboardType = KeyboardType.Email,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    onScannerClick: (() -> Unit)? = null,
    scannerContentDescription: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var hasSearchedOnce by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(searchResults) { expanded = searchResults.isNotEmpty() }

    val handleQueryChange: (String) -> Unit = { newQuery ->
        searchQuery = newQuery
        if (newQuery.length >= minQueryLength) {
            hasSearchedOnce = true
            onSearchQueryChanged(newQuery)
        } else {
            expanded = false
            onSearchQueryChanged("")
        }
    }
    val handleItemAdded: (T) -> Unit = { item ->
        onItemAdded(item)
        searchQuery = ""
        expanded = false
        hasSearchedOnce = false
    }
    val handleClearSearch = {
        searchQuery = ""
        expanded = false
        hasSearchedOnce = false
        onSearchQueryChanged("")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
        }

        if (selectedItems.isNotEmpty()) {
            AsyncSelectedChipsRow(
                selectedItems = selectedItems,
                itemKey = itemKey,
                itemDisplayText = itemDisplayText,
                chipRemoveContentDescription = chipRemoveContentDescription,
                onItemRemoved = onItemRemoved
            )
        }

        AsyncSearchTextField(
            searchQuery = searchQuery,
            onQueryChange = handleQueryChange,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            searchLabel = searchLabel,
            searchPlaceholder = searchPlaceholder,
            searchIcon = searchIcon,
            clearSearchContentDescription = clearSearchContentDescription,
            isSearching = isSearching,
            keyboardType = keyboardType,
            keyboardCapitalization = keyboardCapitalization,
            focusManager = focusManager,
            searchResults = searchResults,
            itemKey = itemKey,
            itemDisplayText = itemDisplayText,
            dropdownItemDisplayText = dropdownItemDisplayText,
            itemSecondaryText = itemSecondaryText,
            onItemAdded = handleItemAdded,
            onClearSearch = handleClearSearch,
            onScannerClick = onScannerClick,
            scannerContentDescription = scannerContentDescription
        )

        if (noResultsText != null) {
            AnimatedVisibility(
                visible = hasSearchedOnce &&
                    !isSearching &&
                    searchResults.isEmpty() &&
                    searchQuery.length >= minQueryLength
            ) {
                SecondaryBodyText(
                    text = noResultsText,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.ExtraSmall),
                    maxLines = Int.MAX_VALUE
                )
            }
        }

        if (helperText != null) {
            AnimatedVisibility(visible = selectedItems.isEmpty() && searchQuery.isEmpty()) {
                SecondaryBodyText(text = helperText, maxLines = Int.MAX_VALUE)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> AsyncSelectedChipsRow(
    selectedItems: List<T>,
    itemKey: (T) -> Any,
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
            key(itemKey(item)) {
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
}

@Composable
private fun AsyncSearchTrailingIcon(
    isSearching: Boolean,
    searchQuery: String,
    clearSearchContentDescription: String?,
    onClearSearch: () -> Unit,
    onScannerClick: (() -> Unit)?,
    scannerContentDescription: String?
) {
    when {
        isSearching -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        searchQuery.isNotEmpty() -> Icon(
            imageVector = TablerIcons.Outline.X,
            contentDescription = clearSearchContentDescription,
            modifier = Modifier.clickable(onClick = onClearSearch)
        )
        onScannerClick != null -> Icon(
            imageVector = TablerIcons.Outline.Qrcode,
            contentDescription = scannerContentDescription,
            modifier = Modifier.clickable(onClick = onScannerClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList") // Private composable wrapping ExposedDropdownMenuBox — params cannot be further reduced
@Composable
private fun <T> AsyncSearchTextField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    searchLabel: String,
    searchPlaceholder: String,
    searchIcon: ImageVector,
    clearSearchContentDescription: String?,
    isSearching: Boolean,
    keyboardType: KeyboardType,
    keyboardCapitalization: KeyboardCapitalization,
    focusManager: FocusManager,
    searchResults: List<T>,
    itemKey: (T) -> Any,
    itemDisplayText: (T) -> String,
    dropdownItemDisplayText: ((T) -> String)?,
    itemSecondaryText: ((T) -> String)?,
    onItemAdded: (T) -> Unit,
    onClearSearch: () -> Unit,
    onScannerClick: (() -> Unit)? = null,
    scannerContentDescription: String? = null
) {
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
                AsyncSearchTrailingIcon(
                    isSearching = isSearching,
                    searchQuery = searchQuery,
                    clearSearchContentDescription = clearSearchContentDescription,
                    onClearSearch = onClearSearch,
                    onScannerClick = onScannerClick,
                    scannerContentDescription = scannerContentDescription
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
                capitalization = keyboardCapitalization
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onExpandedChange(false)
            }),
            colors = softFieldColors(),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .semantics {
                    contentDescription = searchLabel.ifEmpty { searchPlaceholder }
                }
        )
        if (searchResults.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                AsyncDropdownMenuItems(
                    searchResults = searchResults,
                    itemKey = itemKey,
                    itemDisplayText = itemDisplayText,
                    dropdownItemDisplayText = dropdownItemDisplayText,
                    itemSecondaryText = itemSecondaryText,
                    onItemAdded = onItemAdded
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> AsyncDropdownMenuItems(
    searchResults: List<T>,
    itemKey: (T) -> Any,
    itemDisplayText: (T) -> String,
    dropdownItemDisplayText: ((T) -> String)?,
    itemSecondaryText: ((T) -> String)?,
    onItemAdded: (T) -> Unit
) {
    searchResults.forEach { item ->
        key(itemKey(item)) {
            val displayText = dropdownItemDisplayText?.invoke(item) ?: itemDisplayText(item)
            DropdownMenuItem(
                text = {
                    if (itemSecondaryText != null) {
                        Column {
                            BodyText(text = displayText)
                            SecondaryBodyText(text = itemSecondaryText(item))
                        }
                    } else {
                        BodyText(text = displayText)
                    }
                },
                onClick = { onItemAdded(item) },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )
        }
    }
}
