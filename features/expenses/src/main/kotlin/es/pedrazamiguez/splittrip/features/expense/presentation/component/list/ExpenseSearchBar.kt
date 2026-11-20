package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.extension.clearFocusOnKeyboardDismiss
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.expense.R

@Composable
fun ExpenseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    val trailingIcon: @Composable (() -> Unit)? = if (query.isNotEmpty()) {
        {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.expenses_search_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        null
    }

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
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Search,
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() }
        ),
        modifier = modifier
            .clearFocusOnKeyboardDismiss()
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.hasFocus)
            }
    )
}
