package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SplitTypeSelector(
    splitTypes: ImmutableList<SplitTypeUiModel>,
    selectedSplitType: SplitTypeUiModel?,
    onSplitTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        splitTypes.forEachIndexed { index, splitType ->
            SegmentedButton(
                selected = splitType.id == selectedSplitType?.id,
                onClick = { onSplitTypeSelected(splitType.id) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = splitTypes.size
                ),
                label = {
                    Text(
                        text = splitType.displayText,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}
