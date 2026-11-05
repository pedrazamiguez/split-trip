package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel

private val CHEVRON_SIZE = 18.dp
private const val FORMER_MEMBER_ALPHA = 0.6f

@Composable
internal fun SplitRow(
    split: SplitDetailUiModel,
    modifier: Modifier = Modifier,
    reserveChevronSpace: Boolean = false
) {
    val isFormer = split.memberDisplay is MemberDisplay.Former
    val alphaVal = if (isFormer) FORMER_MEMBER_ALPHA else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFormer) Modifier.alpha(alphaVal) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SplitRowMemberDetails(split = split)

        val amountModifier = if (reserveChevronSpace) {
            Modifier.padding(end = MaterialTheme.spacing.ExtraSmall + CHEVRON_SIZE)
        } else {
            Modifier
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = amountModifier
        ) {
            AmountText(
                text = split.formattedAmount,
                color = if (split.isExcluded) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (split.formattedSourceAmount != null) {
                CaptionText(
                    text = split.formattedSourceAmount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
