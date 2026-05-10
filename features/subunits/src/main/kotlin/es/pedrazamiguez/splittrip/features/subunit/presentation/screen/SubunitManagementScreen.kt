package es.pedrazamiguez.splittrip.features.subunit.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.StickyActionBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.SubunitItem
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.SubunitManagementUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.SubunitManagementUiState

@Composable
fun SubunitManagementScreen(
    uiState: SubunitManagementUiState = SubunitManagementUiState(),
    onEvent: (SubunitManagementUiEvent) -> Unit = {}
) {
    var selectedSubunitForMenu by remember { mutableStateOf<SubunitUiModel?>(null) }
    var subunitToDelete by remember { mutableStateOf<SubunitUiModel?>(null) }
    val bottomPadding = LocalBottomPadding.current

    SubunitContent(
        uiState = uiState,
        bottomPadding = bottomPadding,
        onEvent = onEvent,
        onSubunitLongClick = { selectedSubunitForMenu = it }
    )

    SubunitActionSheet(
        subunit = selectedSubunitForMenu,
        onEdit = { id ->
            onEvent(SubunitManagementUiEvent.EditSubunit(id))
            selectedSubunitForMenu = null
        },
        onRequestDelete = { subunit ->
            subunitToDelete = subunit
            selectedSubunitForMenu = null
        },
        onDismiss = { selectedSubunitForMenu = null }
    )

    SubunitDeleteDialog(
        subunit = subunitToDelete,
        onConfirm = { id ->
            onEvent(SubunitManagementUiEvent.ConfirmDeleteSubunit(id))
            subunitToDelete = null
        },
        onDismiss = { subunitToDelete = null }
    )
}

@Composable
private fun SubunitContent(
    uiState: SubunitManagementUiState,
    bottomPadding: Dp,
    onEvent: (SubunitManagementUiEvent) -> Unit,
    onSubunitLongClick: (SubunitUiModel) -> Unit
) {
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
                val fabExtraPadding = 72.dp // Space for StickyActionBar
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.Default,
                        top = MaterialTheme.spacing.Default,
                        end = MaterialTheme.spacing.Default,
                        bottom = MaterialTheme.spacing.Default + bottomPadding + fabExtraPadding
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
                            onLongClick = { onSubunitLongClick(subunit) }
                        )
                    }
                }
            }
        }

        StickyActionBar(
            text = stringResource(R.string.subunit_create),
            icon = TablerIcons.Outline.Plus,
            onClick = { onEvent(SubunitManagementUiEvent.CreateSubunit) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
                .padding(bottom = bottomPadding + MaterialTheme.spacing.ExtraSmall),
            enabled = !uiState.isLoading,
            sharedTransitionKey = CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY
        )
    }
}

@Composable
private fun SubunitActionSheet(
    subunit: SubunitUiModel?,
    onEdit: (String) -> Unit,
    onRequestDelete: (SubunitUiModel) -> Unit,
    onDismiss: () -> Unit
) {
    subunit?.let {
        ActionBottomSheet(
            title = stringResource(R.string.subunit_actions_title, it.name),
            icon = TablerIcons.Outline.Sitemap,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.action_edit_subunit),
                    icon = TablerIcons.Outline.Edit,
                    onClick = { onEdit(it.id) }
                ),
                SheetAction(
                    text = stringResource(R.string.action_delete_subunit),
                    icon = TablerIcons.Outline.Trash,
                    onClick = { onRequestDelete(it) },
                    isDestructive = true
                )
            ),
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SubunitDeleteDialog(
    subunit: SubunitUiModel?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    subunit?.let {
        DestructiveConfirmationDialog(
            title = stringResource(R.string.subunit_delete_title),
            text = stringResource(R.string.subunit_delete_warning, it.name),
            onDismiss = onDismiss,
            onConfirm = { onConfirm(it.id) }
        )
    }
}
