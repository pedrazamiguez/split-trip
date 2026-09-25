package es.pedrazamiguez.splittrip.features.group.presentation.component.step

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.badge.ProBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.AsyncSearchableChipSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

/**
 * Step 3: Invite members by searching their email address (optional).
 */
@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun GroupMembersStep(
    uiState: CreateEditGroupUiState,
    onEvent: (CreateEditGroupUiEvent) -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addUnregisteredFormat = stringResource(R.string.group_member_add_unregistered)
    WizardStepLayout(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.group_members_capacity_counter,
                        uiState.selectedMembers.size,
                        uiState.maxMembersLimit
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!uiState.isGroupPro) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.debouncedClickable {
                            onEvent(CreateEditGroupUiEvent.MemberCapacityUpsellClicked)
                        }
                    ) {
                        ProBadge()
                    }
                }
            }

            if (!uiState.isGroupPro) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .debouncedClickable {
                            onEvent(CreateEditGroupUiEvent.MemberCapacityUpsellClicked)
                        }
                        .padding(
                            horizontal = MaterialTheme.spacing.Small,
                            vertical = MaterialTheme.spacing.ExtraSmall
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.group_members_capacity_pro_upsell,
                            uiState.maxMembersLimit
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
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
