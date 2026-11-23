package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.getNameRes
import es.pedrazamiguez.splittrip.domain.enums.Currency

@Composable
internal fun DefaultCurrencyItemRow(
    currency: Currency,
    isSelected: Boolean,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyName = stringResource(id = currency.getNameRes())
    val codeWithSymbol = if (currency.symbol.isNotBlank() && currency.symbol != currency.name) {
        "${currency.name} (${currency.symbol})"
    } else {
        currency.name
    }

    SettingsSelectionRow(
        title = currencyName,
        subtitle = codeWithSymbol,
        isSelected = isSelected,
        leadingBadgeText = currency.symbol.ifBlank { currency.name },
        onClick = { onCurrencySelected(currency.name) },
        modifier = modifier
    )
}
