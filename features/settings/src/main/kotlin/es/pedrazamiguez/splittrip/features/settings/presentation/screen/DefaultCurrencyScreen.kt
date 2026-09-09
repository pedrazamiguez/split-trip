package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.extension.getNameRes
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.Currency

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
                        val isSelected = currency.name == selectedCurrencyCode
                        val currencyName = stringResource(id = currency.getNameRes())

                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            supportingContent = { Text(text = currency.name) },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = TablerIcons.Outline.Check,
                                        contentDescription = stringResource(
                                            DesignSystemR.string.content_description_selected
                                        ),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .semantics {
                                    role = Role.RadioButton
                                    selected = isSelected
                                }
                                .debouncedClickable {
                                    onCurrencySelected(currency.name)
                                }
                        ) {
                            Text(text = "$currencyName (${currency.symbol})")
                        }
                    }
                }
            }
        }
    }
}
