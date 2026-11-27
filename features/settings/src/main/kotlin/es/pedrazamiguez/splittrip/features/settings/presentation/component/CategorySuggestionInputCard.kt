package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.features.settings.R
import kotlinx.collections.immutable.persistentListOf

private val GROUP_CONTEXT_OPTIONS = persistentListOf(GroupContext.TRAVEL, GroupContext.EVERYDAY)

@Composable
internal fun CategorySuggestionInputCard(
    titleInput: String,
    selectedContext: GroupContext,
    onTitleChanged: (String) -> Unit,
    onContextChanged: (GroupContext) -> Unit,
    onClearInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val travelLabel = stringResource(GroupContext.TRAVEL.toStringRes())
    val everydayLabel = stringResource(GroupContext.EVERYDAY.toStringRes())
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

    SectionCard(
        title = stringResource(R.string.developer_services_category_input_title),
        modifier = modifier
    ) {
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}
