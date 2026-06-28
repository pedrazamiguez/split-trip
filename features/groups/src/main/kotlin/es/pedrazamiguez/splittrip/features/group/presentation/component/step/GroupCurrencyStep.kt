package es.pedrazamiguez.splittrip.features.group.presentation.component.step

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyDropdown
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SearchableChipSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

/**
 * Step 2: Primary currency dropdown + optional extra currencies chip selector.
 */
@Composable
fun GroupCurrencyStep(
    uiState: CreateEditGroupUiState,
    onEvent: (CreateEditGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        CurrencyDropdown(
            selectedCurrency = uiState.selectedCurrency,
            availableCurrencies = uiState.availableCurrencies,
            onCurrencySelected = { onEvent(CreateEditGroupUiEvent.CurrencySelected(it)) },
            label = stringResource(R.string.group_field_currency),
            isLoading = uiState.isLoadingCurrencies,
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState.availableCurrencies.isNotEmpty()) {
            SearchableChipSelector(
                availableItems = uiState.availableCurrencies,
                selectedItems = uiState.extraCurrencies,
                onItemAdded = { onEvent(CreateEditGroupUiEvent.ExtraCurrencyToggled(it.code)) },
                onItemRemoved = { onEvent(CreateEditGroupUiEvent.ExtraCurrencyToggled(it.code)) },
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
}
