package es.pedrazamiguez.splittrip.features.expense.presentation.component.list

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }
    val pendingQueries = remember { ArrayDeque<String>() }

    // Ignore lagging StateFlow echoes of our own input while honoring genuine external resets.
    LaunchedEffect(query) {
        reconcilePendingQuery(query, textFieldValue.text, pendingQueries)?.let { resetValue ->
            textFieldValue = resetValue
        }
    }

    val trailingIcon: @Composable (() -> Unit)? = if (textFieldValue.text.isNotEmpty()) {
        {
            IconButton(
                onClick = {
                    textFieldValue = TextFieldValue(text = "", selection = TextRange.Zero)
                    if (query.isNotEmpty()) pendingQueries.addLast("")
                    onQueryChange("")
                }
            ) {
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
        value = textFieldValue,
        onValueChange = { newTfv ->
            textFieldValue = applySearchTextChange(newTfv, textFieldValue.text, pendingQueries, onQueryChange)
        },
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
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier
            .clearFocusOnKeyboardDismiss()
            .onFocusChanged { onFocusChanged(it.hasFocus) }
    )
}

private fun applySearchTextChange(
    newTfv: TextFieldValue,
    previousText: String,
    pendingQueries: ArrayDeque<String>,
    onQueryChange: (String) -> Unit
): TextFieldValue {
    if (newTfv.text != previousText) {
        pendingQueries.addLast(newTfv.text)
        onQueryChange(newTfv.text)
    }
    return newTfv
}

/**
 * Reconciles an incoming [query] emission against [pendingQueries], returning a new
 * [TextFieldValue] if [query] represents a genuine external reset, or `null` if it is
 * a lagging echo of user input or already matches [currentText].
 */
private fun reconcilePendingQuery(
    query: String,
    currentText: String,
    pendingQueries: ArrayDeque<String>
): TextFieldValue? {
    val pendingIndex = pendingQueries.indexOf(query)
    if (pendingIndex >= 0) {
        repeat(pendingIndex + 1) { pendingQueries.removeFirst() }
        return null
    }
    if (currentText == query) return null
    pendingQueries.clear()
    return TextFieldValue(text = query, selection = TextRange(query.length))
}
