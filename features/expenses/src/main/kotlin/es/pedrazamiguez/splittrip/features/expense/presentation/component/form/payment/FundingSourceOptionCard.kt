package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SelectableOptionCard
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel

/**
 * Selectable option card for choosing a funding source ("Group Pocket" or "Your Money").
 * Always displays the option's plain-language description alongside its title and icon
 * so first-time users understand the choice before tapping.
 */
@Composable
internal fun FundingSourceOptionCard(
    option: FundingSourceUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableOptionCard(
        title = option.displayText,
        description = option.description,
        icon = option.icon,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier
    )
}
