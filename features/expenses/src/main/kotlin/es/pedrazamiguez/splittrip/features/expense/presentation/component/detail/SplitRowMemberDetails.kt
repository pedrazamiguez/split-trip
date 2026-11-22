package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.FormerMemberTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel

@Composable
internal fun SplitRowMemberDetails(
    split: SplitDetailUiModel,
    modifier: Modifier = Modifier
) {
    val isFormer = split.memberDisplay is MemberDisplay.Former
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Text(
                text = if (split.isCurrentUser) {
                    stringResource(R.string.expense_detail_split_you_badge)
                } else {
                    split.displayName
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isFormer) {
                FormerMemberTag()
            }
        }
        if (split.shareText != null) {
            CaptionText(
                text = split.shareText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
