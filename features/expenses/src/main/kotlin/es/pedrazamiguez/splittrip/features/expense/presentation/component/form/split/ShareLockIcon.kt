package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.LockFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LockOpen
import es.pedrazamiguez.splittrip.features.expense.R

@Composable
internal fun ShareLockIcon(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isLocked) TablerIcons.Filled.LockFilled else TablerIcons.Outline.LockOpen,
            contentDescription = stringResource(
                if (isLocked) {
                    R.string.add_expense_split_share_unlock
                } else {
                    R.string.add_expense_split_share_lock
                }
            ),
            tint = if (isLocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(18.dp)
        )
    }
}
