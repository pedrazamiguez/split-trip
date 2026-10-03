package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.presentation.component.DefaultCurrencyItemRow

@Composable
fun DefaultCurrencyScreen(
    availableCurrencies: List<Currency>,
    selectedCurrencyCode: String?,
    onCurrencySelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.ExtraLarge,
            vertical = MaterialTheme.spacing.ExtraLarge
        )
    ) {
        item {
            FlatCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    availableCurrencies.forEach { currency ->
                        DefaultCurrencyItemRow(
                            currency = currency,
                            isSelected = currency.name == selectedCurrencyCode,
                            onCurrencySelected = onCurrencySelected
                        )
                    }
                }
            }
        }
    }
}
