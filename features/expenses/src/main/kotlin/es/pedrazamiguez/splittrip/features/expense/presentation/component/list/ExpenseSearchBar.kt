package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Filter
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.expense.R

@Suppress("LongMethod")
@Composable
fun ExpenseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilterCount: Int = 0,
    onFilterClick: () -> Unit = {},
    onFilterLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current

    StyledOutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.expenses_search_placeholder),
        leadingIcon = {
            Icon(
                imageVector = TablerIcons.Outline.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") }
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.X,
                            contentDescription = stringResource(R.string.expenses_search_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .clip(CircleShape)
                            .debouncedCombinedClickable(
                                role = Role.Button,
                                onClick = onFilterClick,
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onFilterLongClick()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.Filter,
                            contentDescription = stringResource(R.string.expenses_filter_button_cd),
                            tint = if (activeFilterCount > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .sizeIn(minWidth = 16.dp, minHeight = 16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        singleLine = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() }
        ),
        modifier = modifier
    )
}
