package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyDropdown
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SearchableChipSelector
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.EditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.EditGroupUiState

@Composable
internal fun EditGroupCurrenciesSection(
    uiState: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit
) {
    CurrencyDropdown(
        selectedCurrency = uiState.selectedCurrency,
        availableCurrencies = uiState.availableCurrencies,
        onCurrencySelected = { onEvent(EditGroupUiEvent.CurrencySelected(it)) },
        label = stringResource(R.string.group_field_currency),
        isLoading = uiState.isLoadingCurrencies,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

    if (uiState.availableCurrencies.isNotEmpty()) {
        SearchableChipSelector(
            availableItems = uiState.availableCurrencies,
            selectedItems = uiState.extraCurrencies,
            onItemAdded = { onEvent(EditGroupUiEvent.ExtraCurrencyToggled(it.code)) },
            onItemRemoved = { onEvent(EditGroupUiEvent.ExtraCurrencyToggled(it.code)) },
            itemKey = { it.code },
            itemDisplayText = { it.displayText },
            itemSecondaryText = { it.localizedName },
            itemMatchesQuery = { currency, query ->
                val upper = query.uppercase()
                currency.code.contains(upper) ||
                    currency.defaultName.uppercase().contains(upper) ||
                    currency.localizedName.uppercase().contains(upper) ||
                    currency.displayText.uppercase().contains(upper)
            },
            excludedItems = listOfNotNull(uiState.selectedCurrency),
            title = stringResource(R.string.group_field_extra_currencies),
            searchLabel = stringResource(R.string.group_extra_currency_search),
            searchPlaceholder = stringResource(R.string.group_extra_currency_search_hint),
            helperText = stringResource(R.string.group_field_extra_currencies_hint),
            chipRemoveContentDescription = stringResource(R.string.group_extra_currency_remove),
            clearSearchContentDescription = stringResource(R.string.group_extra_currency_clear),
            keyboardCapitalization = KeyboardCapitalization.Characters
        )
    }
}
