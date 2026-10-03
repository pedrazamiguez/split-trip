package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Threshold of available currencies above which search filtering is enabled by default. */
const val CURRENCY_PICKER_SEARCH_THRESHOLD = 6

/** Semantics test tags for currency picker testing. */
const val CURRENCY_PICKER_SEARCH_TAG = "currency_picker_search"
const val CURRENCY_PICKER_ITEM_TAG_PREFIX = "currency_picker_item_"
const val CURRENCY_PICKER_EMPTY_TAG = "currency_picker_empty"

/**
 * Resolves the primary headline label for a currency picker item, preferring localized name,
 * then default English name, then currency code.
 */
internal fun resolveCurrencyItemHeadline(currency: CurrencyUiModel): String =
    currency.localizedName.ifBlank { currency.defaultName.ifBlank { currency.code } }

/**
 * Filters a list of currencies case-insensitively across code, localizedName, defaultName, and displayText.
 */
internal fun filterCurrencies(
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    searchQuery: String
): List<CurrencyUiModel> {
    if (searchQuery.isBlank()) {
        return availableCurrencies
    }
    val query = searchQuery.trim().lowercase()
    return availableCurrencies.filter { currency ->
        currency.code.lowercase().contains(query) ||
            currency.localizedName.lowercase().contains(query) ||
            currency.defaultName.lowercase().contains(query) ||
            currency.displayText.lowercase().contains(query)
    }
}

@Composable
private fun CurrencyPickerSearchField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trailingIcon: @Composable (() -> Unit)? = if (searchQuery.isNotEmpty()) {
        {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.content_description_clear_search)
                )
            }
        }
    } else {
        null
    }

    StyledOutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.currency_picker_search_placeholder),
        leadingIcon = {
            Icon(
                imageVector = TablerIcons.Outline.Search,
                contentDescription = null
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.Large)
            .padding(bottom = MaterialTheme.spacing.Default)
            .testTag(CURRENCY_PICKER_SEARCH_TAG)
    )
}

@Composable
private fun CurrencyPickerEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.Section),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.currency_picker_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(CURRENCY_PICKER_EMPTY_TAG)
        )
    }
}

@Composable
private fun CurrencyPickerItemRow(
    currency: CurrencyUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trailingIcon: @Composable (() -> Unit)? = if (isSelected) {
        {
            Icon(
                imageVector = TablerIcons.Outline.Check,
                contentDescription = stringResource(R.string.content_description_selected),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        null
    }

    val headlineColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val supportingColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = {
            Text(
                text = resolveCurrencyItemHeadline(currency),
                style = MaterialTheme.typography.bodyLarge,
                color = headlineColor
            )
        },
        supportingContent = {
            Text(
                text = currency.displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor
            )
        },
        trailingContent = trailingIcon,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .testTag(CURRENCY_PICKER_ITEM_TAG_PREFIX + currency.code)
            .debouncedClickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerList(
    currencies: List<CurrencyUiModel>,
    selectedCurrency: CurrencyUiModel?,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    if (currencies.isEmpty()) {
        CurrencyPickerEmptyState(modifier = modifier)
    } else {
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            items(
                items = currencies,
                key = { it.code }
            ) { currency ->
                CurrencyPickerItemRow(
                    currency = currency,
                    isSelected = selectedCurrency?.code == currency.code,
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion { cause ->
                            if (cause == null) {
                                onCurrencySelected(currency.code)
                                onDismiss()
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Modal bottom sheet for picking a currency from [availableCurrencies].
 *
 * Provides a virtualized list with checkmark indicators and optional search filtering
 * when the number of available currencies exceeds [CURRENCY_PICKER_SEARCH_THRESHOLD].
 *
 * @param selectedCurrency    Currently selected currency (or `null` if none).
 * @param availableCurrencies List of currencies available for selection.
 * @param onCurrencySelected  Callback invoked when a currency is chosen.
 * @param onDismiss           Callback invoked when the sheet is dismissed.
 * @param modifier            Outer modifier for the bottom sheet.
 * @param title               Header title displayed at the top of the sheet.
 * @param showSearch          Whether to display the inline search filter field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    selectedCurrency: CurrencyUiModel?,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.currency_picker_title),
    showSearch: Boolean = availableCurrencies.size > CURRENCY_PICKER_SEARCH_THRESHOLD
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(availableCurrencies, searchQuery) {
        filterCurrencies(availableCurrencies, searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets.safeDrawing },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showSearch) Modifier.fillMaxHeight() else Modifier)
                .padding(
                    top = MaterialTheme.spacing.ExtraLarge,
                    bottom = MaterialTheme.spacing.ExtraLarge
                )
        ) {
            SheetTitleText(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.Large)
                    .padding(bottom = MaterialTheme.spacing.Default)
            )

            if (showSearch) {
                CurrencyPickerSearchField(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
                HorizontalDivider()
            }

            CurrencyPickerList(
                currencies = filteredCurrencies,
                selectedCurrency = selectedCurrency,
                onCurrencySelected = onCurrencySelected,
                onDismiss = onDismiss,
                sheetState = sheetState,
                scope = coroutineScope,
                modifier = if (showSearch) Modifier.weight(1f) else Modifier
            )
        }
    }
}
