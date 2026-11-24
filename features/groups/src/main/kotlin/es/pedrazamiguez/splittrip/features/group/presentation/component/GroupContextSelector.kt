package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toDescriptionRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.features.group.R

@Composable
internal fun GroupContextSelector(
    selectedContext: GroupContext,
    onContextSelected: (GroupContext) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        CardSectionLabelText(text = stringResource(R.string.group_field_context))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            GroupContext.entries.forEach { context ->
                PassportChip(
                    label = stringResource(context.toStringRes()),
                    selected = context == selectedContext,
                    onClick = { onContextSelected(context) },
                    leadingIcon = {
                        Icon(
                            imageVector = context.toIconVector(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
        CaptionText(
            text = stringResource(selectedContext.toDescriptionRes()),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
