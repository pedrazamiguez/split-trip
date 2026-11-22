package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelectorColors
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelectorDefaults
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SplitTypeSelector(
    splitTypes: ImmutableList<SplitTypeUiModel>,
    selectedSplitType: SplitTypeUiModel?,
    onSplitTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: SegmentedPillSelectorColors = SegmentedPillSelectorDefaults.colors()
) {
    SegmentedPillSelector(
        options = splitTypes,
        selectedOption = selectedSplitType,
        onOptionSelected = { splitType -> onSplitTypeSelected(splitType.id) },
        optionLabel = { splitType -> splitType.displayText },
        optionKey = { splitType -> splitType.id },
        colors = colors,
        modifier = modifier
    )
}
