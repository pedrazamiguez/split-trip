package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.features.expense.R

private val SEARCH_BAR_MIN_HEIGHT = 42.dp
private val FOCUS_BORDER_WIDTH = 1.dp
private const val FOCUS_BORDER_ALPHA = 0.20f
private val SEARCH_ICON_SIZE = 18.dp
private val CLEAR_BUTTON_SIZE = 32.dp

@Suppress("LongMethod")
@Composable
fun ExpenseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val borderModifier = if (isFocused) {
        Modifier.border(
            width = FOCUS_BORDER_WIDTH,
            color = MaterialTheme.colorScheme.outline.copy(alpha = FOCUS_BORDER_ALPHA),
            shape = CircleShape
        )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .heightIn(min = SEARCH_BAR_MIN_HEIGHT)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(borderModifier)
            .padding(
                start = MaterialTheme.spacing.Medium,
                end = MaterialTheme.spacing.Small
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SEARCH_ICON_SIZE)
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.expenses_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        onFocusChanged(state.isFocused)
                    },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                )
            )
        }

        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(CLEAR_BUTTON_SIZE)
                    .clip(CircleShape)
                    .debouncedClickable(
                        role = Role.Button,
                        onClick = { onQueryChange("") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.expenses_search_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SEARCH_ICON_SIZE)
                )
            }
        }
    }
}
