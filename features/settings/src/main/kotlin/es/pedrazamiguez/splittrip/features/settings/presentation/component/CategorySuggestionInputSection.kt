package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.features.settings.R
import kotlinx.collections.immutable.persistentListOf

private val GROUP_CONTEXT_OPTIONS = persistentListOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategorySuggestionInputSection(
    titleInput: String,
    selectedContext: GroupContext,
    onTitleChanged: (String) -> Unit,
    onContextChanged: (GroupContext) -> Unit,
    onClearInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val travelLabel = stringResource(GroupContext.TRAVEL.toStringRes())
    val everydayLabel = stringResource(GroupContext.EVERYDAY.toStringRes())
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isInputFocused by remember { mutableStateOf(false) }
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(isInputFocused, imeBottomPx) {
        if (isInputFocused && imeBottomPx > 0) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    val trailingIcon: (@Composable () -> Unit)? = if (titleInput.isNotEmpty()) {
        {
            IconButton(onClick = onClearInput) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.developer_services_clear)
                )
            }
        }
    } else {
        null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SectionHeadingText(text = stringResource(R.string.developer_services_category_input_title))
        SegmentedPillSelector(
            options = GROUP_CONTEXT_OPTIONS,
            selectedOption = selectedContext,
            onOptionSelected = onContextChanged,
            optionLabel = { if (it == GroupContext.TRAVEL) travelLabel else everydayLabel }
        )
        StyledOutlinedTextField(
            value = titleInput,
            onValueChange = onTitleChanged,
            label = stringResource(R.string.developer_services_category_input_label),
            placeholder = stringResource(R.string.developer_services_category_input_placeholder),
            singleLine = true,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
            trailingIcon = trailingIcon,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState -> isInputFocused = focusState.isFocused }
        )
    }
}
