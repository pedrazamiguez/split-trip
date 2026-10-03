package es.pedrazamiguez.splittrip.features.settings.presentation.component

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
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
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
                text = currencyName,
                style = MaterialTheme.typography.bodyLarge,
                color = headlineColor
            )
        },
        supportingContent = {
            Text(
                text = codeWithSymbol,
                style = MaterialTheme.typography.bodyMedium,
                color = supportingColor
            )
        },
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
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            }
            .debouncedClickable {
                onCurrencySelected(currency.name)
            }
    )
}
