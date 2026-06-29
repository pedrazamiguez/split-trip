package es.pedrazamiguez.splittrip.features.subunit.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.SubunitItem
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.SubunitManagementUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.SubunitManagementUiState

@Suppress("LongMethod")
@Composable
fun SubunitManagementScreen(
    uiState: SubunitManagementUiState = SubunitManagementUiState(),
    onEvent: (SubunitManagementUiEvent) -> Unit = {}
) {
    var selectedSubunitForMenu by remember { mutableStateOf<SubunitUiModel?>(null) }
    var subunitToDelete by remember { mutableStateOf<SubunitUiModel?>(null) }
    val bottomPadding = LocalBottomPadding.current
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> ShimmerLoadingList()

            uiState.subunits.isEmpty() -> {
                EmptyStateView(
                    title = stringResource(R.string.subunit_empty_state),
                    icon = TablerIcons.Outline.Sitemap
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.Default,
                        top = MaterialTheme.spacing.Default,
                        end = MaterialTheme.spacing.Default,
                        bottom = MaterialTheme.spacing.Default + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
                ) {
                    items(
                        items = uiState.subunits,
                        key = { it.id }
                    ) { subunit ->
                        SubunitItem(
                            subunitUiModel = subunit,
                            modifier = Modifier.animateItem(),
                            onLongClick = {
                                if (!uiState.isGroupArchived) {
                                    selectedSubunitForMenu = subunit
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    selectedSubunitForMenu?.let { subunit ->
        ActionBottomSheet(
            title = stringResource(R.string.subunit_actions_title, subunit.name),
            icon = TablerIcons.Outline.Sitemap,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.action_edit_subunit),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {
                        onEvent(SubunitManagementUiEvent.EditSubunit(subunit.id))
                        selectedSubunitForMenu = null
                    }
                ),
                SheetAction(
                    text = stringResource(R.string.action_delete_subunit),
                    icon = TablerIcons.Outline.Trash,
                    onClick = {
                        subunitToDelete = subunit
                        selectedSubunitForMenu = null
                    },
                    isDestructive = true
                )
            ),
            onDismiss = { selectedSubunitForMenu = null }
        )
    }

    subunitToDelete?.let { subunit ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.subunit_delete_title),
            text = stringResource(R.string.subunit_delete_warning, subunit.name),
            onDismiss = { subunitToDelete = null },
            onConfirm = {
                onEvent(SubunitManagementUiEvent.ConfirmDeleteSubunit(subunit.id))
                subunitToDelete = null
            }
        )
    }
}
