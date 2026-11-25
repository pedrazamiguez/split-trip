package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check

internal fun filterChipLeadingIcon(
    isSelected: Boolean,
    unselectedIcon: ImageVector? = null
): (@Composable () -> Unit)? {
    val iconVector = if (isSelected) TablerIcons.Outline.Check else unselectedIcon
    return iconVector?.let { vector ->
        {
            Icon(
                imageVector = vector,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    }
}
