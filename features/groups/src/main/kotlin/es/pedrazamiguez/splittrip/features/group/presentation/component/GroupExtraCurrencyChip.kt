package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

@Composable
internal fun GroupExtraCurrencyChip(
    currency: String,
    modifier: Modifier = Modifier
) {
    PassportTag(
        text = currency,
        style = PassportTagStyle.NEUTRAL,
        modifier = modifier
    )
}
