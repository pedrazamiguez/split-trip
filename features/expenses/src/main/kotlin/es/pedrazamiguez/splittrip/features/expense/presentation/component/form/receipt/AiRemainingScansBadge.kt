package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

/**
 * Compact pill badge showing remaining monthly free AI receipt scans or quota exhaustion.
 */
@Composable
fun AiRemainingScansBadge(
    label: String,
    isExhausted: Boolean,
    modifier: Modifier = Modifier
) {
    val icon = if (isExhausted) {
        TablerIcons.Outline.Lock
    } else {
        TablerIcons.Outline.PhotoAi
    }

    PassportTag(
        text = label,
        icon = icon,
        style = if (isExhausted) PassportTagStyle.ERROR else PassportTagStyle.INFO,
        modifier = modifier
    )
}
