package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

@Composable
internal fun NotificationCategoryItem(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = {
            SecondaryBodyText(text = description, maxLines = Int.MAX_VALUE)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
