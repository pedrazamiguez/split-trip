package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.getNameRes
import es.pedrazamiguez.splittrip.domain.enums.Currency

@Composable
internal fun CurrencyDescription(currentCurrency: Currency?) {
    Crossfade(
        targetState = currentCurrency,
        label = "CurrencyFade"
    ) { currency ->
        if (currency == null) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(20.dp)
            )
        } else {
            val currencyName = stringResource(id = currency.getNameRes())
            Text(text = "$currencyName (${currency.symbol})")
        }
    }
}
