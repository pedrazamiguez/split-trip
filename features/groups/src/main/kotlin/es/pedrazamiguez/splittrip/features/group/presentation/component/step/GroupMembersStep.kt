package es.pedrazamiguez.splittrip.features.group.presentation.component.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.AsyncSearchableChipSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

/**
 * Step 3: Invite members by searching their email address (optional).
 */
@Composable
fun GroupMembersStep(
    uiState: CreateEditGroupUiState,
    onEvent: (CreateEditGroupUiEvent) -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addUnregisteredFormat = stringResource(R.string.group_member_add_unregistered)
    WizardStepLayout(modifier = modifier) {
        AsyncSearchableChipSelector(
            searchResults = uiState.memberSearchResults,
            selectedItems = uiState.selectedMembers,
            onSearchQueryChanged = { onEvent(CreateEditGroupUiEvent.MemberSearchQueryChanged(it)) },
            onItemAdded = { onEvent(CreateEditGroupUiEvent.MemberSelected(it)) },
            onItemRemoved = { onEvent(CreateEditGroupUiEvent.MemberRemoved(it)) },
            itemKey = { it.userId },
            itemDisplayText = { it.displayName ?: it.email },
            dropdownItemDisplayText = {
                if (it.isPending) {
                    String.format(addUnregisteredFormat, it.email)
                } else {
                    it.displayName ?: it.email
                }
            },
            itemSecondaryText = { if (it.isPending) "" else it.email },
            isSearching = uiState.isSearchingMembers,
            title = stringResource(R.string.group_field_members),
            searchLabel = stringResource(R.string.group_member_search),
            searchPlaceholder = stringResource(R.string.group_member_search_hint),
            helperText = stringResource(R.string.group_member_search_helper),
            noResultsText = stringResource(R.string.group_member_search_no_results),
            chipRemoveContentDescription = stringResource(R.string.group_member_remove),
            clearSearchContentDescription = stringResource(R.string.group_member_clear_search),
            onScannerClick = onScannerClick,
            scannerContentDescription = stringResource(R.string.scanner_content_description)
        )
    }
}
