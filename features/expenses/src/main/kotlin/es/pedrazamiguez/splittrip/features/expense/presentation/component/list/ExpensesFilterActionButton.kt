package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Filter
import es.pedrazamiguez.splittrip.features.expense.R

private val ACTION_SLOT_SIZE = 48.dp
private val TOUCH_TARGET_SIZE = 40.dp
private val BADGE_INSET = 4.dp
private val BADGE_MIN_SIZE = 16.dp

@Composable
internal fun ExpensesFilterActionButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier.size(ACTION_SLOT_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(TOUCH_TARGET_SIZE)
                .clip(CircleShape)
                .debouncedCombinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = {
                        if (activeFilterCount > 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Filter,
                contentDescription = stringResource(R.string.expenses_filter_button_cd),
                tint = if (activeFilterCount > 0) {
                    MaterialTheme.colorScheme.secondaryFixed
                } else {
                    LocalContentColor.current
                }
            )
        }

        if (activeFilterCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = BADGE_INSET, end = BADGE_INSET)
                    .sizeIn(minWidth = BADGE_MIN_SIZE, minHeight = BADGE_MIN_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryFixed)
                    .padding(horizontal = MaterialTheme.spacing.ExtraSmall),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeFilterCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryFixed,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
